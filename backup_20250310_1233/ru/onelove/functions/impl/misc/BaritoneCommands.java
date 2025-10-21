package ru.onelove.functions.impl.misc;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.EventSendMessage;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;

/**
 * BaritoneCommands — позволяет использовать команды Baritone через # в чате.
 */
@FunctionRegister(name = "BaritoneCommands", type = Category.Misc)
public class BaritoneCommands extends Function {

    public BaritoneCommands() {
        super();
    }

    @Subscribe
    public void onChat(EventSendMessage e) {
        String msg = e.getMessage();

        // Если сообщение начинается с "#", не отменяем его
        if (msg.startsWith("#")) {
            e.setCancelled(false);
        }
    }
}
