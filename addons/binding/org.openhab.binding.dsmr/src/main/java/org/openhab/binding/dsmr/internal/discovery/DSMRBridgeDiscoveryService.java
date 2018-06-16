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
import org.openhab.binding.dsmr.internal.device.DSMRAutoConfigDevice;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceConstants;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceThread;
import org.openhab.binding.dsmr.internal.device.DSMRPortEventListener;
import org.openhab.binding.dsmr.internal.device.connector.DSMRPortErrorEvent;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;

/**
 * This implements the discovery service for detecting new DSMR Meters
 *
 * The class DSMRBridgeDiscoveryHelper try to identify a DSMR Bridge for a given Serial Port.
 *
 * The helper class will open the given serial port and wait for telegrams.
 * After {@link DSMRDeviceConstants#SERIAL_PORT_AUTO_DETECT_TIMEOUT_SECONDS} seconds it will switch the baud rate and
 * wait again for telegrams. After {@link DSMRDeviceConstants#DSMR_DISCOVERY_TIMEOUT_SECONDS} seconds the helper will
 * give up (assuming no DSMR Bridge is present)
 *
 * If a telegram is received with at least 1 Cosem Object a bridge is assumed available and a Thing is added
 * (regardless if there were problems receiving the telegram) and the discovery is stopped.
 *
 * If there are communication problems the baud rate is switched
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Added discovery of meters to discovery service
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.dsmr")
public class DSMRBridgeDiscoveryService extends DSMRDiscoveryService implements DSMRPortEventListener {

    private static final int _100 = 100;

    private final Logger logger = LoggerFactory.getLogger(DSMRBridgeDiscoveryService.class);

    private final DSMRMeterDetector meterDetector = new DSMRMeterDetector();

    @Nullable
    private DSMRDeviceThread currentScannedDevice;

    private String currentScannedPortName = "";

    private boolean scanning;

    /**
     * Starts a new discovery scan.
     *
     * All available Serial Ports are scanned for P1 telegrams.
     */
    @Override
    protected void startScan() {
        logger.debug("Started discovery scan");
        scanning = true;
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();

        // Traverse each available serial port
        while (scanning && portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();

            currentScannedPortName = portIdentifier.getName();
            logger.trace("Possible port to check:{}, owned:{} by:{}", currentScannedPortName,
                    portIdentifier.isCurrentlyOwned(), portIdentifier.getCurrentOwner());
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portIdentifier.isCurrentlyOwned()) {
                    if (DSMRBindingConstants.DSMR_PORT_NAME.equals(portIdentifier.getCurrentOwner())) {
                        logger.info("The port {} is owned by this binding. If no dsmr meters will be found it "
                                + "might indicate the port is locked by an older instance of this binding. "
                                + "Restart the system to unlock the port.", currentScannedPortName);
                    }
                } else {
                    logger.debug("Start discovery on serial port: {}", currentScannedPortName);
                    currentScannedDevice = new DSMRDeviceThread(
                            new DSMRAutoConfigDevice(portIdentifier.getName(), this, scheduler, _100));
                    currentScannedDevice.run();
                }
            }
        }
    }

    @Override
    protected synchronized void stopScan() {
        scanning = false;
        stopSerialPortScan();
        super.stopScan();
        logger.info("Stop discovery scan");
    }

    /**
    *
    */
    private void stopSerialPortScan() {
        logger.info("Stop discovery scan on port [{}].", currentScannedPortName);
        if (currentScannedDevice != null) {
            currentScannedDevice.stop();
        }
    }

    /**
     * Callback when a new bridge is discovered.
     * At this moment there is no reason why a bridge is not accepted.
     *
     * Therefore this method will always return true
     *
     * @return true if bridge is accepted, false otherwise
     */
    private boolean bridgeDiscovered(List<CosemObject> cosemObjects) {
        ThingUID thingUID = new ThingUID(DSMRBindingConstants.THING_TYPE_DSMR_BRIDGE,
                Integer.toHexString(currentScannedPortName.hashCode()));

        // Construct the configuration for this meter
        Map<String, Object> properties = new HashMap<>();
        properties.put("serialPort", currentScannedPortName);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withThingType(DSMRBindingConstants.THING_TYPE_DSMR_BRIDGE).withProperties(properties)
                .withLabel("DSMR bridge on " + currentScannedPortName).build();

        logger.debug("{} for serial port {}", discoveryResult, currentScannedPortName);

        thingDiscovered(discoveryResult);
        meterDetector.detectMeters(cosemObjects).forEach(m -> meterDiscovered(m, thingUID));
        return true;
    }

    /**
     * Handle if telegrams are received.
     *
     * If there are cosem objects received a new bridge will we discovered
     *
     * @param cosemObjects list of {@link CosemObject}
     * @param stateDetails the details of the received telegram (this parameter is ignored)
     */
    @Override
    public void handleTelegramReceived(List<CosemObject> cosemObjects, String stateDetails) {
        logger.debug("[{}] Received {} cosemObjects", currentScannedPortName, cosemObjects.size());
        if (!cosemObjects.isEmpty()) {
            bridgeDiscovered(cosemObjects);
            stopSerialPortScan();
        }
    }

    @Override
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent) {
        logger.debug("Error on port [{}] during discovery.", currentScannedPortName);
        stopSerialPortScan();
    }
}
