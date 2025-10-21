package ru.onelove.functions.impl.combat;

import com.google.common.eventbus.Subscribe;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@FunctionRegister(name = "KillAura1", type = Category.Combat)
public class KillAura1 extends Function {

    // Основные настройки
    private final SliderSetting attackRange = new SliderSetting("Дистанция атаки", 3.2f, 2.5f, 6.0f, 0.1f);
    private final SliderSetting rotationRange = new SliderSetting("Дистанция ротации", 3.0f, 0.5f, 6.0f, 0.1f);
    private final SliderSetting elytraRange = new SliderSetting("Элитра ротация", 10f, 1f, 30f, 0.1f);
    private final SliderSetting attackSpeed = new SliderSetting("Скорость атаки", 12.0f, 1.0f, 20.0f, 0.5f);
    private final SliderSetting reach = new SliderSetting("Досягаемость", 3.0f, 2.8f, 3.5f, 0.01f);

    // Система обходов
    private final ModeSetting bypassMode = new ModeSetting("Режим обхода", "Grim Advanced",
            "Grim Advanced", "Grim", "Matrix", "Intave", "FunTime", "Snap 1.17+ (funtime)", "Custom");

    private final SliderSetting rotationSpeed = new SliderSetting("Скорость ротации", 120.0f, 1.0f, 360.0f, 1.0f);
    private final SliderSetting smoothness = new SliderSetting("Плавность", 2.5f, 0.1f, 5.0f, 0.1f);
    private final SliderSetting humanize = new SliderSetting("Humanize", 1.2f, 0.5f, 3.0f, 0.1f);
    private final SliderSetting randomization = new SliderSetting("Рандомизация", 0.8f, 0.0f, 2.0f, 0.1f);

    // Настройки для Grim Advanced
    private final SliderSetting grimThreshold = new SliderSetting("Grim Threshold", 0.01f, 0.001f, 0.05f, 0.001f);
    private final SliderSetting grimAdvantage = new SliderSetting("Grim Advantage", 1.0f, 0.5f, 4.0f, 0.1f);
    private final SliderSetting grimCeiling = new SliderSetting("Grim Ceiling", 4.0f, 1.0f, 8.0f, 0.1f);
    private final SliderSetting grimReachThreshold = new SliderSetting("Grim Reach", 0.0005f, 0.0001f, 0.001f, 0.0001f);
    private final SliderSetting grimNoSlowThreshold = new SliderSetting("NoSlow Threshold", 0.035f, 0.01f, 0.1f, 0.001f);

    // Настройки для конкретных античитов
    private final SliderSetting grimOffset = new SliderSetting("Grim Offset", 0.97f, 0.8f, 1.2f, 0.001f);
    private final SliderSetting matrixTick = new SliderSetting("Matrix Tick", 2.0f, 1.0f, 5.0f, 0.1f);
    private final SliderSetting funTimeSpeed = new SliderSetting("FunTime Speed", 1.0f, 0.5f, 2.0f, 0.1f);
    private final SliderSetting snapSmoothness = new SliderSetting("Snap Smoothness", 0.6f, 0.1f, 1.0f, 0.01f);
    private final SliderSetting snapRandomization = new SliderSetting("Snap Random", 0.5f, 0.0f, 2.0f, 0.1f);

