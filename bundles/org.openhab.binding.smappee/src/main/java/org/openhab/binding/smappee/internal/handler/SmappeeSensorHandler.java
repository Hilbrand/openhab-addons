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
package org.openhab.binding.smappee.internal.handler;

import static org.openhab.binding.smappee.internal.SmappeeBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.smappee.internal.model.SmappeeSensorConsumptionRecord;
import org.openhab.binding.smappee.internal.service.SmappeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SmappeeSensorHandler} is responsible for handling commands and sets the actual status for a sensor.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class SmappeeSensorHandler extends AbstractSmappeeHandler {

    private final Logger logger = LoggerFactory.getLogger(SmappeeSensorHandler.class);

    private @Nullable ScheduledFuture<?> scheduledJob;

    private @NonNullByDefault({}) String sensorId;
    private @NonNullByDefault({}) String channelId;

    public SmappeeSensorHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        final SmappeeService smappeeService = getSmappeeService();
        if (smappeeService == null || !smappeeService.isInitialized()) {
            return;
        }

        if (command instanceof RefreshType) {
            readSensor(smappeeService);
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    public void newState(final SmappeeSensorConsumptionRecord readings) {
        if ("1".equals(channelId)) {
            updateState(CHANNEL_SENSOR_VALUE, new DecimalType(readings.value1));
        } else if ("2".equals(channelId)) {
            updateState(CHANNEL_SENSOR_VALUE, new DecimalType(readings.value2));
        }
    }

    @Override
    public void initialize() {
        sensorId = thing.getConfiguration().get(PARAMETER_SENSOR_ID).toString();
        channelId = thing.getConfiguration().get(PARAMETER_SENSOR_CHANNEL_ID).toString();

        // start automatic refresh
        startAutomaticRefresh();
    }

    @Override
    public void dispose() {
        // stop automatic refresh
        if (scheduledJob != null) {
            scheduledJob.cancel(true);
        }
    }

    public void startAutomaticRefresh() {
        final SmappeeService smappeeService = getSmappeeService();
        final Runnable runnable = () -> {
            if (smappeeService != null && smappeeService.isInitialized()) {
                readSensor(smappeeService);
            }
        };

        scheduledJob = scheduler.scheduleWithFixedDelay(runnable, 0, smappeeService.getConfig().pollingInterval,
                TimeUnit.MILLISECONDS);
    }

    private void readSensor(final SmappeeService smappeeService) {
        final SmappeeSensorConsumptionRecord readings = smappeeService.getLatestSensorConsumption(sensorId);
        if (readings == null) {
            logger.debug("failed to read to read power consumption for sensor {}", sensorId);
        } else {
            newState(readings);
        }
    }
}
