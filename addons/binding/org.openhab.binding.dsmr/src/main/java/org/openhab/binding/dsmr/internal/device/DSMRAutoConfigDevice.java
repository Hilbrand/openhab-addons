/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPort;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortErrorEvent;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Simplified everything. Moved code around.
 */
@NonNullByDefault
public class DSMRAutoConfigDevice implements DSMRDevice, DSMRPortEventListener {
    private final Logger logger = LoggerFactory.getLogger(DSMRAutoConfigDevice.class);

    /*
     * Februari 2017
     * Due to the Dutch Smart Meter program where every residence is provided
     * a smart for free and the meters are DSMR V4 or higher
     * we assume the majority of meters communicate with HIGH_SPEED_SETTINGS
     * For older meters this means initializing is taking probably 1 minute
     */
    private final DSMRPortSettings DEFAULT_PORT_SETTINGS = DSMRPortSettings.HIGH_SPEED_SETTINGS;

    /**
     * Detector state
     */
    private enum DetectorState {
        /**
         *
         */
        DETECTING_SETTINGS,
        /**
         *
         */
        SWITCHING_BAUDRATE,
        /**
         *
         */
        NORMAL,
        /**
         *
         */
        ERROR
    }

    /**
     * The port name
     */
    private final String portName;

    /**
     * Listener for discovered devices
     */
    private final DSMRPortEventListener portEventListener;

    /**
    *
    */
    private final ScheduledExecutorService scheduler;

    /**
     * DSMR Port instance
     */
    private final DSMRPort dsmrPort;

    /**
     * Timer for handling discovery half time
     */
    @Nullable
    private ScheduledFuture<?> halfTimeTimer;

    /**
     * Timer for handling end of discovery
     */
    @Nullable
    private ScheduledFuture<?> endTimeTimer;

    /**
    *
    */
    private DetectorState state = DetectorState.NORMAL;

    private DSMRPortSettings portSettings;

    private final DSMRTelegramListener handler;

    /**
     * Creates a new DSMRBridgeDiscoveryHelper
     *
     * @param portName the port name (e.g. /dev/ttyUSB0 or COM1)
     * @param listener the {@link DSMRPortEventListener} to notify of new detected bridges
     * @param scheduler
     */
    public DSMRAutoConfigDevice(String serialPort, DSMRPortEventListener listener, ScheduledExecutorService scheduler) {
        handler = new DSMRTelegramListener(serialPort);
        dsmrPort = new DSMRPort(serialPort, true, handler);
        portName = dsmrPort.getPortName();
        this.portEventListener = listener;
        this.scheduler = scheduler;
        portSettings = DEFAULT_PORT_SETTINGS;
    }

    /**
     * Start the discovery process.
     */
    @Override
    public void start() {
        portSettings = DEFAULT_PORT_SETTINGS;
        handler.setDsmrPortListener(this);
        logger.debug("[{}] Start detecting port settings.", portName);
        halfTimeTimer = scheduler.schedule(this::switchBaudrate,
                DSMRDeviceConstants.SERIAL_PORT_AUTO_DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        endTimeTimer = scheduler.schedule(this::stop, DSMRDeviceConstants.DSMR_DISCOVERY_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);

        // dsmrPort = new DSMRPort(portName, this, null, null, true);
        dsmrPort.open(portSettings);
        state = DetectorState.DETECTING_SETTINGS;
    }

    @Override
    public synchronized void restart() {
        logger.debug("Restart: state:{}", state);
        if (state == DetectorState.NORMAL) { // not received any data
            dsmrPort.restart(portSettings);
            state = DetectorState.DETECTING_SETTINGS;
        } else if (state == DetectorState.ERROR || (endTimeTimer != null && endTimeTimer.isDone())) {
            // did receive anything but was an error.
            stop();
            start();
        }
    }

    /**
     *
     */
    @Override
    public synchronized void stop() {
        dsmrPort.close();
        if (state == DetectorState.DETECTING_SETTINGS) {
            state = DetectorState.ERROR;
        } else {
            state = DetectorState.NORMAL;
        }
        logger.trace("stopped with state:{}", state);
    }

    private void stopDetecting(DetectorState state) {
        logger.debug("[{}] Stop detecting port settings.", portName);
        if (halfTimeTimer != null) {
            halfTimeTimer.cancel(true);
        }
        if (endTimeTimer != null) {
            endTimeTimer.cancel(true);
        }
        this.state = state;
        handler.setDsmrPortListener(portEventListener);
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
    public synchronized void handleTelegramReceived(List<CosemObject> cosemObjects, String stateDetails) {
        logger.debug("[{}] Received {} cosemObjects, state:{}", portName, cosemObjects.size(), state);
        if (state == DetectorState.DETECTING_SETTINGS && !cosemObjects.isEmpty()) {
            stopDetecting(DetectorState.NORMAL);
            portEventListener.handleTelegramReceived(cosemObjects, stateDetails);
        }
    }

    /**
     * Event handler for DSMR Port events
     *
     * @param portEvent {@link DSMRPortErrorEvent} to handle
     */
    @Override
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent) {
        if (state == DetectorState.SWITCHING_BAUDRATE) {
            logger.debug("[{}] Received portEvent during switching baudrate {}", portName, portEvent);
            return;
        }
        logger.trace("[{}] Received portEvent {}", portName, portEvent.getEventDetails());
        switch (portEvent) {
            case DONT_EXISTS: // Port does not exists (unexpected, since it was there, so port is not usable)
            case IN_USE: // Port is in use
            case NOT_COMPATIBLE: // Port not compatible
                handleError(portEvent);
                break;
            case READ_ERROR: // read error(try switching port speed)
                switchBaudrate();
                break;
            default:
                // Unknown event, log and do nothing
                logger.warn("Unknown event {}", portEvent);
                break;

        }
    }

    /**
     * Called when an event was triggered that could
     *
     * @param portEvent
     */
    private synchronized void handleError(DSMRPortErrorEvent portEvent) {
        logger.debug("[{}] Error during detecting port settings: {}, current state:{}.", portName,
                portEvent.getEventDetails(), state);
        if (state == DetectorState.DETECTING_SETTINGS) {
            stopDetecting(DetectorState.ERROR);
            portEventListener.handlePortErrorEvent(portEvent);
        }
    }

    /**
    *
    */
    private synchronized void switchBaudrate() {
        if (state == DetectorState.DETECTING_SETTINGS) {
            if (halfTimeTimer != null) {
                halfTimeTimer.cancel(true);
            }
            state = DetectorState.SWITCHING_BAUDRATE;
            logger.debug(
                    "[{}] Detecting port settings is running for half time now and still nothing discovered, switching baudrate and retrying",
                    portName);
            portSettings = portSettings == DSMRPortSettings.HIGH_SPEED_SETTINGS ? DSMRPortSettings.LOW_SPEED_SETTINGS
                    : DSMRPortSettings.HIGH_SPEED_SETTINGS;
            dsmrPort.restart(portSettings);
            state = DetectorState.DETECTING_SETTINGS;
        }
    }
}
