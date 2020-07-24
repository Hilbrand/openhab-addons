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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.thing.util.ThingHandlerHelper;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.enphase.internal.discovery.EnphaseInverterDiscoveryService;
import org.openhab.binding.enphase.internal.dto.EnvoyEnergyDTO;
import org.openhab.binding.enphase.internal.dto.InverterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BridgeHandler for the Envoy gateway.
 *
 * @author Thomas Hentschel - Initial contribution
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class EnvoyBridgeHandler extends BaseBridgeHandler {

    private static final long RETRY_RECONNECT_SECONDS = 10;

    private final Logger logger = LoggerFactory.getLogger(EnvoyBridgeHandler.class);
    private final EnvoyConnector connector;
    private final EnvoyHostAddressCache envoyHostnameCache;

    private @NonNullByDefault({}) EnvoyConfiguration configuration;
    private @Nullable ScheduledFuture<?> inverterFuture;
    private @Nullable ScheduledFuture<?> retryFuture;
    private @Nullable ExpiringCache<Map<String, @Nullable InverterDTO>> invertersCache;
    private @Nullable EnvoyEnergyDTO productionDTO;
    private @Nullable EnvoyEnergyDTO consumptionDTO;

    public EnvoyBridgeHandler(final Bridge thing, final HttpClient httpClient,
            final EnvoyHostAddressCache envoyHostAddressCache) {
        super(thing);
        connector = new EnvoyConnector(httpClient);
        this.envoyHostnameCache = envoyHostAddressCache;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (command instanceof RefreshType) {
            refresh(channelUID);
        }
    }

    private void refresh(final ChannelUID channelUID) {
        final EnvoyEnergyDTO data = ENVOY_CHANNELGROUP_CONSUMPTION.equals(channelUID.getGroupId()) ? consumptionDTO
                : productionDTO;

        updateWattHoursToday(channelUID, data);
        updateWattHoursSevenDays(channelUID, data);
        updateWattHoursLifetime(channelUID, data);
        updateWattsNow(channelUID, data);
    }

    private void updateWattHoursToday(final ChannelUID channelUID, @Nullable final EnvoyEnergyDTO productionDTO) {
        if (ENVOY_WATT_HOURS_TODAY.equals(channelUID.getIdWithoutGroup())) {
            updateState(channelUID, productionDTO == null ? UnDefType.UNDEF
                    : new QuantityType<>(productionDTO.wattHoursToday, SmartHomeUnits.WATT_HOUR));
        }
    }

    private void updateWattHoursSevenDays(final ChannelUID channelUID, @Nullable final EnvoyEnergyDTO productionDTO) {
        if (ENVOY_WATT_HOURS_SEVEN_DAYS.equals(channelUID.getIdWithoutGroup())) {
            updateState(channelUID, productionDTO == null ? UnDefType.UNDEF
                    : new QuantityType<>(productionDTO.wattHoursSevenDays, SmartHomeUnits.WATT_HOUR));
        }
    }

    private void updateWattHoursLifetime(final ChannelUID channelUID, @Nullable final EnvoyEnergyDTO productionDTO) {
        if (ENVOY_WATT_HOURS_LIFETIME.equals(channelUID.getIdWithoutGroup())) {
            updateState(channelUID, productionDTO == null ? UnDefType.UNDEF
                    : new QuantityType<>(productionDTO.wattHoursLifetime, SmartHomeUnits.WATT_HOUR));
        }
    }

    private void updateWattsNow(final ChannelUID channelUID, @Nullable final EnvoyEnergyDTO productionDTO) {
        if (ENVOY_WATTS_NOW.equals(channelUID.getIdWithoutGroup())) {
            updateState(channelUID, productionDTO == null ? UnDefType.UNDEF
                    : new QuantityType<>(productionDTO.wattsNow, SmartHomeUnits.WATT));
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(EnphaseInverterDiscoveryService.class);
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(EnvoyConfiguration.class);
        if (!EnphaseBindingConstants.isValidSerial(configuration.serialnumber)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Serial Number is not valid");
        }
        if (configuration.hostname.isEmpty()) {
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Waiting for ip address to retrieve envoy gateway. Can take up to a minute.");
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
        connector.setConfiguration(configuration);
        invertersCache = new ExpiringCache<>(Duration.of(configuration.refresh, ChronoUnit.MINUTES),
                this::refreshInverters);
        inverterFuture = scheduler.scheduleWithFixedDelay(this::updateData, 0, configuration.refresh, TimeUnit.MINUTES);
    }

    private void refreshEnvoy() {
        try {
            productionDTO = connector.getProduction();
            if (configuration.consumption) {
                consumptionDTO = connector.getConsumption();
            }
            getThing().getChannels().stream().map(Channel::getUID).filter(this::isLinked).forEach(this::refresh);
        } catch (final EnvoyNoHostnameException e) {
            logger.debug(
                    "No hostname/ip address known for Envoy gateway, schedule update to get it from discovery service.");
            scheduleHostnameUpdate(false);
        } catch (final EnvoyConnectionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            scheduleHostnameUpdate(false);
        }
    }

    /**
     * Method called by the ExpiringCache when no inverter data is present to get the data from the Envoy gateway.
     * When there are connection problems it will start a scheduled job to try to reconnect to the
     *
     * @return the inverter data from the Envoy gateway or null if no data is available.
     */
    private @Nullable Map<String, @Nullable InverterDTO> refreshInverters() {
        try {
            return connector.getInverters().stream()
                    .collect(Collectors.toMap(InverterDTO::getSerialNumber, Function.identity()));
        } catch (final EnvoyNoHostnameException e) {
            logger.debug(
                    "No hostname/ip address known for Envoy gateway, schedule update to get it from discovery service.");
            scheduleHostnameUpdate(false);
        } catch (final EnvoyConnectionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            scheduleHostnameUpdate(false);
        }
        return null;
    }

    /**
     * Returns the data for the inverters. It get the data from cache or updates the cache if possible in case no data
     * is available.
     *
     * @param force force a cache refresh
     * @return data if present or null
     */
    public @Nullable Map<String, @Nullable InverterDTO> getInverters(final boolean force) {
        final ExpiringCache<Map<String, @Nullable InverterDTO>> invertersCache = this.invertersCache;

        if (invertersCache == null) {
            return null;
        } else {
            if (force) {
                invertersCache.invalidateValue();
            }
            return invertersCache.getValue();
        }
    }

    /**
     * Method called by the refresh thread.
     */
    private void updateData() {
        refreshEnvoy();
        final Map<String, @Nullable InverterDTO> inverters = getInverters(false);

        if (inverters != null) {
            getThing().getThings().stream().map(Thing::getHandler).filter(h -> h instanceof EnphaseInverterHandler)
                    .map(EnphaseInverterHandler.class::cast)
                    .forEach(invHandler -> invHandler.refreshState(inverters.get(invHandler.getSerialNumber())));

            if (ThingHandlerHelper.isHandlerInitialized(getThing()) && getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }
        }
    }

    /**
     * Schedules a hostname update, but only schedules the task when not yet running or forced.
     * Force is used to reschedule the task and should only be used from within {@link #updateHostname()}.
     *
     * @param force if true will always schedule the task
     */
    private synchronized void scheduleHostnameUpdate(final boolean force) {
        if (force || retryFuture == null) {
            logger.trace("Schedule hostname/ip address update for thing {} in {} seconds.", getThing().getUID(),
                    RETRY_RECONNECT_SECONDS);
            retryFuture = scheduler.schedule(this::updateHostname, RETRY_RECONNECT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void childHandlerInitialized(final ThingHandler childHandler, final Thing childThing) {
        final Map<String, @Nullable InverterDTO> inverters = getInverters(false);

        if (inverters == null || !(childHandler instanceof EnphaseInverterHandler)) {
            return;
        }
        final EnphaseInverterHandler invHandler = (EnphaseInverterHandler) childHandler;
        invHandler.refreshState(inverters.get(invHandler.getSerialNumber()));
    }

    /**
     * Handles a host name / ip address update.
     */
    private void updateHostname() {
        final String lastKnownHostname = envoyHostnameCache.getLastKnownHostAddress(configuration.serialnumber);

        if (lastKnownHostname.isEmpty()) {
            scheduleHostnameUpdate(true);
        } else {
            final Configuration config = editConfiguration();

            config.put(CONFIG_HOSTNAME, lastKnownHostname);
            logger.info("Envoy ({}) hostname/ip address set to {}", getThing().getUID(), lastKnownHostname);
            configuration.hostname = lastKnownHostname;
            connector.setConfiguration(configuration);
            updateData();
            updateConfiguration(config);
            // The task is done so the future can be released by setting it to null.
            retryFuture = null;
        }
    }

    @Override
    public void dispose() {
        final ScheduledFuture<?> retryFuture = this.retryFuture;
        if (retryFuture != null) {
            retryFuture.cancel(true);
        }
        final ScheduledFuture<?> inverterFuture = this.inverterFuture;

        if (inverterFuture != null) {
            inverterFuture.cancel(true);
        }
    }

    @Override
    public String toString() {
        return "EnvoyBridgeHandler(" + thing.getUID() + ") Status: " + thing.getStatus();
    }
}
