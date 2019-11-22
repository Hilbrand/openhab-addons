package org.openhab.binding.genericbinding.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class Hs100Handler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(Hs100Handler.class);

    protected @NonNullByDefault({}) SwitchConfiguration configuration;

    private @NonNullByDefault({}) ScheduledFuture<?> scheduledRefresh;

    public Hs100Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(SwitchConfiguration.class);
        if (initialize(configuration)) {
            scheduledRefresh = scheduler.scheduleWithFixedDelay(this::safeRefresh, configuration.refresh,
                    configuration.refresh, TimeUnit.SECONDS);
        }
    }

    protected abstract boolean initialize(SwitchConfiguration configuration);

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        switch (channelUID.getId()) {
            case "switch":
                handleSwitch(command == OnOffType.ON);
                break;
            case "color":
                if (command instanceof OnOffType) {
                    handleColorOnOff(command == OnOffType.ON);
                } else if (command instanceof PercentType) {
                    handleColorDimmer(((PercentType) command).intValue());
                } else if (command instanceof HSBType) {
                    handleColor(((HSBType) command));
                }
                break;
            case "led":
                handleLed((OnOffType) command);
                break;
        }
    }

    protected abstract void handleColorOnOff(boolean on);

    protected abstract void handleColorDimmer(int percentage);

    protected abstract void handleColor(HSBType hsbType);

    private void safeRefresh() {
        try {
            refresh();
        } catch (RuntimeException e) {
            logger.debug("Refresh crashed with exception: ", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
        }
    }

    protected abstract void refresh();

    protected abstract void handleSwitch(boolean on);

    protected abstract void handleLed(OnOffType onOff);

    protected void setColorDimmer(int percentage) {
        safeUpdateState("color", new PercentType(percentage));
    }

    protected void setSwitchState(boolean state) {
        safeUpdateState("switch", OnOffType.from(state));
    }

    private void safeUpdateState(String channelId, State state) {
        Channel channel = getThing().getChannel(channelId);

        if (channel != null) {
            updateState(channel.getUID(), state);
        }
    }

    @Override
    public void dispose() {
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(true);
            scheduledRefresh = null;
        }
        super.dispose();
    }
}
