package ru.onelove.ui.schedules.rw.impl;

import ru.onelove.ui.schedules.rw.Schedule;
import ru.onelove.ui.schedules.rw.TimeType;

public class ScroogeSchedule
        extends Schedule {
    @Override
    public String getName() {
        return "Скрудж";
    }

    @Override
    public TimeType[] getTimes() {
        return new TimeType[]{TimeType.FIFTEEN_HALF};
    }
}
