package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import ru.onelove.events.EventPacket;
import ru.onelove.events.EventUpdate;
import ru.onelove.events.EventMotion;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.utils.math.StopWatch;
import ru.onelove.utils.movement.MoveUtil;
import ru.onelove.utils.player.InventoryUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@FunctionRegister(name = "CatStrafe", type = Category.Movement)
public class CatStrafe extends Function {
    private final StopWatch timerUtil = new StopWatch();
    private int oldItem = -1;

    private final SliderSetting speedBoost = new SliderSetting("Ускорение", 0.3F, 0.0F, 0.8F, 1.0E-4F);
    private final BooleanSetting autoJump = new BooleanSetting("Авто-прыжок", false);
    private final BooleanSetting safeMode = new BooleanSetting("Безопасный режим", false);

    public CatStrafe() {
        addSettings(speedBoost, safeMode, autoJump);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.collidedHorizontally && resolveBoolean(safeMode, false)) {
            sendClientMessage(TextFormatting.RED + "Вы врезались в стену — CatStrafe выключен!");
            this.toggle();
            return;
        }

        if (!hasItemInInventory(Items.FIREWORK_ROCKET) || mc.player.collidedHorizontally || !hotbarHasItem(Items.ELYTRA)) {
            return;
        }

        if (resolveBoolean(autoJump, false) && !mc.gameSettings.keyBindJump.isKeyDown() && mc.player.isOnGround()) {
            mc.gameSettings.keyBindJump.setPressed(true);
        }

        if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
            mc.playerController.onStoppedUsingItem(mc.player);
        }

        mc.gameSettings.keyBindBack.setPressed(false);
        mc.gameSettings.keyBindLeft.setPressed(false);
        mc.gameSettings.keyBindRight.setPressed(false);

        boolean test = false;
        if ((mc.player.isOnGround() || (mc.player.fallDistance != 0.0F && mc.gameSettings.keyBindJump.isKeyDown()))
                && stopWatchHasTimeElapsed(timerUtil, 600)) {
            test = true;
        }

        if (mc.player.isOnGround() && test) {
            mc.gameSettings.keyBindForward.setPressed(false);
            return;
        }

        if (mc.player.fallDistance == 0.0F && !mc.player.isOnGround()) {
            test = false;
        }

        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.getItem() == Items.ELYTRA && !mc.player.isOnGround() && !mc.player.isInWater() && !mc.player.isInLava()) {
                float speed = 0.9F + resolveSliderFloat(speedBoost, 0.3F);

                if (hotbarHasItem(Items.FIREWORK_ROCKET) && stopWatchHasTimeElapsed(timerUtil, 300)) {
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                    MoveUtil.setMotion(speed);
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                    if (stopWatchHasTimeElapsed(timerUtil, 300)) {
                        try {
                            InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET, false);
                        } catch (Throwable ignored) {}
                        timerUtil.reset();
                    }
                }

                if (!hotbarHasItem(Items.FIREWORK_ROCKET) && stopWatchHasTimeElapsed(timerUtil, 300)) {
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                    MoveUtil.setMotion(speed);
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                    if (stopWatchHasTimeElapsed(timerUtil, 300)) {
                        try {
                            InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET, false);
                        } catch (Throwable ignored) {}
                        timerUtil.reset();
                    }
                }
            }
        }
    }

    @Subscribe
    public void onMotion(EventMotion eventMotion) {
        if (mc.player == null) return;
        eventMotion.setPitch(15.0F);
        mc.player.rotationPitchHead = 15.0F;
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e == null) return;
        try {
            Object packet = e.getPacket();
            if (packet instanceof SEntityMetadataPacket) {
                SEntityMetadataPacket p = (SEntityMetadataPacket) packet;
                if (p.getEntityId() == mc.player.getEntityId()) {
                    safeCancelEvent(e);
                }
            }
        } catch (Throwable ignored) {
            // если getPacket или setCancel отличается — обработается в safeCancelEvent
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (this.oldItem != -1) {
            try {
                if (mc.player.inventory.armorInventory.get(2).getItem() == Items.ELYTRA) {
                    mc.playerController.windowClick(0, this.oldItem < 9 ? this.oldItem + 36 : this.oldItem, 38, ClickType.SWAP, mc.player);
                }
            } catch (Throwable ignored) {}
            this.oldItem = -1;
        }
    }

    /* ==== Утилиты ==== */
    private boolean hotbarHasItem(net.minecraft.item.Item item) {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) return true;
        }
        return false;
    }

    private boolean hasItemInInventory(net.minecraft.item.Item item) {
        if (mc.player == null) return false;
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) return true;
        }
        return false;
    }

    private void safeCancelEvent(Object event) {
        if (event == null) return;
        List<String> methodNames = Arrays.asList("setCancel", "setCanceled", "setCancelled", "cancel");
        for (String name : methodNames) {
            try {
                Method m = event.getClass().getMethod(name, boolean.class);
                m.invoke(event, true);
                return;
            } catch (Throwable ignored) {}
        }
        List<String> fieldNames = Arrays.asList("cancel", "canceled", "cancelled");
        for (String fName : fieldNames) {
            try {
                Field f = event.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                    f.set(event, true);
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }

    private boolean resolveBoolean(BooleanSetting setting, boolean def) {
        try {
            Method m = setting.getClass().getMethod("get");
            Object r = m.invoke(setting);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (Throwable ignored) {}
        return def;
    }

    private float resolveSliderFloat(SliderSetting slider, float def) {
        try {
            Method m = slider.getClass().getMethod("get");
            Object r = m.invoke(slider);
            if (r instanceof Number) return ((Number) r).floatValue();
        } catch (Throwable ignored) {}
        return def;
    }

    private boolean stopWatchHasTimeElapsed(StopWatch sw, long ms) {
        try {
            Method m = sw.getClass().getMethod("hasTimeElapsed", long.class);
            Object r = m.invoke(sw, ms);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (Throwable ignored) {}
        return false;
    }

    private void sendClientMessage(String msg) {
        System.out.println("[CatStrafe] " + msg);
    }
}
