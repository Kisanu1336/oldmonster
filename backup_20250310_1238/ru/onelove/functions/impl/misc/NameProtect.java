package ru.onelove.functions.impl.misc;

import com.google.common.eventbus.Subscribe;
import ru.onelove.onelove;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.StringSetting;
import net.minecraft.client.Minecraft;

@FunctionRegister(name = "NameProtect", type = Category.Misc)
public class NameProtect extends Function {

    public static String fakeName = "";

    public StringSetting name = new StringSetting(
            "Заменяемое Имя",
            "dedinside",
            "Укажите текст для замены вашего игрового ника"
    );

    public NameProtect() {
        addSettings(name);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        fakeName = name.get();
    }

    public static String getReplaced(String input) {
        if (onelove.getInstance() != null && onelove.getInstance().getFunctionRegistry().getNameProtect().isState()) {
            input = input.replace(Minecraft.getInstance().session.getUsername(), fakeName);
        }
        return input;
    }
}
