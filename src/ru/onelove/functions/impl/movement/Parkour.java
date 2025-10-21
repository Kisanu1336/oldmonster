package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.utils.player.MoveUtils;

@FunctionRegister(name = "Parkour", type = Category.Movement)
public class Parkour extends Function {

    @Subscribe
    private void onUpdate(EventUpdate e) {

        if (MoveUtils.isBlockUnder(0.001f) && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

}
