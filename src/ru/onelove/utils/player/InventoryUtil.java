package ru.onelove.utils.player;

import ru.onelove.events.EventPacket;
import ru.onelove.utils.client.IMinecraft;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.play.server.SHeldItemChangePacket;

/**
 * Объединённый InventoryUtil:
 * - сохраняет старые методы, которые были в твоём классе
 * - добавляет новые статические методы, которые использовали CatFly/CatStrafe
 */
public class InventoryUtil implements IMinecraft {

    @Getter
    private static final InventoryUtil instance = new InventoryUtil();

    // ========== СТАРЫЕ МЕТОДЫ ========== //

    public static int findEmptySlot(boolean inHotBar) {
        if (mc.player == null) return -1;
        int start = inHotBar ? 0 : 9;
        int end = inHotBar ? 9 : 45;

        for (int i = start; i < end; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static void moveItem(int from, int to, boolean air) {
        if (mc.player == null) return;
        if (from == to) return;
        pickupItem(from, 0);
        pickupItem(to, 0);
        if (air) pickupItem(from, 0);
    }

    public static void moveItemTime(int from, int to, boolean air, int time) {
        if (mc.player == null) return;
        if (from == to) return;
        pickupItem(from, 0, time);
        pickupItem(to, 0, time);
        if (air) pickupItem(from, 0, time);
    }

    public static void moveItem(int from, int to) {
        if (mc.player == null) return;
        if (from == to) return;
        pickupItem(from, 0);
        pickupItem(to, 0);
        pickupItem(from, 0);
    }

    public static void pickupItem(int slot, int button) {
        if (mc.player == null || mc.playerController == null) return;
        mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, mc.player);
    }

    public static void pickupItem(int slot, int button, int time) {
        if (mc.player == null || mc.playerController == null) return;
        try {
            mc.playerController.windowClickFixed(0, slot, button, ClickType.PICKUP, mc.player, time);
        } catch (Throwable ignored) {
            mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, mc.player);
        }
    }

    public static int getAxeInInventory(boolean inHotBar) {
        if (mc.player == null) return -1;
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;

        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s != null && s.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public static int findBestSlotInHotBar() {
        if (mc.player == null) return -1;
        int emptySlot = findEmptyHotbarSlot();
        if (emptySlot != -1) return emptySlot;
        return findNonSwordSlot();
    }

    private static int findEmptyHotbarSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if ((s == null || s.isEmpty()) && mc.player.inventory.currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    private static int findNonSwordSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s == null) continue;
            if (!(s.getItem() instanceof SwordItem) && !(s.getItem() instanceof ElytraItem) && mc.player.inventory.currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    public static int getSlotInInventory(Item item) {
        if (mc.player == null) return -1;
        int finalSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == item) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    public static int getSlotInInventoryOrHotbar(Item item, boolean inHotBar) {
        if (mc.player == null) return -1;
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        int finalSlot = -1;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == item) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    public static int getSlotInInventoryOrHotbar() {
        if (mc.player == null) return -1;
        int firstSlot = 0;
        int lastSlot = 9;
        int finalSlot = -1;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s == null) continue;
            if (Block.getBlockFromItem(s.getItem()) instanceof SlabBlock) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    // ========== НОВЫЕ УТИЛИТЫ ========== //

    public static int getItemSlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == item) return i;
        }
        return -1;
    }

    public static int findItem(Item item) {
        return getItemSlot(item);
    }

    public static boolean doesHotbarHaveItem(Item item) {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == item) return true;
        }
        return false;
    }

    public static void inventorySwapClick(Item item, boolean armor) {
        if (mc.player == null || mc.playerController == null) return;
        int slot = getItemSlot(item);
        if (slot == -1) return;

        int windowId = 0;
        try {
            if (mc.player.openContainer != null) windowId = mc.player.openContainer.windowId;
        } catch (Throwable ignored) {}

        int slotInWindow = (slot < 9) ? slot + 36 : slot;
        int target = armor ? 38 : mc.player.inventory.currentItem + 36;

        try {
            mc.playerController.windowClick(windowId, slotInWindow, target, ClickType.SWAP, mc.player);
        } catch (Throwable ignored) {
            try {
                mc.playerController.windowClick(0, slotInWindow, target, ClickType.SWAP, mc.player);
            } catch (Throwable ignored2) {}
        }
    }

    public static void switchToHotbarSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.inventory.currentItem = slot;
        try {
            mc.player.connection.sendPacket(new net.minecraft.network.play.client.CHeldItemChangePacket(slot));
        } catch (Throwable ignored) {}
    }

    // ========== Вложенный Hand ========== //
    public static class Hand {
        public static boolean isEnabled;
        private boolean isChangingItem;
        private int originalSlot = -1;

        public void onEventPacket(EventPacket eventPacket) {
            if (eventPacket == null) return;
            if (!eventPacket.isReceive()) return;
            try {
                if (eventPacket.getPacket() instanceof SHeldItemChangePacket) {
                    this.isChangingItem = true;
                }
            } catch (Throwable ignored) {}
        }

        public void handleItemChange(boolean resetItem) {
            if (this.isChangingItem && this.originalSlot != -1) {
                isEnabled = true;
                mc.player.inventory.currentItem = this.originalSlot;
                if (resetItem) {
                    this.isChangingItem = false;
                    this.originalSlot = -1;
                    isEnabled = false;
                }
            }
        }

        public void setOriginalSlot(int slot) {
            this.originalSlot = slot;
        }
    }
}
