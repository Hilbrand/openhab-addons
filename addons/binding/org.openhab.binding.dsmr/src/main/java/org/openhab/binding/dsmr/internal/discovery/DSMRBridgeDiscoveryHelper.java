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
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.internal.device.DSMRAutoConfigDevice;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceConstants;
import org.openhab.binding.dsmr.internal.device.DSMRPortEventListener;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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
 * @author Hilbrand Bouwkamp -
 */
@NonNullByDefault
class DSMRBridgeDiscoveryHelper implements DSMRPortEventListener {
    private final Logger logger = LoggerFactory.getLogger(DSMRBridgeDiscoveryHelper.class);

    // /**
    // * Discovery state
    // */
    // private enum DiscoveryState {
    // /**
    // *
    // */
    // RUNNING,
    // /**
    // *
    // */
    // SWITCHING_BAUDRATE,
    // /**
    // *
    // */
    // FINISHED
    // }

    /**
     * The port name
     */
    private final String portName;

    /**
     * Listener for discovered devices
     */
    private final DSMRBridgeDiscoveryListener discoveryListener;

    /**
    *
    */
    private final ScheduledExecutorService scheduler;

    /**
     *
     */
    // private DiscoveryState state = DiscoveryState.FINISHED;

    /**
     * DSMR Port instance
     */
    // @Nullable
    // private DSMRPort dsmrPort;

    /**
     * Timer for handling discovery half time
     */
    // @Nullable
    // private ScheduledFuture<?> halfTimeTimer;

    /**
     * Timer for handling end of discovery
     */
    // @Nullable
    // private ScheduledFuture<?> endTimeTimer;

    @Nullable
    private DSMRAutoConfigDevice detector;

    // private Semaphore discoveryLock = new Semaphore(0);

    /**
     * Creates a new DSMRBridgeDiscoveryHelper
     *
     * @param portName the port name (e.g. /dev/ttyUSB0 or COM1)
     * @param listener the {@link DSMRMeterDiscoveryListener} to notify of new detected bridges
     * @param scheduler
     */
    public DSMRBridgeDiscoveryHelper(String portName, DSMRBridgeDiscoveryListener listener,
            ScheduledExecutorService scheduler) {
        this.portName = portName;
        this.discoveryListener = listener;
        this.scheduler = scheduler;
    }

    /**
     * Start the discovery process.
     */
    public void startDiscovery() {
        // try {
        // discoveryLock.acquire();
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // }
        logger.debug("[{}] Start discovery", portName);
        // halfTimeTimer = scheduler.schedule(this::switchBaudrate,
        // DSMRDeviceConstants.SERIAL_PORT_AUTO_DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // endTimeTimer = scheduler.schedule(this::stopScan, DSMRDeviceConstants.DSMR_DISCOVERY_TIMEOUT_SECONDS,
        // TimeUnit.SECONDS);

        // dsmrPort = new DSMRPort(portName, true);
        detector = new DSMRAutoConfigDevice(portName, this, scheduler);
        detector.start();
    }

    // /**
    // *
    // */
    // private synchronized void switchBaudrate() {
    // if (dsmrPort != null && state == DiscoveryState.RUNNING) {
    // state = DiscoveryState.SWITCHING_BAUDRATE;
    // logger.debug(
    // "[{}] Discovery is running for half time now and still nothing discovered, switching baudrate and retrying",
    // portName);
    // dsmrPort.switchPortSpeed();
    // state = DiscoveryState.RUNNING;
    // }
    // }

    /**
     * Called when an event was triggered that could
     */
    // private synchronized void handleError() {
    // if (state == DiscoveryState.RUNNING) {
    // stopScan();
    // }
    // }

    @Override
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent) {
        stopScan();
        logger.debug("Error on port [{}] during discovery.", portName);
    }

    /**
     *
     */
    public synchronized void stopScan() {
        logger.trace("Stop discovery on port [{}].", portName);
        if (detector != null) {
            detector.stop();
        }
        // if (halfTimeTimer != null) {
        // halfTimeTimer.cancel(true);
        // }
        // if (endTimeTimer != null) {
        // endTimeTimer.cancel(true);
        // }

        // if (dsmrPort != null) {
        // dsmrPort.close();
        // dsmrPort = null;
        // }
        // state = DiscoveryState.FINISHED;
        // if (discoveryLock.availablePermits() == 0) {
        // discoveryLock.release();
        // }
    }

    // /**
    // * Event handler for DSMR Port events
    // *
    // * @param portEvent {@link DSMRPortEvent} to handle
    // */
    // @Override
    // public void handleDSMRPortEvent(DSMRPortEvent portEvent) {
    // if (state == DiscoveryState.SWITCHING_BAUDRATE) {
    // logger.debug("[{}] Received portEvent during switching baudrate {}", portName, portEvent);
    // return;
    // }
    // logger.trace("[{}] Received portEvent {}", portName, portEvent);
    // switch (portEvent) {
    // // case CLOSED: // Port closed is an expected event when switch baudrate, ignore
    // // break;
    // case DONT_EXISTS:
    // // Port does not exists (unexpected, since it was there, so port is not usable)
    // handleError();
    // break;
    // case IN_USE:
    // // Port is in use
    // handleError(/* portEvent.getEventDetails() */);
    // break;
    // // case LINE_BROKEN:
    // // No data available (try switching port speed)
    // // switchBaudrate();
    // // break;
    // case NOT_COMPATIBLE:
    // // Port not compatible
    // handleError();
    // break;
    // // case OPENED:
    // // // Port is opened (this is expected), ignore this events
    // // break;
    // case READ_ERROR:
    // // read error(try switching port speed)
    // switchBaudrate();
    // break;
    // // case READ_OK:
    // // Read is successful, so wait for telegrams and ignore this event
    // // break;
    // // case WRONG_BAUDRATE:
    // // wrong baud rate (try switching port speed)
    // // switchBaudrate();
    // // break;
    // default:
    // // Unknown event, log and do nothing
    // logger.warn("Unknown event {}", portEvent);
    // break;
    //
    // }
    // }

    /**
     * Handle if telegrams are received.
     *
     * If there are cosem objects received a new bridge will we discovered
     *
     * @param cosemObjects list of {@link CosemObject}
     * @param stateDetails the details of the received telegram (this parameter is ignored)
     */
    @Override
    public synchronized void handleTelegramReceived(List<CosemObject> cosemObjects, String stateDetails) {
        logger.debug("[{}] Received {} cosemObjects", portName, cosemObjects.size());
        if (!cosemObjects.isEmpty()) {
            stopScan();
            discoveryListener.bridgeDiscovered(portName, cosemObjects);
        }
    }

}
