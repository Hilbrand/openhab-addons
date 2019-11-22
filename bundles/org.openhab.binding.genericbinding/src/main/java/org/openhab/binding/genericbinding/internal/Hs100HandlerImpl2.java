package org.openhab.binding.genericbinding.internal;

import java.util.concurrent.ScheduledFuture;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.binding.genericbinding.handler.Hs100Handler;
import org.openhab.binding.genericbinding.handler.SwitchConfiguration;

public class Hs100HandlerImpl2 implements Hs100Handler<D> {

    @Override
    public void handleSwitch(OnOffType onOff) {
    }

    @Override
    public void handleSwitchColor(HSBType onOff) {
    }

    @Override
    public void handleLed(OnOffType onOff) {
    }

    @Override
    public D getStateData() {
        return null;
    }

    @Override
    public OnOffType getSwitchState(D data) {
        return null;
    }

    @Override
    public OnOffType getLedState(D data) {
        return null;
    }

    @Override
    public StringType getRssiState(D data) {
        return null;
    }

    @Override
    public ScheduledFuture<?> initialize(SwitchConfiguration configuration, Runnable refreshMethod) {
        return null;
    }

    @Override
    public void dispose() {
        // NO-OP
    }

}
