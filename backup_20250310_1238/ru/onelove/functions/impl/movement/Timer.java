package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.SliderSetting;

@FunctionRegister(name = "Timer", type = Category.Movement)
public class Timer extends Function {

    private final SliderSetting speed = new SliderSetting("Скорость", 2f, 0.1f, 10f, 0.1f);

    public Timer() {
        addSettings(speed);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        mc.timer.timerSpeed = speed.get();
    }

    private void reset() {
        mc.timer.timerSpeed = 1;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        reset();
        return false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }
}
