package ru.onelove.functions.impl.misc;

import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BindSetting;

@FunctionRegister(name = "AutoBuyUI", type = Category.Misc)
public class AutoBuyUI extends Function {

    public BindSetting setting = new BindSetting("Кнопка открытия", -1);

    public AutoBuyUI() {
        addSettings(setting);
    }
}
