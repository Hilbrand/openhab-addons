package org.openhab.binding.genericbinding.handler;

import org.eclipse.smarthome.core.library.types.HSBType;

public interface Hs100HandleCommands {
    void handleColorOnOff(boolean on);

    void handleColorDimmer(int percentage);

    void handleColor(HSBType hsbType);

    void handleSwitch(boolean on);

    void handleLed(boolean onOff);
}
