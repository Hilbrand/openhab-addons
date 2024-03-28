/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.thingstatus.internal;

import static org.openhab.binding.thingstatus.internal.ThingStatusBindingConstants.CHANNEL_STATUS;
import static org.openhab.binding.thingstatus.internal.ThingStatusBindingConstants.CHANNEL_STATUS_DESCRIPTION;
import static org.openhab.binding.thingstatus.internal.ThingStatusBindingConstants.CHANNEL_STATUS_DETAIL;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.events.ThingStatusInfoEvent;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ThingStatusHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class ThingStatusHandler extends BaseThingHandler implements EventSubscriber {

    private final Logger logger = LoggerFactory.getLogger(ThingStatusHandler.class);

    private final ThingRegistry thingRegistry;

    private @Nullable ThingUID thingUID;

    public ThingStatusHandler(final Thing thing, final ThingRegistry thingRegistry) {
        super(thing);
        this.thingRegistry = thingRegistry;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // Nothing to handle
    }

    @Override
    public void initialize() {
        final ThingStatusConfiguration config = getConfigAs(ThingStatusConfiguration.class);
        thingUID = new ThingUID(config.thingUid);
        updateChannels(getThingStatusInfo(thingUID));
        updateStatus(ThingStatus.ONLINE);
    }

    private @Nullable ThingStatusInfo getThingStatusInfo(final @Nullable ThingUID thingUid) {
        final Thing thing = thingUid == null ? null : thingRegistry.get(thingUid);

        return thing == null ? null : thing.getStatusInfo();
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ThingStatusInfoEvent.TYPE);
    }

    @Override
    public void receive(final Event event) {
        if (event instanceof ThingStatusInfoEvent) {
            final ThingStatusInfoEvent e = ((ThingStatusInfoEvent) event);

            if (e.getThingUID().equals(thingUID)) {
                updateChannels(e.getStatusInfo());
            }
        }
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    private void updateChannels(final @Nullable ThingStatusInfo thingStatusInfo) {
        updateState(CHANNEL_STATUS, getState(thingStatusInfo, tsi -> StringType.valueOf(tsi.getStatus().name())));
        updateState(CHANNEL_STATUS_DETAIL,
                getState(thingStatusInfo, tsi -> StringType.valueOf(tsi.getStatusDetail().name())));
        updateState(CHANNEL_STATUS_DESCRIPTION,
                getState(thingStatusInfo, tsi -> StringType.valueOf(tsi.getDescription())));
    }

    private State getState(final @Nullable ThingStatusInfo thingStatusInfo,
            final Function<ThingStatusInfo, State> function) {
        return thingStatusInfo == null ? UnDefType.UNDEF : function.apply(thingStatusInfo);
    }
}
