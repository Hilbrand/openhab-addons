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
package org.openhab.binding.enphase.internal.discovery;

import static org.openhab.binding.enphase.internal.EnphaseBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.enphase.internal.EnvoyBridgeHandler;
import org.openhab.binding.enphase.internal.dto.InverterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service to discovery Enphase inverters connected to an Envoy gateway.
 *
 * @author Thomas Hentschel - Initial contribution
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class EnphaseInverterDiscoveryService extends AbstractDiscoveryService
        implements ThingHandlerService, DiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(EnphaseInverterDiscoveryService.class);
    private static final int TIMEOUT_SECONDS = 20;

    private @Nullable EnvoyBridgeHandler envoyHandler;

    public EnphaseInverterDiscoveryService() {
        super(Collections.singleton(THING_TYPE_ENPHASE_INVERTER), TIMEOUT_SECONDS, false);
    }

    @Override
    public void setThingHandler(final @Nullable ThingHandler handler) {
        if (handler instanceof EnvoyBridgeHandler) {
            envoyHandler = (EnvoyBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return envoyHandler;
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
        removeOlderResults(getTimestampOfLastScan());
        scanForInverterThings();
    }

    private void scanForInverterThings() {
        final EnvoyBridgeHandler envoyHandler = this.envoyHandler;

        if (envoyHandler == null || envoyHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Envoy handler not available: {}", envoyHandler);
            return;
        }
        final Map<String, @Nullable InverterDTO> inverters = envoyHandler.getInverters(true);

        if (inverters == null) {
            logger.debug("No inverters found. Envoy connection returned nothing.");
            return;
        }
        final ThingUID bridgeID = envoyHandler.getThing().getUID();
        for (final Entry<String, @Nullable InverterDTO> entry : inverters.entrySet()) {
            final String serialNumber = entry.getKey();
            final ThingUID thingUID = new ThingUID(THING_TYPE_ENPHASE_INVERTER, defaultPassword(serialNumber));
            final Map<String, Object> properties = new HashMap<>(5);

            properties.put(CONFIG_SERIAL_NUMBER, serialNumber);
            final DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridgeID)
                    .withRepresentationProperty(serialNumber).withProperties(properties)
                    .withLabel("Enphase Inverter " + serialNumber).build();
            thingDiscovered(discoveryResult);
        }
    }
}
