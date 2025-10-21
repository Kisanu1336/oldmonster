package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.functions.impl.combat.KillAura;
import ru.onelove.utils.player.InventoryUtil;
import ru.onelove.events.EventUpdate;

/**
 * TargetPearl — кидает Ender Pearl "за" целью, используя API KillAura (isState() + getTarget()).
 * Конструктор принимает экземпляр KillAura (чтобы брать оттуда current target).
 */
@FunctionRegister(name = "TargetPearl", type = Category.Movement)
public class TargetPearl extends Function {

    private final SliderSetting behindDistance = new SliderSetting("Дистанция за целью", 2.5f, 0.5f, 6.0f, 0.1f);
    private final BooleanSetting autoDisable = new BooleanSetting("Авто-выключение", true);

    private final KillAura killAura;

    /**
     * Конструктор принимает экземпляр KillAura.
     * Если у тебя модульы создаются другим способом, замени этот конструктор на нужный.
     */
    public TargetPearl(KillAura killAura) {
        this.killAura = killAura;
        addSettings(behindDistance, autoDisable);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        // Проверяем API KillAura
        if (killAura == null) return;
        if (!killAura.isState() || killAura.getTarget() == null) return;

        LivingEntity target = killAura.getTarget();
        if (target == null || !target.isAlive()) return;

        // Ищем жемчуг
        int pearlSlot = InventoryUtil.getItemSlot(Items.ENDER_PEARL);
        if (pearlSlot == -1) return; // нет жемчуга

        int oldSlot = mc.player.inventory.currentItem;
        boolean swappedFromInv = false;

        try {
            // Если жемчуг в инвентаре (слот >= 9), свапаем в текущий хотбар-слот
            if (pearlSlot >= 9) {
                InventoryUtil.inventorySwapClick(Items.ENDER_PEARL, false);
                swappedFromInv = true;
            } else {
                // если в хотбаре — переключаемся на него
                mc.player.inventory.currentItem = pearlSlot;
                try {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(pearlSlot));
                } catch (Throwable ignored) {}
            }

            // Считаем позицию "за" целью (в плоскости XZ)
            Vector3d targetEye = target.getEyePosition(1.0F);
            Vector3d look = target.getLookVec(); // направление взгляда цели
            double behind = behindDistance.get();
            Vector3d behindPos = targetEye.subtract(look.x * behind, 0.0, look.z * behind);

            // Вектор от глаз игрока до точки за целью
            Vector3d from = mc.player.getEyePosition(1.0F);
            Vector3d vec = behindPos.subtract(from);

            // Вычисляем yaw/pitch
            float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
            yaw = MathHelper.clamp(yaw, -180F, 180F);
            pitch = MathHelper.clamp(pitch, -90F, 90F);

            // Локально устанавливаем поворот игрока для корректного броска
            mc.player.rotationYaw = yaw;
            mc.player.rotationPitch = pitch;
            mc.player.rotationYawHead = yaw;
            mc.player.renderYawOffset = yaw;

            // Бросаем жемчуг
            mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
            mc.player.swingArm(Hand.MAIN_HAND);

        } finally {
            // Восстанавливаем предмет/слот
            if (swappedFromInv) {
                // вернуть предмет обратно в инвентарь
                try {
                    InventoryUtil.inventorySwapClick(Items.ENDER_PEARL, false);
                } catch (Throwable ignored) {}
            } else {
                mc.player.inventory.currentItem = oldSlot;
                try {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(oldSlot));
                } catch (Throwable ignored) {}
            }

            if (autoDisable.get()) {
                this.toggle();
            }
        }
    }
}
