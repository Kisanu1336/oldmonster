package ru.onelove.functions.impl.render;

import com.google.common.eventbus.Subscribe;

import ru.onelove.events.EventPacket;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.ModeSetting;
import lombok.Getter;
import net.minecraft.network.play.server.SUpdateTimePacket;

@Getter
@FunctionRegister(name = "World", type = Category.Render)
public class World extends Function {

    public ModeSetting time = new ModeSetting("Time", "Day", "Day", "Night");

    public World() {
        addSettings(time);
    }
    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof SUpdateTimePacket p) {
            if (time.get().equalsIgnoreCase("Day"))
                p.worldTime = 1000L;
            else
                p.worldTime = 13000L;
        }
    }
}
