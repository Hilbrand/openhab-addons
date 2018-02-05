/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 *
 */
@NonNullByDefault
public class DSMRTelegramQueue implements Runnable {

    private static final int _100 = 100;

    private final Logger logger = LoggerFactory.getLogger(DSMRTelegramQueue.class);
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(_100);
    private final P1TelegramParser parser;

    private boolean running;

    public DSMRTelegramQueue(P1TelegramParser parser) {
        this.parser = parser;
    }

    // @Override
    public void push(byte[] data) {
        parser.parseData(data, 0, data.length);
    }

    public void push2(byte[] data) {
        synchronized (queue) {
            try {
                if (queue.remainingCapacity() == 0) {
                    queue.poll();
                }
                queue.put(data);
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
    }

    public void dispose() {
        running = false;
    }

    @Override
    public void run() {
        parser.reset();
        running = true;
        logger.trace("Starting parser");
        try {
            while (running) {
                byte[] take = queue.take();
                parser.parseData(take, 0, take.length);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            logger.debug("run finished.");
        }
    }

}
