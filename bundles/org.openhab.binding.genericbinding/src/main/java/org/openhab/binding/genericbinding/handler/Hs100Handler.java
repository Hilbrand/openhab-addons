package org.openhab.binding.genericbinding.handler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.cache.ExpiringCache;
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
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class Hs100Handler extends BaseThingHandler implements Hs100HandleCommands {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected @NonNullByDefault({}) SwitchConfiguration configuration;

    private @NonNullByDefault({}) ScheduledFuture<?> scheduledRefresh;

    private final ExpiringCache<Hs100Data> data = new ExpiringCache<Hs100Data>(Duration.ofSeconds(3),
            this::refreshData);

    public Hs100Handler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(SwitchConfiguration.class);
        scheduler.execute(() -> {
            if (initialize(configuration)) {
                scheduledRefresh = scheduler.scheduleWithFixedDelay(this::safeRefresh, configuration.refresh,
                        configuration.refresh, TimeUnit.SECONDS);
            }
        });
    }

    protected abstract boolean initialize(SwitchConfiguration configuration);

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (command instanceof RefreshType) {
            handleRefresh(channelUID);
        }
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
                if (command instanceof OnOffType) {
                    handleLed(command == OnOffType.ON);
                }
                break;
        }
    }

    protected abstract @Nullable Hs100Data refreshData();

    protected abstract void handleRefresh(ChannelUID channelUID);

    private void x(final ChannelUID channelUID) {
        final Hs100Data value = data.getValue();
        final State state;

        if (value == null) {
            state = UnDefType.UNDEF;
        } else {
            switch (channelUID.getId()) {
                case "switch":
                    state = OnOffType.from(value.getSwitchState());
                    break;
                default:
                    state = UnDefType.UNDEF;
                    break;
            }
        }
        updateState(channelUID, state);
    }

    private void safeRefresh() {
        try {
            final Hs100Data data = refreshData();
            if (data == null) {

            } else {
                for (final Channel channel : getThing().getChannels()) {
                    if (isLinked(channel.getUID())) {
                        State state = UnDefType.UNDEF;
                        switch (channel.getUID().getId()) {
                            case "switch":
                                state = OnOffType.from(data.getSwitchState());
                                break;
                        }
                        updateState(channel.getUID(), state);
                    }
                }
            }
        } catch (final RuntimeException e) {
            logger.debug("Refresh crashed with exception: ", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
        }
    }

    protected abstract void refresh();

    protected void setColorDimmer(final int percentage) {
        safeUpdateState("color", new PercentType(percentage));
    }

    protected void setSwitchState(final boolean state) {
        safeUpdateState("switch", OnOffType.from(state));
    }

    private void safeUpdateState(final String channelId, final State state) {
        final Channel channel = getThing().getChannel(channelId);

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
