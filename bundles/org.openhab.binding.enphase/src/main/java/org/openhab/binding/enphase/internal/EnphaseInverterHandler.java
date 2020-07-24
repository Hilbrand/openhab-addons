/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.enphase.internal;

import static org.openhab.binding.enphase.internal.EnphaseBindingConstants.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.util.ThingHandlerHelper;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.enphase.internal.dto.InverterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EnphaseInverterHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
class EnphaseInverterHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(EnphaseInverterHandler.class);
    private @Nullable InverterDTO lastKnownState;
    private String serialNumber = "";

    public EnphaseInverterHandler(final Thing thing) {
        super(thing);
    }

    /**
     * @return the serialNumber
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (command instanceof RefreshType) {
            final String channelId = channelUID.getId();

            if (INVERTER_CHANNEL_LAST_REPORT_WATTS.equals(channelId)) {
                refreshLastReportWatts(lastKnownState);
            } else if (INVERTER_CHANNEL_MAX_REPORT_WATTS.equals(channelId)) {
                refreshMaxReportWatts(lastKnownState);
            } else if (INVERTER_CHANNEL_LAST_REPORT_DATE.equals(channelId)) {
                refreshLastReportDate(lastKnownState);
            }
        }
    }

    public void refreshState(final @Nullable InverterDTO inverterData) {
        refreshLastReportWatts(inverterData);
        refreshMaxReportWatts(inverterData);
        refreshLastReportDate(inverterData);
        lastKnownState = inverterData;
        if (ThingHandlerHelper.isHandlerInitialized(getThing()) && getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    private void refreshLastReportWatts(final @Nullable InverterDTO inverterData) {
        updateState(INVERTER_CHANNEL_LAST_REPORT_WATTS, inverterData == null ? UnDefType.UNDEF
                : new QuantityType<>(inverterData.lastReportWatts, SmartHomeUnits.WATT));
    }

    private void refreshMaxReportWatts(final @Nullable InverterDTO inverterData) {
        updateState(INVERTER_CHANNEL_MAX_REPORT_WATTS, inverterData == null ? UnDefType.UNDEF
                : new QuantityType<>(inverterData.maxReportWatts, SmartHomeUnits.WATT));
    }

    private void refreshLastReportDate(final @Nullable InverterDTO inverterData) {
        final State state;

        if (inverterData == null) {
            state = UnDefType.UNDEF;
        } else {
            final Instant instant = Instant.ofEpochSecond(inverterData.lastReportDate);
            final ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
            logger.trace("[{}] Epoch time {}, zonedDateTime: {}", getThing().getUID(), inverterData.lastReportDate,
                    zonedDateTime);
            state = new DateTimeType(zonedDateTime);
        }
        updateState(INVERTER_CHANNEL_LAST_REPORT_DATE, state);
    }

    @Override
    public void initialize() {
        serialNumber = (String) getConfig().get(EnphaseBindingConstants.CONFIG_SERIAL_NUMBER);
        updateStatus(ThingStatus.UNKNOWN);
    }
}
