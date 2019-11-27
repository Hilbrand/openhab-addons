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
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.smappee.internal.model.SmappeeApplianceEvent;
import org.openhab.binding.smappee.internal.service.SmappeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SmappeeApplianceHandler} is responsible for handling commands and sets the actual status for an appliance.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class SmappeeApplianceHandler extends AbstractSmappeeHandler {

    private final Logger logger = LoggerFactory.getLogger(SmappeeApplianceHandler.class);

    private @Nullable ScheduledFuture<?> scheduledJob;

    private @NonNullByDefault({}) String applianceId;

    public SmappeeApplianceHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        final SmappeeService smappeeService = getSmappeeService();
        if (smappeeService == null || !smappeeService.isInitialized()) {
            return;
        }

        if (command instanceof RefreshType) {
            readAppliance(smappeeService);
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    public void newState(final SmappeeApplianceEvent readings) {
        if (readings != null) {
            updateState(CHANNEL_APPLIANCE_POWER, new DecimalType(readings.activePower));
            updateState(CHANNEL_APPLIANCE_LASTUPDATE, new DateTimeType(readings.getTimestamp()));
        }
    }

    @Override
    public void initialize() {
        applianceId = thing.getConfiguration().get(PARAMETER_APPLIANCE_ID).toString();

        // start automatic refresh
        startAutomaticRefresh();
    }

    @Override
    public void dispose() {
        // stop automatic refresh
        if (scheduledJob != null) {
            scheduledJob.cancel(true);
            scheduledJob = null;
        }
    }

    public void startAutomaticRefresh() {
        final SmappeeService smappeeService = getSmappeeService();
        final Runnable runnable = () -> {
            if (smappeeService != null && smappeeService.isInitialized()) {
                readAppliance(smappeeService);
            }
        };

        scheduledJob = scheduler.scheduleWithFixedDelay(runnable, 0, smappeeService.getConfig().pollingInterval,
                TimeUnit.MILLISECONDS);
    }

    private void readAppliance(final SmappeeService smappeeService) {
        final SmappeeApplianceEvent readings = smappeeService.getLatestApplianceReading(applianceId);
        if (readings == null) {
            logger.debug("failed to read to read power consumption for appliance {}", applianceId);
        } else {
            newState(readings);
        }
    }
}
