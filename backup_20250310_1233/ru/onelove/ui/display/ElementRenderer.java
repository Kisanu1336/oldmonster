package ru.onelove.ui.display;

import ru.onelove.events.EventDisplay;
import ru.onelove.utils.client.IMinecraft;

public interface ElementRenderer extends IMinecraft {
    void render(EventDisplay eventDisplay);
}
