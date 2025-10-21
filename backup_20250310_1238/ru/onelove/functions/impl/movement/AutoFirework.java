package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.math.StopWatch;

@FunctionRegister(name = "AutoFirework", type = Category.Movement)
public class AutoFirework extends Function {

    private final SliderSetting delay = new SliderSetting("Задержка (мс)", 1000, 500, 5000, 100);
    private final StopWatch timer = new StopWatch();

    public AutoFirework() {
        addSettings(delay);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isElytraFlying() && timer.isReached(Math.round(delay.get()))) {
            launchFirework();
            timer.reset();
        }
    }

    private void launchFirework() {
        int fireworkSlot = findFireworkInHotbar();
        if (fireworkSlot != -1) {
            int oldSlot = mc.player.inventory.currentItem;

            // переключаемся на слот с фейерверком
            mc.player.inventory.currentItem = fireworkSlot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(fireworkSlot));

            // используем фейерверк
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));

            // возвращаем старый слот
            mc.player.inventory.currentItem = oldSlot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(oldSlot));

            System.out.println("[AutoFirework] Запущен фейерверк со слота " + fireworkSlot);
        }
    }

    private int findFireworkInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
}
