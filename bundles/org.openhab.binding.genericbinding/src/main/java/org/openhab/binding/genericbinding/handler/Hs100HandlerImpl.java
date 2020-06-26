package org.openhab.binding.genericbinding.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.thing.ChannelUID;

public class Hs100HandlerImpl extends Hs100Handler {

    @Override
    public void handleColorOnOff(final boolean on) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleColorDimmer(final int percentage) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleColor(final HSBType hsbType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleSwitch(final boolean on) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleLed(final boolean onOff) {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean initialize(@NonNull final SwitchConfiguration configuration) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Hs100Data refreshData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void handleRefresh(@NonNull final ChannelUID channelUID) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void refresh() {
        // TODO Auto-generated method stub

    }

}
