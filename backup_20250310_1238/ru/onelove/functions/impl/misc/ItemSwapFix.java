package ru.onelove.functions.impl.misc;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.EventPacket;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;

@FunctionRegister(name = "ItemSwapFix", type = Category.Misc)
public class ItemSwapFix extends Function {

    @Subscribe
    private void onPacket(EventPacket e) {
        if (mc.player == null) return;
        if (e.getPacket() instanceof SHeldItemChangePacket wrapper) {
            final int serverSlot = wrapper.getHeldItemHotbarIndex();
            if (serverSlot != mc.player.inventory.currentItem) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(Math.max(mc.player.inventory.currentItem - 1, 0)));
                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                e.cancel();
            }
        }
    }
}
