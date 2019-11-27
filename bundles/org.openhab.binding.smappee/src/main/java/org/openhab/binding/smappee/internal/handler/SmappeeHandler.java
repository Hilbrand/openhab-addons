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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthFactory;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.smappee.internal.discovery.SmappeeDiscoveryService;
import org.openhab.binding.smappee.internal.exception.CommunicationException;
import org.openhab.binding.smappee.internal.exception.InvalidConfigurationException;
import org.openhab.binding.smappee.internal.model.SmappeeDeviceReading;
import org.openhab.binding.smappee.internal.service.SmappeeConfigurationParameters;
import org.openhab.binding.smappee.internal.service.SmappeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SmappeeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class SmappeeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SmappeeHandler.class);
    private final HttpClient httpClient;
    private final OAuthFactory oAuthFactory;

    private @Nullable ScheduledFuture<?> scheduledInitialize;
    private @Nullable ScheduledFuture<?> scheduledJob;
    private @Nullable SmappeeService smappeeService;
    private @Nullable SmappeeConfigurationParameters config;

    public SmappeeHandler(final Bridge bridge, final HttpClient httpClient, final OAuthFactory oAuthFactory) {
        super(bridge);
        this.httpClient = httpClient;
        this.oAuthFactory = oAuthFactory;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(SmappeeDiscoveryService.class);
    }

    public @Nullable SmappeeService getSmappeeService() {
        return smappeeService;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (smappeeService == null || !smappeeService.isInitialized()) {
            return;
        }

        if (command instanceof RefreshType) {
            final SmappeeDeviceReading readings = smappeeService.getDeviceReadings();
            newState(readings);
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    public void newState(final @Nullable SmappeeDeviceReading readings) {
        if (readings != null) {
            updateState(CHANNEL_CONSUMPTION, new QuantityType<>(readings.getLatestConsumption(), SmartHomeUnits.WATT));
            updateState(CHANNEL_SOLAR, new QuantityType<>(readings.getLatestSolar(), SmartHomeUnits.WATT));
            updateState(CHANNEL_ALWAYSON, new QuantityType<>(readings.getLatestAlwaysOn(), SmartHomeUnits.WATT));
        }
    }

    @Override
    public void initialize() {
        final SmappeeConfigurationParameters config = getConfigAs(SmappeeConfigurationParameters.class);
        this.config = config;

        if (config.clientId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Check configuration, Client Id must be provided");
            return;
        }
        if (config.clientSecret.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Check configuration, Client secret must be provided");
            return;
        }
        if (config.username.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Check configuration, Username must be provided");
            return;
        }
        if (config.password.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Check configuration, Password must be provided");
            return;
        }
        if (config.serviceLocationName.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Check configuration, Service location name must be provided");
            return;
        }

        logger.debug("Initialize Network handler.");

        smappeeService = new SmappeeService(thing.getUID().getAsString(), config, httpClient, oAuthFactory);

        // contact Smappee API
        scheduledInitialize = scheduler.schedule(this::initializeSmappeeService, 0, TimeUnit.SECONDS);
    }

    private void initializeSmappeeService() {
        try {
            smappeeService.initialize();
            // ok, initialization succeeded
            startAutomaticRefresh();

            scheduledInitialize = null;
            updateStatus(ThingStatus.ONLINE);
        } catch (final CommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Could not contact Smappee, retrying");

            if (smappeeService != null) { // only reschedule if smappeeService is not disposed.
                scheduledInitialize = scheduler.schedule(this::initializeSmappeeService, 30, TimeUnit.SECONDS);
            }
        } catch (final InvalidConfigurationException ex) {
            // bad configuration, stay offline until user corrects the configuration
            scheduledInitialize = null;
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
        }
    }

    public void startAutomaticRefresh() {
        scheduledJob = scheduler.scheduleWithFixedDelay(() -> {
            final SmappeeDeviceReading readings = smappeeService.getDeviceReadings();

            // sometimes smappee returns reading without aggregated consumptions
            if (readings != null && readings.consumptions.length > 0) {
                newState(readings);
            }
        }, 0, config.pollingInterval, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        if (scheduledJob != null) {
            scheduledJob.cancel(true);
            scheduledJob = null;
        }
        if (scheduledInitialize != null) {
            scheduledInitialize.cancel(true);
            scheduledInitialize = null;
        }
    }
}
