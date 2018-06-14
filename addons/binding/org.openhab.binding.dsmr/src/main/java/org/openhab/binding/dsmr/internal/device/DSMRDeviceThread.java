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

    private boolean shutdown;

    public DSMRDeviceThread(DSMRDevice device) {
        this.device = device;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void restart() {
        semaphore.release();
    }

    @Override
    public void stop() {
        shutdown = true;
        semaphore.release();
    }

    @Override
    public void run() {
        try {
            device.start();
            while (!shutdown) {
                semaphore.acquire();
                // Just drain all other permits to make sure it's not called twice
                semaphore.drainPermits();
                if (!shutdown) {
                    logger.trace("Restarting device");
                    device.restart();
                }
                logger.trace("Device shutdown");
            }
        } catch (RuntimeException e) {
            logger.warn("DSMRDeviceThread stopped with a RuntimeException", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            device.stop();
        }

    }
}
