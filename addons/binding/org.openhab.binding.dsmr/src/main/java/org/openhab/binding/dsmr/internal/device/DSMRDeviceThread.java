/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dsmr.internal.device.connector.DSMRPortErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hilbrand Bouwkamp - Initial contribution
 *
 */
@NonNullByDefault
public class DSMRDeviceThread implements DSMRDevice, Runnable {
    private final Logger logger = LoggerFactory.getLogger(DSMRDeviceThread.class);
    private final Semaphore semaphore = new Semaphore(0);
    private final DSMRDevice device;
    private final DSMRPortEventListener portEventListener;

    private boolean running = true;

    public DSMRDeviceThread(DSMRDevice device, DSMRPortEventListener portEventListener) {
        this.device = device;
        this.portEventListener = portEventListener;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void restart() {
        releaseSemaphore();
    }

    @Override
    public void stop() {
        running = false;
        releaseSemaphore();
    }

    @Override
    public void run() {
        try {
            device.start();
            while (running && !Thread.interrupted()) {
                semaphore.acquire();
                // Just drain all other permits to make sure it's not called twice
                semaphore.drainPermits();
                if (running) {
                    logger.trace("Restarting device");
                    device.restart();
                }
            }
            logger.trace("Device shutdown");
        } catch (RuntimeException e) {
            logger.warn("DSMRDeviceThread stopped with a RuntimeException", e);
            portEventListener.handlePortErrorEvent(DSMRPortErrorEvent.READ_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            device.stop();
        }

    }

    private void releaseSemaphore() {
        synchronized (semaphore) {
            if (semaphore.availablePermits() == 0) {
                semaphore.release();
            }
        }
    }
}
