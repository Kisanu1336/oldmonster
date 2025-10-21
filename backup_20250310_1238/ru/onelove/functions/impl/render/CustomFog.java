package ru.onelove.functions.impl.render;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.ColorSetting;
import ru.onelove.functions.settings.impl.ModeSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.render.ColorUtils;

@FunctionRegister(name = "Custom Fog", type = Category.Render)
public class CustomFog extends Function {
    public SliderSetting power = new SliderSetting("Сила", 20F, 1F,40F, 1F);
    public final ModeSetting mode = new ModeSetting("Мод","Клиент","Клиент","Свой");
    public ColorSetting color = new ColorSetting("Цвет", ColorUtils.rgb(255,255,255)).setVisible(()-> mode.is("Свой"));

    public CustomFog() {
        addSettings(power,mode,color);
    }

    public boolean state;
    public boolean onEnable() {
        super.onEnable();
        //Shaders.setShaderPack(Shaders.SHADER_PACK_NAME_DEFAULT);
        return false;
    }

    public void onEvent(EventUpdate event) {
    }

    public int getDepth() {
        return 6;
    }
}