    // Цели
    final ModeListSetting targets = new ModeListSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Голые невидимки", true),
            new BooleanSetting("Невидимки", true),
            new BooleanSetting("Сквозь стены", false));

    // Опции для Grim Advanced
    final ModeListSetting options = new ModeListSetting("Опции",
            new BooleanSetting("Критические удары", true),
            new BooleanSetting("Ломать щиты", true),
            new BooleanSetting("Отжимать щит", true),
            new BooleanSetting("Коррекция движения", true),
            new BooleanSetting("Приоритет слабых", true),
            new BooleanSetting("Быстрая смена цели", false),
            new BooleanSetting("Игнорировать ботов", true),
            new BooleanSetting("Только при нажатии", false),
            new BooleanSetting("Умное предсказание", true),
            new BooleanSetting("Ускорять ротацию при атаке", false),
            new BooleanSetting("Синхронизировать атаку с ТПС", false),
            new BooleanSetting("Фокусировать одну цель", true),
            new BooleanSetting("Случайная точка прицеливания", false),
            new BooleanSetting("Grim: Избегать Aim", true),
            new BooleanSetting("Grim: Избегать Reach", true),
            new BooleanSetting("Grim: Избегать NoSlow", true),
            new BooleanSetting("Grim: Избегать Timer", true),
            new BooleanSetting("Grim: Избегать Knockback", true),
            new BooleanSetting("Grim: Случайные паузы", true),
            new BooleanSetting("Grim: Рандомный порядок атак", true));

    private final ModeSetting correctionType = new ModeSetting("Тип коррекции", "Незаметный", "Незаметный", "Сфокусированный");

    @Getter
    private final StopWatch attackTimer = new StopWatch();
    private final StopWatch rotationTimer = new StopWatch();
    private final StopWatch grimPauseTimer = new StopWatch();
    private Vector2f rotateVector = new Vector2f(0, 0);
    @Getter
    private LivingEntity target;
    private final Random random = new Random();
    private int attackCounter = 0;
    private int rotationTicks = 0;
    private double lastMotionX = 0;
    private double lastMotionZ = 0;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private Entity selected;
    private int ticks = 0;
    private boolean isRotated;
    private int grimAttackPattern = 0;
    private boolean grimPaused = false;

    public KillAura1() {
        addSettings(attackRange, rotationRange, elytraRange, attackSpeed, reach, bypassMode,
                rotationSpeed, smoothness, humanize, randomization, grimThreshold,
                grimAdvantage, grimCeiling, grimReachThreshold, grimNoSlowThreshold,
                grimOffset, matrixTick, funTimeSpeed, snapSmoothness, snapRandomization,
                targets, options, correctionType);
    }

    @Subscribe
    public void onInput(EventInput eventInput) {
        if (options.getValueByName("Коррекция движения").get() && target != null) {
            if (correctionType.is("Незаметный")) {
                MoveUtils.fixMovement(eventInput, rotateVector.x);
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (options.getValueByName("Фокусировать одну цель").get() && (target == null || !isValid(target)) ||
                !options.getValueByName("Фокусировать одну цель").get()) {
            updateTarget();
        }

        handleGrimPauses();

        if (target != null && shouldWork() && !grimPaused) {
            isRotated = true;
            handleRotation();
            handleBypassSpecifics();

            if (shouldAttack()) {
                performAttack();
            }
        } else {
            resetRotation();
            isRotated = false;
        }
    }

    private void handleGrimPauses() {
        if (bypassMode.is("Grim Advanced") && options.getValueByName("Grim: Случайные паузы").get()) {
            long currentTime = System.currentTimeMillis();
            long pauseInterval = 2000 + random.nextInt(1500);
            long resumeInterval = 500 + random.nextInt(300);

            if (!grimPaused && currentTime - grimPauseTimer.getLastMS() > pauseInterval && random.nextFloat() < 0.3f) {
                grimPaused = true;
                grimPauseTimer.reset();
            } else if (grimPaused && currentTime - grimPauseTimer.getLastMS() > resumeInterval) {
                grimPaused = false;
            }
        } else {
            grimPaused = false;
        }
    }

    @Subscribe
    private void onWalking(EventMotion e) {
        if (target == null || !shouldWork() || grimPaused) return;

        e.setYaw(rotateVector.x);
        e.setPitch(rotateVector.y);

        mc.player.rotationYawHead = rotateVector.x;
        mc.player.renderYawOffset = rotateVector.x;
        mc.player.rotationPitchHead = rotateVector.y;
    }

    private boolean shouldWork() {
        if (options.getValueByName("Только при нажатии").get()) {
            return mc.gameSettings.keyBindAttack.isKeyDown();
        }
        return true;
    }

    private void updateTarget() {
        List<LivingEntity> validTargets = new ArrayList<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                if (isValid(living)) {
                    validTargets.add(living);
                }
            }
        }

        if (validTargets.isEmpty()) {
            target = null;
            return;
        }

        if (options.getValueByName("Приоритет слабых").get()) {
            validTargets.sort(Comparator.comparingDouble(this::getTargetPriority));
        } else {
            validTargets.sort(Comparator.comparingDouble(entity -> mc.player.getDistance(entity)));
        }

        target = validTargets.get(0);
    }

    private double getTargetPriority(LivingEntity entity) {
        double priority = 0.0;
        priority += (20.0 - (entity.getHealth() + entity.getAbsorptionAmount())) * 10.0;

        if (entity instanceof PlayerEntity) {
            priority += (20.0 - entity.getTotalArmorValue()) * 5.0;
        }

        double distance = mc.player.getDistance(entity);
        priority -= distance * 2.0;

        return priority;
    }

    private void handleRotation() {
        Vector3d targetPos = getTargetHitboxPosition();
        Vector3d eyesPos = mc.player.getEyePosition(1.0F);
        Vector3d diff = targetPos.subtract(eyesPos);

        double x = diff.x;
        double z = diff.z;

        float targetYaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(x * x + z * z)));

        applyBypassRotation(targetYaw, targetPitch);
    }

    private Vector3d getTargetHitboxPosition() {
        double x = target.getPosX();
        double y = target.getPosY() + target.getEyeHeight() * 0.85;
        double z = target.getPosZ();

        if (options.getValueByName("Умное предсказание").get()) {
            double prediction = getPredictionValue();
            x += target.getMotion().x * prediction;
            z += target.getMotion().z * prediction;

            x += (mc.player.getMotion().x - lastMotionX) * 0.3;
            z += (mc.player.getMotion().z - lastMotionZ) * 0.3;
        }

        lastMotionX = mc.player.getMotion().x;
        lastMotionZ = mc.player.getMotion().z;

        return new Vector3d(x, y, z);
    }

    private double getPredictionValue() {
        switch (bypassMode.get()) {
            case "Grim Advanced": return 1.7 * grimOffset.get();
            case "Grim": return 1.8 * grimOffset.get();
            case "Matrix": return 1.5;
            case "Intave": return 1.3;
            case "FunTime": return 1.4;
            case "Snap 1.17+ (funtime)": return 1.6;
            case "Custom": return 1.6;
            default: return 1.5;
        }
    }

    private void applyBypassRotation(float targetYaw, float targetPitch) {
        float currentYaw = rotateVector.x;
        float currentPitch = rotateVector.y;

        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = wrapDegrees(targetPitch - currentPitch);

        switch (bypassMode.get()) {
            case "Grim Advanced":
                applyGrimAdvancedRotation(yawDiff, pitchDiff);
                break;
            case "Grim":
                applyGrimRotation(yawDiff, pitchDiff);
                break;
            case "Matrix":
                applyMatrixRotation(yawDiff, pitchDiff);
                break;
            case "Intave":
                applyIntaveRotation(yawDiff, pitchDiff);
                break;
            case "FunTime":
                applyFunTimeRotation(yawDiff, pitchDiff, shouldAttack());
                break;
            case "Snap 1.17+ (funtime)":
                applySnap117Rotation();
                break;
            default:
                applyCustomRotation(yawDiff, pitchDiff);
                break;
        }
    }

    private void applyGrimAdvancedRotation(float yawDiff, float pitchDiff) {
        float speed = rotationSpeed.get() * humanize.get() * 0.8f;
        float smooth = smoothness.get() * 1.8f;

        float humanErrorYaw = (random.nextFloat() - 0.5f) * randomization.get() * 0.3f;
        float humanErrorPitch = (random.nextFloat() - 0.5f) * randomization.get() * 0.2f;

        float maxChange = grimThreshold.get() * 100f;
        float yawChange = clamp(yawDiff + humanErrorYaw, -maxChange, maxChange) / smooth;
        float pitchChange = clamp(pitchDiff + humanErrorPitch, -maxChange * 0.7f, maxChange * 0.7f) / smooth;

        float newYaw = rotateVector.x + yawChange;
        float newPitch = rotateVector.y + pitchChange;

        float gcd = SensUtils.getGCDValue();
        if (options.getValueByName("Grim: Избегать Aim").get()) {
            newYaw -= (newYaw - rotateVector.x) % gcd;
            newPitch -= (newPitch - rotateVector.y) % gcd;
        }

        rotateVector = new Vector2f(newYaw, clamp(newPitch, -90.0F, 90.0F));
    }

    private void applyGrimRotation(float yawDiff, float pitchDiff) {
        float speed = rotationSpeed.get() * humanize.get();
        float smooth = smoothness.get();

        float newYaw = rotateVector.x + clamp(yawDiff, -speed, speed) / (smooth * 1.5f);
        float newPitch = rotateVector.y + clamp(pitchDiff, -speed, speed) / (smooth * 1.5f);

        newYaw += (random.nextFloat() - 0.5f) * randomization.get();
        newPitch += (random.nextFloat() - 0.5f) * randomization.get() * 0.5f;

        rotateVector = new Vector2f(newYaw, clamp(newPitch, -90.0F, 90.0F));
    }

    private void applyMatrixRotation(float yawDiff, float pitchDiff) {
        if (rotationTicks++ % (int) Math.floor(matrixTick.get()) == 0) {
            float speed = rotationSpeed.get();
            float newYaw = rotateVector.x + clamp(yawDiff, -speed, speed) * 0.8f;
            float newPitch = rotateVector.y + clamp(pitchDiff, -speed, speed) * 0.8f;
            rotateVector = new Vector2f(newYaw, clamp(newPitch, -90.0F, 90.0F));
        }
    }

    private void applyIntaveRotation(float yawDiff, float pitchDiff) {
        float speed = rotationSpeed.get() * 0.7f;
        float smooth = smoothness.get() * 1.2f;

        if (System.currentTimeMillis() - rotationTimer.getLastMS() > (50 + random.nextInt(30))) {
            float newYaw = rotateVector.x + clamp(yawDiff, -speed, speed) / smooth;
            float newPitch = rotateVector.y + clamp(pitchDiff, -speed, speed) / smooth;
            rotateVector = new Vector2f(newYaw, clamp(newPitch, -90.0F, 90.0F));
            rotationTimer.reset();
        }
    }

    private void applyFunTimeRotation(float yawDiff, float pitchDiff, boolean attack) {
        float rotationYawSpeed = rotationSpeed.get() * funTimeSpeed.get();
        float rotationPitchSpeed = rotationSpeed.get() * funTimeSpeed.get() * 0.5f;

        float clampedYaw = Math.min(Math.max(Math.abs(yawDiff), 1.0F), rotationYawSpeed);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDiff) * 0.33f, 1.0f), rotationPitchSpeed);

        if (attack && this.selected != this.target && options.getValueByName("Ускорять ротацию при атаке").get()) {
            clampedPitch = Math.max(Math.abs(pitchDiff), 0.5F);
        } else {
            clampedPitch /= 0.2f;
        }

        if (Math.abs(clampedYaw - this.lastYaw) <= 5.0F) {
            clampedYaw = this.lastYaw + 15F;
        }

        float yaw = this.rotateVector.x + (yawDiff > 0.0F ? clampedYaw : -clampedYaw);
        float pitch = clamp(rotateVector.y + (pitchDiff > 0 ? clampedPitch : -clampedPitch), -90, 90);

        float gcd = SensUtils.getGCDValue();
        yaw -= (yaw - this.rotateVector.x) % gcd;
        pitch -= (pitch - this.rotateVector.y) % gcd;

        this.rotateVector = new Vector2f(yaw, pitch);
        this.lastYaw = clampedYaw;
        this.lastPitch = clampedPitch;

        if (options.getValueByName("Коррекция движения").get()) {
            mc.player.rotationYawOffset = yaw;
        }
    }

    private void applySnap117Rotation() {
        if (target == null) return;
        if (!isRotated) return;

        AxisAlignedBB targetBoundingBox = target.getBoundingBox();

        double playerX = mc.player.getPosX();
        double playerY = mc.player.getPosY();
        double playerZ = mc.player.getPosZ();

        double targetX, targetY, targetZ;

        if (options.getValueByName("Случайная точка прицеливания").get()) {
            targetX = targetBoundingBox.minX + (random.nextDouble() * (targetBoundingBox.maxX - targetBoundingBox.minX));
            targetY = targetBoundingBox.minY + (random.nextDouble() * (targetBoundingBox.maxY - targetBoundingBox.minY));
            targetZ = targetBoundingBox.minZ + (random.nextDouble() * (targetBoundingBox.maxZ - targetBoundingBox.minZ));
        } else {
            targetX = (targetBoundingBox.minX + targetBoundingBox.maxX) / 2;
            targetY = (targetBoundingBox.minY + targetBoundingBox.maxY) / 2;
            targetZ = (targetBoundingBox.minZ + targetBoundingBox.maxZ) / 2;
        }

        float yawToTarget = (float) Math.toDegrees(Math.atan2(targetZ - playerZ, targetX - playerX)) - 90;
        float pitchToTarget = (float) -Math.toDegrees(Math.atan2(
                targetY - (playerY + mc.player.getEyeHeight()),
                Math.sqrt(Math.pow(targetX - playerX, 2) + Math.pow(targetZ - playerZ, 2))
        ));

        yawToTarget = normalizeAngle(yawToTarget - mc.player.rotationYaw) + mc.player.rotationYaw;

        float smoothFactorYaw = snapSmoothness.get();
        float smoothFactorPitch = snapSmoothness.get() * 0.7f;

        float randomYawMicro = (float) (random.nextDouble() * snapRandomization.get() * 0.6 - snapRandomization.get() * 0.3);
        float randomPitchMicro = (float) (random.nextDouble() * snapRandomization.get() * 0.5 - snapRandomization.get() * 0.25);

        yawToTarget += randomYawMicro;
        pitchToTarget += randomPitchMicro;

        float randomYawOffset = (float) (random.nextDouble() * snapRandomization.get() * 0.2 - snapRandomization.get() * 0.1);
        randomYawOffset *= 1.2f;
        yawToTarget += randomYawOffset;

        float randomPitchOffset = (float) (random.nextDouble() * snapRandomization.get() * 0.8 - snapRandomization.get() * 0.4);
        randomPitchOffset *= 0.9f;
        pitchToTarget += randomPitchOffset;

        float currentYaw = rotateVector.x;
        float currentPitch = rotateVector.y;

        currentYaw += (yawToTarget - currentYaw) * smoothFactorYaw;
        currentPitch += (pitchToTarget - currentPitch) * smoothFactorPitch;

        float gcd = SensUtils.getGCDValue();
        currentYaw -= (currentYaw - rotateVector.x) % gcd;
        currentPitch -= (currentPitch - rotateVector.y) % gcd;

        rotateVector = new Vector2f(currentYaw, clamp(currentPitch, -90.0F, 90.0F));
    }

    private float normalizeAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }
        if (angle < -180.0F) {
            angle += 360.0F;
        }
        return angle;
    }

    private void applyCustomRotation(float yawDiff, float pitchDiff) {
        float speed = rotationSpeed.get() * humanize.get();
        float smooth = smoothness.get();

        float newYaw = rotateVector.x + clamp(yawDiff, -speed, speed) / smooth;
        float newPitch = rotateVector.y + clamp(pitchDiff, -speed, speed) / smooth;

        rotateVector = new Vector2f(newYaw, clamp(newPitch, -90.0F, 90.0F));
    }

    private void handleBypassSpecifics() {
        switch (bypassMode.get()) {
            case "Grim Advanced":
                if (options.getValueByName("Быстрая смена цели").get()) {
                    options.getValueByName("Быстрая смена цели").set(false);
                }
                if (options.getValueByName("Случайная точка прицеливания").get()) {
                    options.getValueByName("Случайная точка прицеливания").set(false);
                }
                break;
        }
    }

    private boolean shouldAttack() {
        if (target == null || !shouldWork() || grimPaused) return false;

        if (bypassMode.is("Grim Advanced")) {
            double distance = mc.player.getDistance(target);
            double maxReach = reach.get() + (options.getValueByName("Grim: Избегать Reach").get() ?
                    -grimReachThreshold.get() : 0);

            if (distance > maxReach) {
                return false;
            }

            if (options.getValueByName("Grim: Рандомный порядок атак").get()) {
                if (grimAttackPattern == 0 && random.nextFloat() < 0.2f) {
                    return false;
                }
            }
        }

        float attackStrength = mc.player.getCooledAttackStrength(0.0f);
        if (attackStrength < getRequiredStrength()) {
            return false;
        }

        double distance = mc.player.getDistance(target);
        if (distance > reach.get()) {
            return false;
        }

        float attacksPerSecond = attackSpeed.get() * getSpeedMultiplier();
        long requiredDelay = (long) (1000.0f / attacksPerSecond);
        if (System.currentTimeMillis() - attackTimer.getLastMS() < requiredDelay) {
            return false;
        }

        if (!passBypassChecks()) {
            return false;
        }

        return true;
    }

    private float getRequiredStrength() {
        switch (bypassMode.get()) {
            case "Grim Advanced": return 0.97f;
            case "Grim": return 0.95f;
            case "Matrix": return 0.92f;
            case "Intave": return 0.98f;
            case "FunTime": return 0.93f;
            case "Snap 1.17+ (funtime)": return 0.94f;
            default: return 0.9f;
        }
    }

    private float getSpeedMultiplier() {
        switch (bypassMode.get()) {
            case "Grim Advanced": return 0.85f;
            case "Grim": return 0.9f;
            case "Matrix": return 0.8f;
            case "Intave": return 1.0f;
            case "FunTime": return 1.1f;
            case "Snap 1.17+ (funtime)": return 1.05f;
            default: return 1.0f;
        }
    }

    private boolean passBypassChecks() {
        switch (bypassMode.get()) {
            case "Grim Advanced":
                if (options.getValueByName("Grim: Избегать Aim").get() && mc.player.ticksExisted % 4 == 0) {
                    return false;
                }
                if (options.getValueByName("Grim: Избегать Timer").get() && attackCounter % 3 == 0) {
                    return false;
                }
                if (options.getValueByName("Grim: Избегать Knockback").get() && mc.player.hurtTime > 0) {
                    return false;
                }
                return mc.player.ticksExisted % 5 != 0;
            case "Grim":
                return mc.player.ticksExisted % 3 != 0;
            case "Matrix":
                return attackCounter % 2 == 0;
            case "FunTime":
                return mc.player.ticksExisted % 4 != 0;
            case "Snap 1.17+ (funtime)":
                return mc.player.ticksExisted % 5 != 0;
            default:
                return true;
        }
    }

    private void performAttack() {
        mc.playerController.attackEntity(mc.player, target);
        mc.player.swingArm(Hand.MAIN_HAND);

        attackTimer.reset();
        attackCounter++;

        if (bypassMode.is("Grim Advanced") && options.getValueByName("Grim: Рандомный порядок атак").get()) {
            grimAttackPattern = (grimAttackPattern + 1) % 5;
            if (random.nextFloat() < 0.3f) {
                grimAttackPattern = random.nextInt(5);
            }
        }
    }

    private void resetRotation() {
        rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        if (options.getValueByName("Коррекция движения").get()) {
            mc.player.rotationYawOffset = Integer.MIN_VALUE;
        }
    }

    private boolean isValid(LivingEntity entity) {
        if (entity == null || entity == mc.player) return false;
        if (entity.ticksExisted < 10) return false;
        if (mc.player.getDistance(entity) > attackRange.get() + 1.0f) return false;
        if (!entity.isAlive() || entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStandEntity) return false;

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (!targets.getValueByName("Друзья").get() && FriendStorage.isFriend(player.getName().getString())) return false;
            if (!targets.getValueByName("Игроки").get()) return false;
            if (!targets.getValueByName("Невидимки").get() && player.isInvisible()) return false;
            if (!targets.getValueByName("Голые").get() && player.getTotalArmorValue() == 0) return false;
            if (!targets.getValueByName("Голые невидимки").get() && player.isInvisible() && player.getTotalArmorValue() == 0) return false;

            if (options.getValueByName("Игнорировать ботов").get() && isBot(player)) {
                return false;
            }
        }

        if (entity instanceof MonsterEntity && !targets.getValueByName("Мобы").get()) return false;
        if (entity instanceof AnimalEntity && !targets.getValueByName("Животные").get()) return false;

        return true;
    }

    private boolean isBot(PlayerEntity player) {
        return player.getGameProfile().getName().matches(".*[0-9].*") ||
                player.ticksExisted < 20 ||
                player.getMotion().x == 0 && player.getMotion().z == 0 && player.ticksExisted > 100;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        resetRotation();
        target = null;
        attackTimer.reset();
        rotationTimer.reset();
        grimPauseTimer.reset();
        attackCounter = 0;
        rotationTicks = 0;
        lastYaw = 0;
        lastPitch = 0;
        isRotated = false;
        grimAttackPattern = 0;
        grimPaused = false;
        return false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetRotation();
        target = null;
    }
}