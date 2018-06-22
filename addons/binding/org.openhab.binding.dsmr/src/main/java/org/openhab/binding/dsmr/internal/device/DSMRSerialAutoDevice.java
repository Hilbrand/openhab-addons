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
import org.openhab.binding.dsmr.internal.device.connector.DSMRConnectorErrorEvent;
import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialConnector;
import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialSettings;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DSMR Serial device that auto discovers the serial port speed.
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - New class. contains code specific to discover the serial configuration.
 */
@NonNullByDefault
public class DSMRSerialAutoDevice implements DSMRDevice, DSMREventListener {

    /**
     * Enum to keep track of the internal state of {@link DSMRSerialAutoDevice}.
     */
    private enum DeviceState {
        /**
         * Discovers the settings of the serial port.
         */
        DISCOVER_SETTINGS,
        /**
         * Device is receiving telegram data from the serial port.
         */
        NORMAL,
        /**
         * Communication with serial port isn't working.
         */
        ERROR
    }

    /**
     * When switching baudrate ignore any errors received with the given time frame. Switching baudrate causes errors
     * and should not be interpreted as reading errors.
     */
    private static final long SWITCHING_BAUDRATE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    /**
     * This factor is multiplied with the {@link #baudrateSwitchTimeoutSeconds} and used as the duration the discovery
     * of the
     * baudrate may take.
     */
    private static final int DISCOVER_TIMEOUT_FACTOR = 4;

    /*
     * Februari 2017
     * Due to the Dutch Smart Meter program where every residence is provided
     * a smart for free and the meters are DSMR V4 or higher
     * we assume the majority of meters communicate with HIGH_SPEED_SETTINGS
     * For older meters this means initializing is taking probably 1 minute
     */
    private static final DSMRSerialSettings DEFAULT_PORT_SETTINGS = DSMRSerialSettings.HIGH_SPEED_SETTINGS;

    private final Logger logger = LoggerFactory.getLogger(DSMRSerialAutoDevice.class);

    /**
     * The serial port name
     */
    private final String serialPortName;

    /**
     * DSMR Connector to the serial port
     */
    private final DSMRSerialConnector dsmrConnector;
    private final ScheduledExecutorService scheduler;
    private final DSMRTelegramListener telegramListener;

    /**
     * Time in seconds in which period valid data is expected during discovery. If exceeded without success the baudrate
     * is switches
     */
    private final int baudrateSwitchTimeoutSeconds;

    /**
     * Serial port connection settings
     */
    private DSMRSerialSettings portSettings;

    /**
     * Keeps track of the state this instance is in.
     */
    private DeviceState state = DeviceState.NORMAL;

    /**
     * Timer for handling discovery of a single setting.
     */
    @Nullable
    private ScheduledFuture<?> halfTimeTimer;

    /**
     * Timer for handling end of discovery.
     */
    @Nullable
    private ScheduledFuture<?> endTimeTimer;

    /**
     *
     */
    private DSMREventListener parentListener;

    /**
     * Time in nanos the last time the baudrate was switched. This is used during discovery ignore errors retrieved
     * after switching baudrate for the period set in {@link #SWITCHING_BAUDRATE_TIMEOUT_NANOS}.
     */
    private long lastSwitchedBaudrateNanos;

    /**
     * Creates a new {@link DSMRSerialAutoDevice}
     *
     * @param serialPortName the port name (e.g. /dev/ttyUSB0 or COM1)
     * @param telegramListener the {@link DSMREventListener}
     * @param scheduler the scheduler to use with the baudrate switching timers
     * @param baudrateSwitchTimeoutSeconds timeout period for when to try other baudrate settings and end the discovery
     *            of the baudrate
     */
    public DSMRSerialAutoDevice(String serialPortName, DSMREventListener listener, ScheduledExecutorService scheduler,
            int baudrateSwitchTimeoutSeconds) {
        this.parentListener = listener;
        this.scheduler = scheduler;
        this.baudrateSwitchTimeoutSeconds = baudrateSwitchTimeoutSeconds;
        telegramListener = new DSMRTelegramListener();
        telegramListener.setDsmrEventListener(listener);
        portSettings = DEFAULT_PORT_SETTINGS;
        dsmrConnector = new DSMRSerialConnector(serialPortName, telegramListener);
        this.serialPortName = dsmrConnector.getPortName();
    }

