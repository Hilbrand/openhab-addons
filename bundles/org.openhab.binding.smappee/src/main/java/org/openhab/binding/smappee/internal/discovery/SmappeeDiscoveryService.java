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
package org.openhab.binding.smappee.internal.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.smappee.internal.SmappeeBindingConstants;
import org.openhab.binding.smappee.internal.handler.SmappeeHandler;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationInfo;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationInfoActuator;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationInfoAppliance;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationInfoSensor;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationInfoSensorChannel;
import org.openhab.binding.smappee.internal.service.SmappeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery class for the Smappee.
 * This will detect the switches in your home.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class SmappeeDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService, DiscoveryService {

    private static final int SEARCH_TIME_SECONDS = 60;

    private final Logger logger = LoggerFactory.getLogger(SmappeeDiscoveryService.class);

    private @Nullable SmappeeHandler smappeeHandler;
    private @Nullable ScheduledFuture<?> scheduledJob;

    /**
     * Whether we are currently scanning or not
     */
    private boolean isScanning;

    /**
     * Constructs the discovery class using the thing IDs that smappee can discover
     * - actuators (plugs)
     * - detected appliances
     */
    public SmappeeDiscoveryService(final SmappeeHandler smappeeHandler) {
        super(Stream.of(SmappeeBindingConstants.THING_TYPE_ACTUATOR, SmappeeBindingConstants.THING_TYPE_APPLIANCE)
                .collect(Collectors.toSet()), SEARCH_TIME_SECONDS, false);

        this.smappeeHandler = smappeeHandler;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SmappeeBindingConstants.SUPPORTED_THING_TYPES_UIDS;
    }

    /**
     * Activate the discovery service.
     */
    @Override
    public void activate() {
        super.activate(null);
        startScan();
    }

    /**
     * Deactivate the discovery service.
     */
    @Override
    public void deactivate() {
        super.deactivate();
        stopScan();
    }

    /**
     * Starts the scan. This discovery will:
     * <ul>
     * <li>Call 'get service location info'
     * (https://smappee.atlassian.net/wiki/spaces/DEVAPI/pages/8552482/Get+Servicelocation+Info)</li>
     * <li>For each actuator, add a thing in the inbox (if it doesn't exist already)</li>
     * <li>For each appliance, add a thing in the inbox (if it doesn't exist already)</li>
     * </ul>
     * The process will continue until {@link #stopScan()} is called.
     */
    @Override
    protected void startScan() {
        if (isScanning) {
            stopScan();
        }

        // this should be done by base class ???
        // somehow this is not working, so starting a scheduler instead
        startAutomaticRefresh();

        isScanning = true;
    }

    public void startAutomaticRefresh() {
        scheduledJob = scheduler.scheduleWithFixedDelay(this::scanForNewDevices, 0, 5, TimeUnit.MINUTES);
    }

    private void scanForNewDevices() {
        final SmappeeService smappeeService = smappeeHandler.getSmappeeService();

        if (smappeeService == null || !smappeeService.isInitialized()) {
            logger.debug("skipping discovery because smappee service is not up yet (config error ?)");
            return;
        }

        logger.debug("Starting Discovery");

        final SmappeeServiceLocationInfo serviceLocationInfo = smappeeService.getServiceLocationInfo();

        if (serviceLocationInfo == null) {
            logger.debug("failed to scan for new smappee devices");
            return;
        }

        for (final SmappeeServiceLocationInfoActuator actuator : serviceLocationInfo.actuators) {
            final ThingTypeUID typeId = SmappeeBindingConstants.THING_TYPE_ACTUATOR;

            addNewDiscoveredThing(actuator.id, actuator.name, typeId);
        }

        for (final SmappeeServiceLocationInfoAppliance appliance : serviceLocationInfo.appliances) {
            if ("Find me".equals(appliance.type)) {
                continue; // skip
            }

            final ThingTypeUID typeId = SmappeeBindingConstants.THING_TYPE_APPLIANCE;
            addNewDiscoveredThing(appliance.id, appliance.name, typeId,
                    Collections.singletonMap("type", appliance.type));
        }

        for (final SmappeeServiceLocationInfoSensor sensor : serviceLocationInfo.sensors) {
            for (final SmappeeServiceLocationInfoSensorChannel channel : sensor.channels) {
                final String id = sensor.id + "-" + channel.channel;
                final ThingTypeUID typeId = SmappeeBindingConstants.THING_TYPE_SENSOR;

                addNewDiscoveredThing(id, channel.type + " sensor", typeId,
                        Collections.singletonMap("type", channel.type));
            }
        }
    }

    private void addNewDiscoveredThing(final String id, final String label, final ThingTypeUID typeId) {
        addNewDiscoveredThing(id, label, typeId, Collections.emptyMap());
    }

    private void addNewDiscoveredThing(final String id, final String label, final ThingTypeUID typeId,
            final Map<String, String> properties) {
        logger.debug("Creating new {} thing with id {}", typeId, id);
        final ThingUID newthing = new ThingUID(typeId, id);

        final Map<String, Object> thingProperties = new HashMap<>(1);
        thingProperties.put("id", id);
        thingProperties.putAll(properties);

        final ThingUID bridgeUID = smappeeHandler.getThing().getUID();
        final DiscoveryResult result = DiscoveryResultBuilder.create(newthing).withBridge(bridgeUID)
                .withProperties(thingProperties).withLabel(label).build();
        thingDiscovered(result);
    }

    /**
     * Stops the discovery scan.
     */
    @Override
    public synchronized void stopScan() {
        super.stopScan();

        if (scheduledJob != null) {
            scheduledJob.cancel(true);
        }

        isScanning = false;
    }

    @Override
    public void setThingHandler(final @Nullable ThingHandler handler) {
        if (handler instanceof SmappeeHandler) {
            this.smappeeHandler = (SmappeeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return smappeeHandler;
    }
}
