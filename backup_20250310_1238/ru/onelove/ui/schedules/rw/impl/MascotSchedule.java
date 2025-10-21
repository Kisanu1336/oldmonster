package ru.onelove.ui.schedules.rw.impl;

import ru.onelove.ui.schedules.rw.Schedule;
import ru.onelove.ui.schedules.rw.TimeType;

public class MascotSchedule
        extends Schedule {
    @Override
    public String getName() {
        return "Талисман";
    }

    @Override
    public TimeType[] getTimes() {
        return new TimeType[]{TimeType.NINETEEN_HALF};
    }
}
