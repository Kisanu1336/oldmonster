package ru.onelove.functions.impl.combat;

import com.google.common.eventbus.Subscribe;
import ru.onelove.onelove;
import ru.onelove.command.friends.FriendStorage;
import ru.onelove.events.EventInput;
import ru.onelove.events.EventMotion;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.ModeListSetting;
import ru.onelove.functions.settings.impl.ModeSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.math.SensUtils;
import ru.onelove.utils.math.StopWatch;
import ru.onelove.utils.player.MoveUtils;
import lombok.Getter;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.hypot;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@FunctionRegister(name = "Killaura", type = Category.Combat)
public class KillAura extends Function {
    @Getter
    private final ModeSetting type = new ModeSetting("Тип", "FunTime", "FunTime", "Grim", "SpookyTime", "Reallyworld", "Плавная");
    private final SliderSetting attackRange = new SliderSetting("Дистанция атаки", 3f, 3f, 9f, 0.01f);
    private final SliderSetting preRange = new SliderSetting("Дистанция предикции", 0.5f, 0.0f, 3.0f, 0.05f)
            .setVisible(() -> type.is("Reallyworld"));
    private final ModeSetting sortMode = new ModeSetting("Сортировать", "По всему", "По всему", "По здоровью", "По броне", "По дистанции", "По обзору");

