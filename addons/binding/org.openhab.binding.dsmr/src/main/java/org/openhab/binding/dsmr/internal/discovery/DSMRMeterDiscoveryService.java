/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.discovery;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dsmr.handler.DSMRBridgeHandler;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implements the discovery service for new DSMR Meters
 *
 * @author M. Volaart - Initial contribution
 */
@NonNullByDefault
public class DSMRMeterDiscoveryService extends DSMRDiscoveryService implements DSMRMeterListener {

    private final Logger logger = LoggerFactory.getLogger(DSMRMeterDiscoveryService.class);

    /**
     * The DSMRBridgeHandler instance
     */
    private final DSMRBridgeHandler dsmrBridgeHandler;

    /**
     * Constructs a new DSMRMeterDiscoveryService with the specified DSMR Bridge ThingUID
     *
     * @param dsmrBridgeUID ThingUID for the DSMR Bridges
     */
    public DSMRMeterDiscoveryService(DSMRBridgeHandler dsmrBridgeHandler) {
        this.dsmrBridgeHandler = dsmrBridgeHandler;
    }

    /**
     * Manual scanning is not supported for meters. The bridge will handle this automatically
     */
    @Override
    protected void startScan() {
        dsmrBridgeHandler.registerDSMRMeterListener(this);
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        dsmrBridgeHandler.unregisterDSMRMeterListener(this);
    }

    @Override
    public void meterValueReceived(List<CosemObject> cosemObjects) {
        logger.debug("Detect meters from #{} objects", cosemObjects.size());
        meterDetector.detectMeters(cosemObjects)
                .forEach(m -> meterDiscovered(m, dsmrBridgeHandler.getThing().getUID()));
    }
}
