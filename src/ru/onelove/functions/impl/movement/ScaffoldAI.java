package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import ru.onelove.events.EventInput;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.math.StopWatch;

import java.util.Random;

@FunctionRegister(name = "ScaffoldAI", type = Category.Movement)
public class ScaffoldAI extends Function {

    private final SliderSetting delay = new SliderSetting("Задержка (мс)", 120f, 50f, 400f, 1f);
    private final SliderSetting jitter = new SliderSetting("Джиттер (мс)", 30f, 0f, 150f, 1f);
    private final SliderSetting minSpeed = new SliderSetting("Мин.скорость", 0.0f, 0.0f, 1.0f, 0.01f);

    private final BooleanSetting silentSwitch = new BooleanSetting("Silent switch", true);
    private final BooleanSetting sneakWhenPlace = new BooleanSetting("Sneak при постановке", true);
    private final BooleanSetting onlyWhenMoving = new BooleanSetting("Только при движении", true);

    private final StopWatch timer = new StopWatch();
    private final Random random = new Random();

    @Getter
    private int previousSlot = -1;

    public ScaffoldAI() {
        addSettings(delay, jitter, minSpeed, silentSwitch, sneakWhenPlace, onlyWhenMoving);
    }

    @Subscribe
    public void onInput(EventInput eventInput) {
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (onlyWhenMoving.get() && getHorizontalSpeed() < minSpeed.get().doubleValue()) {
            return;
        }

        BlockPos below = new BlockPos(mc.player.getPosX(),
                mc.player.getPosY() - 1.0, mc.player.getPosZ());
        BlockState stateBelow = mc.world.getBlockState(below);
        if (!isReplaceable(stateBelow)) return;

        // --- вот эта строка исправлена ---
        long ms = delay.get().longValue() +
                (jitter.get().doubleValue() > 0 ? random.nextInt(jitter.get().intValue()) : 0);

        if (!timer.isReached(ms)) return;

        int blockSlot = findBlockSlotInHotbar();
        if (blockSlot == -1) return;

        previousSlot = mc.player.inventory.currentItem;
        if (silentSwitch.get()) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(blockSlot));
            mc.player.inventory.currentItem = blockSlot;
        } else {
            mc.player.inventory.currentItem = blockSlot;
        }

        if (sneakWhenPlace.get()) sendSneak(true);

        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
        mc.player.swingArm(Hand.MAIN_HAND);

        if (sneakWhenPlace.get()) sendSneak(false);

        if (silentSwitch.get()) {
            mc.player.inventory.currentItem = previousSlot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(previousSlot));
        }

        timer.reset();
    }

    private void sendSneak(boolean state) {
        try {
            if (state) {
                if (hasAction("START_SNEAKING"))
                    mc.player.connection.sendPacket(
                            new CEntityActionPacket(mc.player,
                                    CEntityActionPacket.Action.valueOf("START_SNEAKING")));
                else
                    mc.player.connection.sendPacket(
                            new CEntityActionPacket(mc.player,
                                    CEntityActionPacket.Action.valueOf("PRESS_SHIFT_KEY")));
            } else {
                if (hasAction("STOP_SNEAKING"))
                    mc.player.connection.sendPacket(
                            new CEntityActionPacket(mc.player,
                                    CEntityActionPacket.Action.valueOf("STOP_SNEAKING")));
                else
                    mc.player.connection.sendPacket(
                            new CEntityActionPacket(mc.player,
                                    CEntityActionPacket.Action.valueOf("RELEASE_SHIFT_KEY")));
            }
        } catch (Exception ignored) {
        }
    }

    private boolean hasAction(String name) {
        for (CEntityActionPacket.Action a : CEntityActionPacket.Action.values()) {
            if (a.name().equals(name)) return true;
        }
        return false;
    }

    private boolean isReplaceable(BlockState state) {
        if (state == null) return true;
        Block b = state.getBlock();
        return b == Blocks.AIR || b == Blocks.CAVE_AIR || b == Blocks.VOID_AIR || b == Blocks.WATER;
    }

    private int findBlockSlotInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private double getHorizontalSpeed() {
        Vector3d vel = mc.player.getMotion();
        return Math.sqrt(vel.x * vel.x + vel.z * vel.z);
    }

    @Override
    public boolean onEnable() {
        previousSlot = -1;
        timer.reset();
        return super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (previousSlot != -1 && mc.player != null) {
            mc.player.inventory.currentItem = previousSlot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(previousSlot));
            previousSlot = -1;
        }
    }
}
