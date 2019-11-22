package org.openhab.binding.genericbinding.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;

@NonNullByDefault
public class Hs100HandlerImpl extends BaseThingHandler {

    private final Hs100Handler<DeviceData> handler;

    @Nullable
    private ScheduledFuture<?> scheduledRefresh;

    public Hs100HandlerImpl(Thing thing, Hs100Handler<D> handler) {
        super(thing);
        this.handler = handler;
    }

    @Override
    public void initialize() {
        scheduledRefresh = handler.initialize(getConfigAs(SwitchConfiguration.class), this::refreshAll);
    }

    @Override
    public void dispose() {
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(true);
            scheduledRefresh = null;
        }
        handler.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (command instanceof RefreshType) {

            } else {
                boolean handled = false;

                switch (channelUID.getId()) {
                    case "":
                        handleCommandType(handler::handleSwitch, command, OnOffType.class);
                        break;
                    case "color":
                        handled = handleCommandType(handler::handleSwitchColor, command, HSBType.class)
                                || handleCommandType(handler::handleSwitch, command, OnOffType.class);
                }
                if (handled) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    // log.warn("");
                }
            }
        } catch (ThingStatusException e) {
            updateStatus(ThingStatus.OFFLINE, e.getThingStatusDetail(), e.getMessage());
        }
    }

    private <C extends Command> boolean handleCommandType(Consumer<C> consumer, Command command, Class<C> clazz) {
        if (clazz.equals(command.getClass())) {
            consumer.accept((C) command);
            return true;
        } else {
            return false;
        }
    }

    public void startRefresh() {
        scheduler.scheduleWithFixedDelay(this::refreshAll, 0, 10, TimeUnit.SECONDS);
    }

    public void refreshAll() {
        try {
            D data = handler.getStateData();
            updateState("switchChannel", handler.getSwitchState(data));
            updateStatus(ThingStatus.ONLINE);
        } catch (ThingStatusException e) {
            updateStatus(ThingStatus.OFFLINE, e.getThingStatusDetail(), e.getMessage());
        }
    }

    public void refreshAll2() {
        try {
            handler.updateSwitchState()
            D data = handler.getStateData();
            updateState("switchChannel", handler.getSwitchState(data));
            updateStatus(ThingStatus.ONLINE);
        } catch (ThingStatusException e) {
            updateStatus(ThingStatus.OFFLINE, e.getThingStatusDetail(), e.getMessage());
        }
    }

    void updateSwitchState(OnOffType onOff) {
        updateState("switchChannel", onOff);
    }

}
