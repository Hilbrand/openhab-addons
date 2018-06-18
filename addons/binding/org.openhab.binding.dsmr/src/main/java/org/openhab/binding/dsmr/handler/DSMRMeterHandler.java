/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.handler;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.cosem.CosemValue;
import org.openhab.binding.dsmr.internal.device.cosem.CosemValueDescriptor;
import org.openhab.binding.dsmr.internal.meter.DSMRMeter;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterConfiguration;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterDescriptor;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterListener;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MeterHandler will create logic DSMR meter ThingTypes
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Separated thing state update cycle from meter values received cycle
 */
@NonNullByDefault
public class DSMRMeterHandler extends BaseThingHandler implements DSMRMeterListener {

    private final Logger logger = LoggerFactory.getLogger(DSMRMeterHandler.class);

    /**
     * The DSMRMeter instance
     */
    @Nullable
    private DSMRMeter meter;

    /**
     * Last received cosem objects.
     */
    private List<CosemObject> lastReceivedValues = Collections.emptyList();

    /**
     * Reference to the meter watchdog
     */
    @Nullable
    private ScheduledFuture<?> meterWatchdog;

    /**
     * Creates a new MeterHandler for the given Thing
     *
     * @param thing {@link Thing} to create the MeterHandler for
     */
    public DSMRMeterHandler(Thing thing) {
        super(thing);
    }

    /**
     * DSMR Meter don't support handling commands
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // No comments can be handled
    }

    /**
     * Initializes a DSMR Meter
     *
     * This method will load the corresponding configuration
     */
    @Override
    public void initialize() {
        logger.debug("Initialize MeterHandler for Thing {}", getThing().getUID());
        DSMRMeterType meterType;

        try {
            meterType = DSMRMeterType.valueOf(getThing().getThingTypeUID().getId().toUpperCase());
        } catch (IllegalArgumentException iae) {
            logger.warn(
                    "{} could not be initialized due to an invalid meterType {}. Delete this Thing if the problem persists.",
                    getThing(), getThing().getThingTypeUID().getId().toUpperCase());
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This could not be initialized. Delete Thing if the problem persists.");
            return;
        }
        DSMRMeterConfiguration meterConfig = getConfigAs(DSMRMeterConfiguration.class);
        DSMRMeterDescriptor meterDescriptor = new DSMRMeterDescriptor(meterType, meterConfig.channel);
        meter = new DSMRMeter(meterDescriptor);

        meterWatchdog = scheduler.scheduleWithFixedDelay(this::updateState, meterConfig.refresh, meterConfig.refresh,
                TimeUnit.SECONDS);
        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void dispose() {
        if (meterWatchdog != null) {
            meterWatchdog.cancel(false);
            meterWatchdog = null;
        }
    }

    /**
     * Updates the state of all channels from the last received Cosem values from the meter.
     */
    private synchronized void updateState() {
        logger.trace("Update state for device: {}", getThing().getThingTypeUID().getId());
        if (lastReceivedValues.isEmpty()) {
            if (getThing().getStatus() != ThingStatus.OFFLINE) {
                setDeviceOffline(ThingStatusDetail.COMMUNICATION_ERROR, "No data received from meter.");
            }
        } else {
            for (CosemObject cosemObject : lastReceivedValues) {
                String channel = cosemObject.getType().name().toLowerCase();

                for (Entry<String, ? extends CosemValue<?>> entry : cosemObject.getCosemValues().entrySet()) {
                    if (!CosemValueDescriptor.DEFAULT_CHANNEL.equals(entry.getKey())) {
                        /* CosemObject has a specific sub channel */
                        channel += "_" + entry.getKey();
                    }
                    State newState = entry.getValue().getStateValue();
                    logger.debug("Updating state for channel {} to value {}", channel, newState);
                    updateState(channel, newState);
                }
            }
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }
            lastReceivedValues = Collections.emptyList();
        }
    }

    /**
     * Callback for received meter values
     *
     * In this method the conversion is done from the {@link CosemObjct} to the OpenHAB value.
     * For CosemObjects containing more then one value post processing is needed
     *
     */
    @Override
    public synchronized void meterValueReceived(List<CosemObject> cosemObjects) {
        lastReceivedValues = Collections.emptyList();
        if (meter == null) {
            return;
        }
        List<CosemObject> filteredValues = meter.filterMeterValues(cosemObjects);

        if (!filteredValues.isEmpty()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Received {} objects for {}", filteredValues.size(), getThing().getThingTypeUID().getId());
            }
            lastReceivedValues = filteredValues;
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateState();
            }
        }
    }

    @Override
    public synchronized void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE
                && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            // Set status to offline --> Thing will become online after receiving meter values
            setDeviceOffline(ThingStatusDetail.NONE, null);
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            setDeviceOffline(ThingStatusDetail.BRIDGE_OFFLINE, null);
        }
    }

    /**
     * Convenience method to set the meter off line.
     *
     * @param status off line status
     * @param details off line detailed message
     */
    private void setDeviceOffline(ThingStatusDetail status, @Nullable String details) {
        updateStatus(ThingStatus.OFFLINE, status, details);
        getThing().getChannels().forEach(c -> updateState(c.getUID(), UnDefType.NULL));
    }
}
