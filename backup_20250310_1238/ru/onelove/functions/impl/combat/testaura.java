package ru.onelove.functions.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.ModeListSetting;
import ru.onelove.functions.settings.impl.ModeSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.player.MoveUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@FunctionRegister(name = "testaura", type = Category.Combat)
public class testaura extends Function {

    private final ModeSetting mode = new ModeSetting("Тип атаки", "Плавная", "Плавная", "Снап");
    private final SliderSetting range = new SliderSetting("Дистанция", 3f, 3f, 6f, 0.05f);
    private final SliderSetting preRange = new SliderSetting("Дистанция предикции", 0.5f, 0.0f, 3.0f, 0.05f)
            .setVisible(() -> mode.is("Снап"));

    private final ModeListSetting targets = new ModeListSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Мобы", false)
    );

    private final ModeListSetting ignore = new ModeListSetting("Игнорировать",
            new BooleanSetting("Друзья", true),
            new BooleanSetting("Невидимки", false),
            new BooleanSetting("Голые", false),
            new BooleanSetting("Ники", true)
    );

    private final ModeSetting sort = new ModeSetting("Сортировка", "По дистанции", "По дистанции", "По здоровью", "По углу");

    public final ModeListSetting settings = new ModeListSetting("Опции",
            new BooleanSetting("Только криты", true),
            new BooleanSetting("Коррекция движения", true),
            new BooleanSetting("Не атаковать при использовании", false),
            new BooleanSetting("Ломать щит", true),
            new BooleanSetting("Ломать щит топором", true)
    );

    private final BooleanSetting space = new BooleanSetting("Только в воздухе", false).setVisible(() -> settings.getValueByName("Только криты").get());
    private final BooleanSetting silent = new BooleanSetting("Тихая коррекция", true).setVisible(() -> settings.getValueByName("Коррекция движения").get());

    public testaura() {
        addSettings(range, preRange, mode, targets, ignore, sort, settings, space, silent);
    }

    private long cpsLimit = 0;
    private LivingEntity target;
    private float rotYaw, rotPitch;
    private int ticks;

    @Subscribe
    public void onInput(ru.onelove.events.EventInput event) {
        if (settings.getValueByName("Коррекция движения").get() && silent.get()) {
            MoveUtils.fixMovement(event, rotYaw);
        }
    }

    @Subscribe
    public void onUpdate(ru.onelove.events.EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        target = findTarget();

        if (target == null) {
            reset();
            return;
        }

        switch (mode.get()) {
            case "Плавная" -> {
                updateRotation(0.5f);
                if (canAttack() && getMouseOver(target, rotYaw, rotPitch, range.get()) == target)
                    updateAttack(target);
            }
            case "Снап" -> {
                if (canAttack()) {
                    updateAttack(target);
                    ticks = 3;
                }

                if (ticks > 0) {
                    updateRotation(1);
                    ticks--;
                } else {
                    rotYaw = mc.player.rotationYaw;
                    rotPitch = mc.player.rotationPitch;
                }
            }
        }
    }

    @Subscribe
    public void onMotion(ru.onelove.events.EventMotion event) {
        if (target == null) return;

        event.setYaw(rotYaw);
        event.setPitch(rotPitch);
        updateClientRotation(rotYaw, rotPitch);
    }

    private void updateRotation(float speed) {
        if (isInHitBox(mc.player, target)) return;

        Vector3d vec3d = getVector3d(mc.player, target);

        float rawYaw = (float) wrapDegrees(toDegrees(atan2(vec3d.z, vec3d.x)) - 90);
        float rawPitch = (float) wrapDegrees(toDegrees(-atan2(vec3d.y, hypot(vec3d.x, vec3d.z))));

        float yawDelta = wrapDegrees(rawYaw - rotYaw);
        float pitchDelta = wrapDegrees(rawPitch - rotPitch);

        if (abs(yawDelta) > 180) yawDelta -= signum(yawDelta) * 360;

        float additionYaw = clamp(yawDelta, -180 * speed, 180 * speed);
        float additionPitch = clamp(pitchDelta, -90 * speed, 90 * speed);

        float yaw = rotYaw + additionYaw + ThreadLocalRandom.current().nextFloat(-1f, 1f);
        float pitch = rotPitch + additionPitch + ThreadLocalRandom.current().nextFloat(-1f, 1f);

        rotYaw = getSensitivity(yaw);
        rotPitch = getSensitivity(clamp(pitch, -89.0F, 89.0F));
    }

    private void updateAttack(LivingEntity target) {
        if (mc.player.isHandActive()) {
            if (settings.getValueByName("Ломать щит").get() && mc.player.isActiveItemStackBlocking()) {
                mc.playerController.onStoppedUsingItem(mc.player);
            } else if (settings.getValueByName("Не атаковать при использовании").get()) {
                return;
            }
        }

        boolean sprint = false;

        if (settings.getValueByName("Только криты").get() && mc.player.isSprinting()) {
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
            if (!mc.player.isInWater())
                mc.player.setSprinting(false);
            sprint = true;
        }

        cpsLimit = System.currentTimeMillis() + 550;
        attackEntity(target);

        if (settings.getValueByName("Ломать щит топором").get() && target.isBlocking()) {
            int slot = getAxeSlot(true);
            if (slot == -1) return;

            mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
            attackEntity(target);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
        }

        if (sprint) {
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_SPRINTING));
        }
    }

    private boolean canAttack() {
        if (cpsLimit >= System.currentTimeMillis() ||
                getDistance(target) >= range.get() ||
                mc.player.getCooledAttackStrength(1) <= 0.93F) {
            return false;
        }

        if (mc.player.isInWater() && mc.player.areEyesInFluid(FluidTags.WATER) || mc.player.isInLava() ||
                mc.player.isOnLadder() || mc.player.isPassenger() ||
                mc.player.abilities.isFlying || mc.player.isElytraFlying() ||
                mc.player.isPotionActive(Effects.LEVITATION) || mc.player.isPotionActive(Effects.SLOW_FALLING) ||
                !settings.getValueByName("Только криты").get()) {
            return true;
        }

        return (space.get() && !mc.player.movementInput.jump) || mc.player.fallDistance > 0 && !mc.player.isOnGround();
    }

    private LivingEntity findTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof LivingEntity living && isValid(living) && getDistance(living) <= range.get() +
                    (mode.is("Снап") ? preRange.get() : 0.0f)) {

                if (ignore.getValueByName("Друзья").get() && isFriend(living.getName().getString())) continue;
                if (ignore.getValueByName("Невидимки").get() && living.isInvisible()) continue;
                if (living.getHealth() < 0.01) continue;
                if (ignore.getValueByName("Голые").get() && living instanceof PlayerEntity && living.getTotalArmorValue() == 0) continue;
                if (ignore.getValueByName("Ники").get() && living instanceof PlayerEntity && !living.getUniqueID().equals(PlayerEntity.getOfflineUUID(living.getName().getString())))
                    continue;

                targets.add(living);
            }
        }

        if (targets.isEmpty()) return null;
        if (targets.size() == 1) return targets.get(0);

        return Collections.min(targets, switch (sort.get()) {
            case "По дистанции" -> Comparator.comparingDouble(this::getDistance);
            case "По здоровью" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "По углу" -> Comparator.comparingDouble(this::getDegree);
            default -> null;
        });
    }

    private boolean isValid(LivingEntity entity) {
        if (entity instanceof ArmorStandEntity || entity == mc.player) return false;

        if (entity instanceof PlayerEntity) return targets.getValueByName("Игроки").get();
        if (entity instanceof AnimalEntity || entity instanceof VillagerEntity || entity instanceof WaterMobEntity)
            return targets.getValueByName("Животные").get();
        if (entity instanceof MonsterEntity || entity instanceof SlimeEntity) return targets.getValueByName("Мобы").get();

        return true;
    }

    private void updateClientRotation(float yaw, float pitch) {
        mc.player.rotationYawHead = yaw;
        mc.player.renderYawOffset = yaw;
        mc.player.rotationPitchHead = pitch;
    }

    private void reset() {
        rotYaw = mc.player.rotationYaw;
        rotPitch = mc.player.rotationPitch;
        target = null;
    }

    private void attackEntity(LivingEntity entity) {
        mc.playerController.attackEntity(mc.player, entity);
        mc.player.swingArm(Hand.MAIN_HAND);
    }

    private boolean isInHitBox(LivingEntity me, LivingEntity to) {
        double wHalf = to.getWidth() / 2;
        double yExpand = clamp(me.getPosYEye() - to.getPosY(), 0, to.getHeight());

        double xExpand = clamp(mc.player.getPosX() - to.getPosX(), -wHalf, wHalf);
        double zExpand = clamp(mc.player.getPosZ() - to.getPosZ(), -wHalf, wHalf);

        return new Vector3d(
                to.getPosX() - me.getPosX() + xExpand,
                to.getPosY() - me.getPosYEye() + yExpand,
                to.getPosZ() - me.getPosZ() + zExpand
        ).length() == 0;
    }

    private Vector3d getVector3d(LivingEntity me, LivingEntity to) {
        double wHalf = to.getWidth() / 2;
        double yExpand = clamp(me.getPosYEye() - to.getPosY(), 0, to.getHeight() * (mc.player.getDistance(to) / range.get()));

        double xExpand = clamp(mc.player.getPosX() - to.getPosX(), -wHalf, wHalf);
        double zExpand = clamp(mc.player.getPosZ() - to.getPosZ(), -wHalf, wHalf);

        return new Vector3d(
                to.getPosX() - me.getPosX() + xExpand,
                to.getPosY() - me.getPosYEye() + yExpand,
                to.getPosZ() - me.getPosZ() + zExpand
        );
    }

    private double getDistance(LivingEntity entity) {
        return getVector3d(mc.player, entity).length();
    }

    private double getDegree(LivingEntity entity) {
        Vector3d vec = getVector3d(mc.player, entity);
        double yaw = wrapDegrees(toDegrees(atan2(vec.z, vec.x)) - 90);
        double delta = wrapDegrees(yaw - mc.player.rotationYaw);

        if (abs(delta) > 180) delta -= signum(delta) * 360;
        return abs(delta);
    }

    private float getSensitivity(float value) {
        float gcd = ru.onelove.utils.math.SensUtils.getGCDValue();
        return value - (value % gcd);
    }

    private Entity getMouseOver(Entity target, float yaw, float pitch, float range) {
        // Упрощенная реализация RayTrace
        if (mc.player == null || target == null) return null;

        double distance = mc.player.getDistance(target);
        if (distance > range) return null;

        // Простая проверка - если цель в поле зрения, возвращаем её
        Vector3d playerPos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = target.getBoundingBox().getCenter();
        Vector3d direction = targetPos.subtract(playerPos).normalize();

        // Упрощенная проверка угла
        double deltaYaw = Math.abs(wrapDegrees((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90) - yaw);
        double deltaPitch = Math.abs(wrapDegrees((float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))) - pitch));

        if (deltaYaw < 30 && deltaPitch < 30) {
            return target;
        }

        return null;
    }

    private int getAxeSlot(boolean hotbarOnly) {
        // Простая реализация поиска топора
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem().getTranslationKey().contains("axe")) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFriend(String name) {
        // Заглушка для проверки друзей
        return false;
    }

    public void testaura() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }
}