    final ModeListSetting targets = new ModeListSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Голые невидимки", true),
            new BooleanSetting("Невидимки", true),
            new BooleanSetting("Боты", false));

    @Getter
    final ModeListSetting options = new ModeListSetting("Опции",
            new BooleanSetting("Только криты", true),
            new BooleanSetting("Отжимать щит", true),
            new BooleanSetting("Ломать щит", true),
            new BooleanSetting("Ускорять ротацию при атаке", false),
            new BooleanSetting("Синхронизировать атаку с ТПС", false),
            new BooleanSetting("Фокусировать одну цель", true),
            new BooleanSetting("Коррекция движения", true),
            new BooleanSetting("Не бить при использовании", true),
            new BooleanSetting("Не бить через стены", true));

    final ModeSetting correctionType = new ModeSetting("Тип коррекции", "Незаметный", "Незаметный", "Сфокусированный");
    final ModeSetting sprintResetType = new ModeSetting("Сброс спринта", "Выключен", "Выключен", "Обычный", "Незаметный");

    @Getter
    private final StopWatch stopWatch = new StopWatch();
    public Vector2f rotateVector = new Vector2f(0.0F, 0.0F);
    @Getter
    private static LivingEntity target;
    private Entity selected;
    private final Random random = new Random();

    private boolean isHeadReacting = false;
    private float headReactionProgress = 0f;
    private float targetHeadYaw = 0f;
    private boolean headReactionDirection = true;
    private boolean isReturningToTarget = false;
    private final StopWatch headReactionTimer = new StopWatch();

    int ticks = 0;
    boolean isRotated;
    private float lastBodyYaw = 0F;
    private float bodyRotationSpeed = 3.6F;
    private int hitCounter = 0;
    private String currentHitPart = "head";
    private int previousSlot = -1;

    public KillAura() {
        addSettings(type, attackRange, preRange, sortMode, targets, options, correctionType, sprintResetType);
    }

    @Subscribe
    public void onInput(EventInput eventInput) {
        if (options.getValueByName("Коррекция движения").get() && correctionType.is("Незаметный") && target != null && mc.player != null) {
            MoveUtils.fixMovement(eventInput, rotateVector.x);
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (sprintResetType.is("Выключен")) {
            mc.player.setSprinting(false);
            mc.gameSettings.keyBindSprint.setPressed(false);
        }
        if (options.getValueByName("Фокусировать одну цель").get() && (target == null || !isValid(target)) || !options.getValueByName("Фокусировать одну цель").get()) {
            updateTarget();
        }

        if (target != null && !isUsingItem()) {
            isRotated = false;
            if (shouldPlayerFalling() && (stopWatch.hasTimeElapsed())) {
                updateAttack();
                ticks = 2;
            }
            if (type.is("Grim")) {
                if (!isRotated) {
                    updateRotation(false, 80, 35);
                }
            } else if (type.is("Reallyworld")) {
                if (canAttack() && mc.player.getDistance(target) <= attackRange.get()) {
                    updateAttack();
                    ticks = 3;
                }

                if (ticks > 0) {
                    updateRotation(false, 80, 35);
                    ticks--;
                } else {
                    rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
                }
            } else {
                if (!isRotated) {
                    updateRotation(false, 80, 35);
                }
            }
        } else {
            stopWatch.setLastMS(0);
            reset();
        }
    }

    @Subscribe
    private void onWalking(EventMotion e) {
        if (target == null) return;
        float targetYaw = rotateVector.x;
        float targetPitch = rotateVector.y;

        if (isHeadReacting && (type.is("Grim") || type.is("SpookyTime"))) {
            if (!isReturningToTarget) {
                float reactionSpeed = 0.12f;
                headReactionProgress += reactionSpeed;

                if (headReactionProgress >= 1.0f) {
                    isReturningToTarget = true;
                    headReactionProgress = 0f;
                } else {
                    float smoothProgress = 1.0f - (float) Math.pow(1.0f - headReactionProgress, 3.0);
                    float currentReaction = targetHeadYaw * smoothProgress;
                    targetYaw += currentReaction;
                }
            } else {
                float returnSpeed = 0.10f;
                headReactionProgress += returnSpeed;

                if (headReactionProgress >= 1.0f) {
                    isHeadReacting = false;
                    isReturningToTarget = false;
                    headReactionProgress = 0f;
                    targetHeadYaw = 0f;
                } else {
                    float smoothProgress = (float) Math.pow(1.0f - headReactionProgress, 2.0);
                    float currentReaction = targetHeadYaw * smoothProgress;
                    targetYaw += currentReaction;
                }
            }
        }

        if (type.is("SpookyTime")) {
            float circleAmplitude = 17.0f;
            float circleSpeed = 1.0f;
            float time = mc.player.ticksExisted * circleSpeed;
            float headCircleYaw = (float) Math.sin(time) * circleAmplitude;
            float headCirclePitch = (float) Math.cos(time) * circleAmplitude * 0.5f;
            targetYaw += headCircleYaw;
            targetPitch += headCirclePitch;
        }

        float headYaw = targetYaw;
        float headPitch = MathHelper.clamp(targetPitch, -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        headYaw -= (headYaw - mc.player.rotationYawHead) % gcd;
        headPitch -= (headPitch - mc.player.rotationPitchHead) % gcd;
        mc.player.rotationYawHead = headYaw;
        mc.player.rotationPitchHead = headPitch;

        float bodyTargetYaw = rotateVector.x;
        float yawDifference = MathHelper.wrapDegrees(bodyTargetYaw - lastBodyYaw);
        float bodyYaw = lastBodyYaw;
        if (Math.abs(yawDifference) > 0.1F) {
            bodyYaw += yawDifference / bodyRotationSpeed;
        }
        mc.player.renderYawOffset = bodyYaw;
        mc.player.prevRenderYawOffset = lastBodyYaw;
        lastBodyYaw = bodyYaw;
        e.setYaw(rotateVector.x);
        e.setPitch(rotateVector.y);
    }

    private void updateTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof LivingEntity living && isValid(living)) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (targets.size() == 1) {
            target = targets.get(0);
            return;
        }

        switch (sortMode.get()) {
            case "По всему" ->
                    Collections.shuffle(targets, random);
            case "По здоровью" ->
                    targets.sort(Comparator.comparingDouble(this::getEntityHealth));
            case "По броне" ->
                    targets.sort(Comparator.comparingDouble(entity ->
                            entity instanceof PlayerEntity ? getEntityArmor((PlayerEntity) entity) : entity.getTotalArmorValue()));
            case "По дистанции" ->
                    targets.sort(Comparator.comparingDouble(entity ->
                            mc.player.getDistance(entity)));
            case "По обзору" ->
                    targets.sort(Comparator.comparingDouble(this::getFovToEntity));
        }

        target = targets.get(0);
    }

    float lastYaw, lastPitch;

    private void updateRotation(boolean attack, float rotationYawSpeed, float rotationPitchSpeed) {
        double heightOffset;
        switch (currentHitPart) {
            case "head" -> heightOffset = target.getHeight() * 0.9;
            case "torso" -> heightOffset = target.getHeight() * 0.5;
            case "arms" -> heightOffset = target.getHeight() * 0.7;
            case "legs" -> heightOffset = target.getHeight() * 0.3;
            default -> heightOffset = target.getHeight() * 0.5;
        }

        Vector3d vec = target.getPositionVec().add(0, clamp(mc.player.getPosYEye() - target.getPosY(),
                        0, heightOffset), 0)
                .subtract(mc.player.getEyePosition(1.0F));

        isRotated = true;

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, hypot(vec.x, vec.z))));

        float yawDelta = (wrapDegrees(yawToTarget - rotateVector.x));
        float pitchDelta = (wrapDegrees(pitchToTarget - rotateVector.y));

        switch (type.get()) {
            case "FunTime" -> {
                float yaw;
                float pitch;
                if (attack && selected != target && options.getValueByName("Ускорять ротацию при атаке").get()) {
                    yaw = rotateVector.x + yawDelta;
                    pitch = clamp(rotateVector.y + pitchDelta, -89.0F, 89.0F);
                } else {
                    float yawSpeed = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed * 2.5f);
                    float pitchSpeed = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed * 2.5f);
                    yaw = rotateVector.x + (yawDelta > 0 ? yawSpeed : -yawSpeed);
                    pitch = clamp(rotateVector.y + (pitchDelta > 0 ? pitchSpeed : -pitchSpeed), -89.0F, 89.0F);
                }
                float shakeIntensity = 0.2f;
                float shakeFrequency = 0.02f;
                if (mc.player.ticksExisted % Math.max(1, (int) (shakeFrequency * 10)) == 0) {
                    yaw += (float) (Math.random() - 0.1) * shakeIntensity;
                    pitch += (float) (Math.random() - 0.5) * shakeIntensity;
                }
                float circleAmplitude = 13.4f;
                float circleSpeed = 1.2f;
                float time = mc.player.ticksExisted * circleSpeed;
                yaw += (float) Math.sin(time) * circleAmplitude;
                pitch += (float) Math.cos(time) * circleAmplitude;
                yaw += (float) (Math.random() - 0.5) * 0.05f;
                pitch += (float) (Math.random() - 0.5) * 0.05f;
                float gcd = SensUtils.getGCDValue();
                float gcdRandomizer = (float) (Math.random() * 0.01f + 0.995f);
                yaw -= (yaw - rotateVector.x) % (gcd * gcdRandomizer);
                pitch -= (pitch - rotateVector.y) % (gcd * gcdRandomizer);
                float maxYawChange = 46.0f;
                float maxPitchChange = 42.0f;
                yaw = rotateVector.x + clamp(yaw - rotateVector.x, -maxYawChange, maxPitchChange);
                pitch = clamp(rotateVector.y + clamp(pitch - rotateVector.y, -maxPitchChange, maxPitchChange), -89.0F, 89.0F);
                rotateVector = new Vector2f(yaw, pitch);
                lastYaw = yaw;
                lastPitch = pitch;
                if (options.getValueByName("Коррекция движения").get()) {
                    mc.player.rotationYawOffset = yaw;
                }
            }
            case "Grim" -> {
                float yaw;
                float pitch;
                if (attack && selected != target && options.getValueByName("Ускорять ротацию при атаке").get()) {
                    yaw = rotateVector.x + yawDelta;
                    pitch = clamp(rotateVector.y + pitchDelta, -89.0F, 89.0F);
                } else {
                    float yawSpeed = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed * 2.5f);
                    float pitchSpeed = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed * 2.5f);
                    yaw = rotateVector.x + (yawDelta > 0 ? yawSpeed : -yawSpeed);
                    pitch = clamp(rotateVector.y + (pitchDelta > 0 ? pitchSpeed : -pitchSpeed), -89.0F, 89.0F);
                }
                float shakeIntensity = 0.08f;
                float shakeFrequency = 0.05f;
                if (mc.player.ticksExisted % Math.max(1, (int) (shakeFrequency * 20)) == 0) {
                    yaw += (float) (Math.random() - 0.5) * shakeIntensity;
                    pitch += (float) (Math.random() - 0.5) * shakeIntensity;
                }
                float circleAmplitude = 4.2f;
                float circleSpeed = 0.6f;
                float time = mc.player.ticksExisted * circleSpeed;
                yaw += (float) Math.sin(time) * circleAmplitude;
                pitch += (float) Math.cos(time) * circleAmplitude;
                yaw += (float) (Math.random() - 0.5) * 0.02f;
                pitch += (float) (Math.random() - 0.5) * 0.02f;
                float gcd = SensUtils.getGCDValue();
                float gcdRandomizer = (float) (Math.random() * 0.005f + 0.9975f);
                yaw -= (yaw - rotateVector.x) % (gcd * gcdRandomizer);
                pitch -= (pitch - rotateVector.y) % (gcd * gcdRandomizer);
                float maxYawChange = 35.0f;
                float maxPitchChange = 30.0f;
                yaw = rotateVector.x + clamp(yaw - rotateVector.x, -maxYawChange, maxYawChange);
                pitch = clamp(rotateVector.y + clamp(pitch - rotateVector.y, -maxPitchChange, maxPitchChange), -89.0F, 89.0F);
                rotateVector = new Vector2f(yaw, pitch);
                lastYaw = yaw;
                lastPitch = pitch;
                if (options.getValueByName("Коррекция движения").get()) {
                    mc.player.rotationYawOffset = yaw;
                }
            }
            case "SpookyTime" -> {
                float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1f), 205);
                float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1f), 205);

                yawDelta = rotateVector.x + (yawDelta > 0 ? clampedYaw : -clampedYaw) + ThreadLocalRandom.current().nextFloat(-3f, 3f);
                pitchDelta = clamp(rotateVector.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -85.0F, 85.0F) + ThreadLocalRandom.current().nextFloat(-3f, 3f);

                if (!shouldPlayerFalling()) {
                    yawDelta = rotateVector.x + (mc.player.rotationYaw - rotateVector.x) / 1.5f + ThreadLocalRandom.current().nextFloat(-3, 3f);
                    pitchDelta = clamp(rotateVector.y + (mc.player.rotationPitch - rotateVector.y) / 1.5f, -85, 85) + ThreadLocalRandom.current().nextFloat(-3f, 3f);
                }

                float gcd = SensUtils.getGCDValue();
                yawDelta -= (yawDelta - rotateVector.x) % gcd;
                pitchDelta -= (pitchDelta - rotateVector.y) % gcd;
                rotateVector = new Vector2f(yawDelta, pitchDelta);

                if (options.getValueByName("Коррекция движения").get()) {
                    mc.player.rotationYawOffset = yawDelta;
                }
            }
            case "Reallyworld" -> {
                Vector3d vecToTarget = target.getPositionVec().add(0, clamp(mc.player.getPosYEye() - target.getPosY(),
                                0, target.getHeight() * (mc.player.getDistanceEyePos(target) / attackRange.get())), 0)
                        .subtract(mc.player.getEyePosition(1.0F));

                float yawToTargetNew = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToTarget.z, vecToTarget.x)) - 90);
                float pitchToTargetNew = (float) (-Math.toDegrees(Math.atan2(vecToTarget.y, hypot(vecToTarget.x, vecToTarget.z))));

                float yawDeltaNew = wrapDegrees(yawToTargetNew - rotateVector.x);
                float pitchDeltaNew = wrapDegrees(pitchToTargetNew - rotateVector.y);

                float clampedYaw = Math.min(Math.max(Math.abs(yawDeltaNew), 1.0f), rotationYawSpeed);
                float clampedPitch = Math.min(Math.max(Math.abs(pitchDeltaNew), 1.0f), rotationPitchSpeed);

                if (attack && selected != target && options.getValueByName("Ускорять ротацию при атаке").get()) {
                    clampedPitch = Math.max(Math.abs(pitchDeltaNew), 1.0f);
                } else {
                    clampedPitch /= 3f;
                }

                if (Math.abs(clampedYaw - this.lastYaw) <= 3.0f) {
                    clampedYaw = this.lastYaw + 3.1f;
                }

                float yaw = rotateVector.x + (yawDeltaNew > 0 ? clampedYaw : -clampedYaw);
                float pitch = clamp(rotateVector.y + (pitchDeltaNew > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

                float gcd = SensUtils.getGCDValue();
                yaw -= (yaw - rotateVector.x) % gcd;
                pitch -= (pitch - rotateVector.y) % gcd;

                rotateVector = new Vector2f(yaw, pitch);
                lastYaw = clampedYaw;
                lastPitch = clampedPitch;

                if (options.getValueByName("Коррекция движения").get()) {
                    mc.player.rotationYawOffset = yaw;
                }
            }
            case "Плавная" -> {
                float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed);
                float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed);

                if (attack && selected != target && options.getValueByName("Ускорять ротацию при атаке").get()) {
                    clampedPitch = Math.max(Math.abs(pitchDelta), 1.0f);
                } else {
                    clampedPitch /= 3f;
                }


                if (Math.abs(clampedYaw - this.lastYaw) <= 3.0f) {
                    clampedYaw = this.lastYaw + 3.1f;
                }

                float yaw = rotateVector.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
                float pitch = clamp(rotateVector.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);


                float gcd = SensUtils.getGCDValue();
                yaw -= (yaw - rotateVector.x) % gcd;
                pitch -= (pitch - rotateVector.y) % gcd;

                rotateVector = new Vector2f(yaw, pitch);
                lastYaw = clampedYaw;
                lastPitch = clampedPitch;
                if (options.getValueByName("Коррекция движения").get()) {
                    mc.player.rotationYawOffset = yaw;
                }
            }
        }
    }
    private void updateAttack() {
        float attackDistance = attackRange.get();
        selected = target; // Упрощаем логику

        if (options.getValueByName("Ускорять ротацию при атаке").get()) {
            updateRotation(true, 60, 35);
        }

        if (mc.player.getDistanceEyePos(target) > attackDistance) {
            return;
        }

        if (mc.player.isBlocking() && options.getValueByName("Отжимать щит").get()) {
            mc.playerController.onStoppedUsingItem(mc.player);
        }

        boolean wasSprinting = mc.player.isSprinting();
        if (!sprintResetType.is("Выключен") && wasSprinting) {
            if (sprintResetType.is("Обычный")) {
                mc.player.setSprinting(false);
            } else if (sprintResetType.is("Незаметный")) {
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
            }
        }

        boolean needShieldBreak = options.getValueByName("Ломать щит").get() && isTargetBlocking();

        if (needShieldBreak) {
            int axeSlot = findAxeSlot();
            if (axeSlot != -1) {
                previousSlot = mc.player.inventory.currentItem;
                switchToSlot(axeSlot);
            }
        }

        stopWatch.setLastMS(500);
        mc.playerController.attackEntity(mc.player, target);
        mc.player.swingArm(Hand.MAIN_HAND);
        startHeadReaction();

        if (needShieldBreak && previousSlot != -1) {
            switchToSlot(previousSlot);
            previousSlot = -1;
        }

        if (!sprintResetType.is("Выключен") && wasSprinting) {
            if (sprintResetType.is("Обычный")) {
                mc.player.setSprinting(true);
            } else if (sprintResetType.is("Незаметный")) {
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_SPRINTING));
            }
        }

        hitCounter++;
        if (hitCounter >= 2) {
            String[] hitParts = {"head", "torso", "arms", "legs"};
            currentHitPart = hitParts[random.nextInt(hitParts.length)];
            hitCounter = 0;
        }
    }

    // Остальной код остается без изменений...
    // [Все остальные методы остаются такими же как в оригинальном KillAura

    // Остальные методы остаются без изменений...
    private boolean canAttack() {
        if (stopWatch.hasTimeElapsed() ||
                mc.player.getDistanceEyePos(target) >= attackRange.get() + (type.is("Reallyworld") ? preRange.get() : 0.0f) ||
                mc.player.getCooledAttackStrength(options.getValueByName("Синхронизировать атаку с ТПС").get()
                        ? onelove.getInstance().getTpsCalc().getAdjustTicks() : 1.5f) <= 0.93F) {
            return false;
        }

        if (mc.player.isInWater() && mc.player.areEyesInFluid(FluidTags.WATER) || mc.player.isInLava() ||
                mc.player.isOnLadder() || mc.player.isPassenger() ||
                mc.player.abilities.isFlying || mc.player.isElytraFlying() ||
                !options.getValueByName("Только криты").get()) {
            return true;
        }

        return mc.player.fallDistance > 0 && !mc.player.isOnGround();
    }

    private void startHeadReaction() {
        isHeadReacting = true;
        isReturningToTarget = false;
        headReactionProgress = 0f;

        headReactionDirection = !headReactionDirection;

        float reactionAngle = 30f + random.nextFloat() * 15f;
        targetHeadYaw = headReactionDirection ? reactionAngle : -reactionAngle;

        headReactionTimer.reset();
    }

    private boolean isTargetBlocking() {
        if (target == null) return false;

        if (target.isHandActive()) {
            ItemStack activeItem = target.getActiveItemStack();
            return activeItem.getItem() == Items.SHIELD;
        }

        return false;
    }

    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof AxeItem || stack.getItem() instanceof SwordItem) {
                return i;
            }
        }
        return -1;
    }

    private void switchToSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            mc.player.inventory.currentItem = slot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
        }
    }

    private boolean shouldPlayerFalling() {
        boolean cancelReason = mc.player.isInWater() && mc.player.areEyesInFluid(FluidTags.WATER) || mc.player.isInLava() || mc.player.isOnLadder() || mc.player.isPassenger() || mc.player.abilities.isFlying;

        float attackStrength = mc.player.getCooledAttackStrength(options.getValueByName("Синхронизировать атаку с ТПС").get()
                ? onelove.getInstance().getTpsCalc().getAdjustTicks() : 1.5f);

        if (attackStrength < 0.92f) {
            return false;
        }

        if (!cancelReason && options.getValueByName("Только криты").get()) {
            return !mc.player.isOnGround() && mc.player.fallDistance > 0;
        }

        return true;
    }

    private boolean isValid(LivingEntity entity) {
        if (entity instanceof ClientPlayerEntity) return false;

        if (entity.ticksExisted < 3) return false;

        if (mc.player.getDistanceEyePos(entity) > attackRange.get() + (type.is("Reallyworld") ? preRange.get() : 0.0f)) return false;

        if (options.getValueByName("Не бить через стены").get() && !hasClearLineOfSight(entity)) {
            return false;
        }

        if (entity instanceof PlayerEntity p) {
            // Временно убираем AntiBot проверку
            if (!targets.getValueByName("Друзья").get() && FriendStorage.isFriend(p.getName().getString())) {
                return false;
            }
            if (p.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        }

        if (entity instanceof PlayerEntity && !targets.getValueByName("Игроки").get()) {
            return false;
        }
        if (entity instanceof PlayerEntity && entity.getTotalArmorValue() == 0 && !targets.getValueByName("Голые").get()) {
            return false;
        }
        if (entity instanceof PlayerEntity && entity.isInvisible() && entity.getTotalArmorValue() == 0 && !targets.getValueByName("Голые невидимки").get()) {
            return false;
        }
        if (entity instanceof PlayerEntity && entity.isInvisible() && !targets.getValueByName("Невидимки").get()) {
            return false;
        }

        if (entity instanceof MonsterEntity && !targets.getValueByName("Мобы").get()) {
            return false;
        }
        if (entity instanceof AnimalEntity && !targets.getValueByName("Животные").get()) {
            return false;
        }

        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
    }

    private void reset() {
        if (options.getValueByName("Коррекция движения").get()) {
            mc.player.rotationYawOffset = Integer.MIN_VALUE;
        }
        rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        hitCounter = 0;
        currentHitPart = "head";
        previousSlot = -1;
        isHeadReacting = false;
        isReturningToTarget = false;
        headReactionProgress = 0f;
        targetHeadYaw = 0f;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        reset();
        target = null;
        return false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
        stopWatch.setLastMS(0);
        target = null;
    }

    private double getEntityArmor(PlayerEntity entityPlayer2) {
        double d2 = 0.0;
        for (int i2 = 0; i2 < 4; ++i2) {
            ItemStack is = entityPlayer2.inventory.armorInventory.get(i2);
            if (!(is.getItem() instanceof ArmorItem)) continue;
            d2 += getProtectionLvl(is);
        }
        return d2;
    }

    private double getProtectionLvl(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem i) {
            double damageReduceAmount = i.getDamageReduceAmount();
            if (stack.isEnchanted()) {
                damageReduceAmount += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
            }
            return damageReduceAmount;
        }
        return 0;
    }

    private double getEntityHealth(LivingEntity ent) {
        if (ent instanceof PlayerEntity player) {
            return (double) (player.getHealth() + player.getAbsorptionAmount()) * (getEntityArmor(player) / 20.0);
        }
        return ent.getHealth() + ent.getAbsorptionAmount();
    }

    private double getFovToEntity(LivingEntity entity) {
        Vector3d playerLook = mc.player.getLookVec();
        Vector3d toEntity = entity.getPositionVec().subtract(mc.player.getPositionVec()).normalize();
        double dot = playerLook.dotProduct(toEntity);
        return Math.acos(dot) * (180.0 / Math.PI);
    }

    private boolean isUsingItem() {
        if (!options.getValueByName("Не бить при использовании").get()) return false;
        ItemStack item = mc.player.getHeldItemMainhand();
        return mc.player.isHandActive() || item.getItem().isFood() || item.getItem() instanceof BowItem ||
                item.getItem() == Items.TRIDENT || item.getItem() == Items.POTION;
    }

    private boolean hasClearLineOfSight(LivingEntity entity) {
        Vector3d start = mc.player.getEyePosition(1.0F);
        Vector3d end = entity.getPositionVec().add(0, entity.getHeight() * 0.5, 0);
        RayTraceContext context = new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
        RayTraceResult result = mc.world.rayTraceBlocks(context);
        return result.getType() == RayTraceResult.Type.MISS;
    }
}