/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.discovery;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.dsmr.DSMRBindingConstants;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;

/**
 * This implements the discovery service for detecting new DSMR Meters
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Added discovery of meters to discovery service
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.dsmr")
public class DSMRBridgeDiscoveryService extends DSMRDiscoveryService implements DSMRBridgeDiscoveryListener {
    private final Logger logger = LoggerFactory.getLogger(DSMRBridgeDiscoveryService.class);

    private final DSMRMeterDetector meterDetector = new DSMRMeterDetector();

    @Nullable
    private DSMRBridgeDiscoveryHelper discoveryHelper;

    /**
     * Starts a new discovery scan.
     *
     * All available Serial Ports are scanned for P1 telegrams.
     */
    @Override
    protected void startScan() {
        logger.info("Started discovery scan");

        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();

        // Traverse each available serial port
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();

            // Check only available SERIAL ports
            logger.trace("Possible port to check:{}, owned:{} by:{}", portIdentifier.getName(),
                    portIdentifier.isCurrentlyOwned(), portIdentifier.getCurrentOwner());
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portIdentifier.isCurrentlyOwned()) {
                    if (DSMRBindingConstants.DSMR_PORT_NAME.equals(portIdentifier.getCurrentOwner())) {
                        logger.info("The port {} is owned by this binding. If no devices have been found yet it "
                                + "might indicate the port is locked by an older instance of this binding. "
                                + "Restart the system to unlock the port.", portIdentifier.getName());
                    }
                } else {
                    logger.debug("Start discovery for serial port: {}", portIdentifier.getName());
                    discoveryHelper = new DSMRBridgeDiscoveryHelper(portIdentifier.getName(), this, scheduler);
                    discoveryHelper.startDiscovery();
                }
            }
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        if (discoveryHelper != null) {
            discoveryHelper.stopScan();
        }
        logger.info("Finished discovery scan");
    }

    /**
     * Callback when a new bridge is discovered.
     * At this moment there is no reason why a bridge is not accepted.
     *
     * Therefore this method will always return true
     *
     * @param serialPort the serialPort name of the new discovered DSMRBridge Thing
     * @return true if bridge is accepted, false otherwise
     */
    @Override
    public boolean bridgeDiscovered(String serialPort, List<CosemObject> cosemObjects) {
        ThingUID thingUID = new ThingUID(DSMRBindingConstants.THING_TYPE_DSMR_BRIDGE,
                Integer.toHexString(serialPort.hashCode()));

        // Construct the configuration for this meter
        Map<String, Object> properties = new HashMap<>();
        properties.put("serialPort", serialPort);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withThingType(DSMRBindingConstants.THING_TYPE_DSMR_BRIDGE).withProperties(properties)
                .withLabel("DSMR bridge on " + serialPort).build();

        logger.debug("{} for serialPort {}", discoveryResult, serialPort);

        thingDiscovered(discoveryResult);
        meterDetector.detectMeters(cosemObjects).forEach(m -> meterDiscovered(m, thingUID));
        return true;
    }
}
