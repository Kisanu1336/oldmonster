package ru.onelove.ui.display;

import ru.onelove.events.EventUpdate;
import ru.onelove.utils.client.IMinecraft;

public interface ElementUpdater extends IMinecraft {

    void update(EventUpdate e);
}
