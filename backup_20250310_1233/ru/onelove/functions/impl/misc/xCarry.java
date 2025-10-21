package ru.onelove.functions.impl.misc;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.EventPacket;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import net.minecraft.network.play.client.CCloseWindowPacket;
import ru.onelove.functions.api.FunctionRegister;

@FunctionRegister(name = "xCarry", type = Category.Misc)
public class xCarry extends Function {

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;

        if (e.getPacket() instanceof CCloseWindowPacket) {
            e.cancel();
        }
    }
}