    @Override
    public void start() {
        logger.debug("[{}] Start discover port settings.", serialPortName);
        stopDiscover(DeviceState.DISCOVER_SETTINGS);
        portSettings = DEFAULT_PORT_SETTINGS;
        telegramListener.setDsmrEventListener(this);
        dsmrConnector.open(portSettings);
        restartHalfTimer();
        endTimeTimer = scheduler.schedule(this::stopEndScheduler,
                baudrateSwitchTimeoutSeconds * DISCOVER_TIMEOUT_FACTOR, TimeUnit.SECONDS);
    }

    @Override
    public void restart() {
        if (state == DeviceState.ERROR) {
            stop();
            start();
        } else if (state == DeviceState.NORMAL) {
            dsmrConnector.restart(portSettings);
        }
    }

    @Override
    public synchronized void stop() {
        dsmrConnector.close();
        stopDiscover(state);
        logger.trace("stopped with state:{}", state);
    }

    /**
     * Handle if telegrams are received.
     *
     * If there are cosem objects received a new bridge will we discovered
     *
     * @param cosemObjects list of {@link CosemObject}
     * @param telegramDetails the details of the received telegram (this parameter is ignored)
     */
    @Override
    public void handleTelegramReceived(List<CosemObject> cosemObjects, String telegramDetails) {
        logger.debug("[{}] Received {} cosemObjects, state:{}", serialPortName, cosemObjects.size(), state);
        if (!cosemObjects.isEmpty()) {
            stopDiscover(DeviceState.NORMAL);
            parentListener.handleTelegramReceived(cosemObjects, telegramDetails);
        }
    }

    /**
     * Event handler for DSMR Port events
     *
     * @param portEvent {@link DSMRConnectorErrorEvent} to handle
     */
    @Override
    public void handleErrorEvent(DSMRConnectorErrorEvent portEvent) {
        logger.trace("[{}] Received portEvent {}", serialPortName, portEvent.getEventDetails());
        if (portEvent == DSMRConnectorErrorEvent.READ_ERROR) {
            switchBaudrate();
        } else {
            logger.debug("[{}] Error during discovery of port settings: {}, current state:{}.", serialPortName,
                    portEvent.getEventDetails(), state);
            stopDiscover(DeviceState.ERROR);
            parentListener.handleErrorEvent(portEvent);
        }
    }

    /**
     * Switches the baudrate on the serial port.
     */
    private void switchBaudrate() {
        if (lastSwitchedBaudrateNanos + SWITCHING_BAUDRATE_TIMEOUT_NANOS > System.nanoTime()) {
            // Ignore switching baudrate if this is called within the timeout after a previous switch.
            return;
        }
        lastSwitchedBaudrateNanos = System.nanoTime();
        if (state == DeviceState.DISCOVER_SETTINGS) {
            restartHalfTimer();
            logger.debug(
                    "[{}] Discover port settings is running for half time now and still nothing discovered, switching baudrate and retrying",
                    serialPortName);
            portSettings = portSettings == DSMRSerialSettings.HIGH_SPEED_SETTINGS
                    ? DSMRSerialSettings.LOW_SPEED_SETTINGS
                    : DSMRSerialSettings.HIGH_SPEED_SETTINGS;
            dsmrConnector.setSerialPortParams(portSettings);
        }
    }

    /**
     * Stops the discovery process as triggered by the end timer. It will only act if the discovery process was still
     * running.
     */
    private void stopEndScheduler() {
        if (state == DeviceState.DISCOVER_SETTINGS) {
            stopDiscover(DeviceState.ERROR);
            parentListener.handleErrorEvent(DSMRConnectorErrorEvent.DONT_EXISTS);
        }
    }

    /**
     * Stops the discovery of port baudrate process and sets the state with which it should be stopped.
     *
     * @param state the state with which the process was stopped.
     */
    private void stopDiscover(DeviceState state) {
        telegramListener.setDsmrEventListener(parentListener);
        logger.debug("[{}] Stop discovery of port settings.", serialPortName);
        if (halfTimeTimer != null) {
            halfTimeTimer.cancel(true);
            halfTimeTimer = null;
        }
        if (endTimeTimer != null) {
            endTimeTimer.cancel(true);
            endTimeTimer = null;
        }
        this.state = state;
    }

    /**
     * Method to (re)start the switching baudrate timer.
     */
    private void restartHalfTimer() {
        if (halfTimeTimer != null) {
            halfTimeTimer.cancel(true);
        }
        halfTimeTimer = scheduler.schedule(this::switchBaudrate, baudrateSwitchTimeoutSeconds, TimeUnit.SECONDS);
    }

}
