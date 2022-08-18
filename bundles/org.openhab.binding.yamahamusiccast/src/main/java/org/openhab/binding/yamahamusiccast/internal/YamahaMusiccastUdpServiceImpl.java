/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.yamahamusiccast.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.yamahamusiccast.internal.dto.UdpMessage;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link YamahaMusiccastUdpServiceImpl} is responsible for dispatching UDP events to linked Things.
 *
 * @author Lennert Coopman - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored Bridge into OSGI service and made code more robust
 */
@NonNullByDefault
@Component(configurationPid = "binding.yamahamusiccast.udp.service", service = YamahaMusiccastUdpService.class)
class YamahaMusiccastUdpServiceImpl implements YamahaMusiccastUdpService {
    private static final int UDP_PORT = 41100;
    private static final int SOCKET_TIMEOUT_MILLISECONDS = 3_000;
    private static final long RESTART_TIMEOUT_MILLISECONDS = 5_000;
    private static final int BUFFER_SIZE = 5120;
    private static final String SERVICE_THREAD_NAME = "OH-binding-YamahaMusiccast-UDP-Service";

    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(YamahaMusiccastUdpServiceImpl.class);
    private final List<YamahaMusiccastUdpMessageHandler> handlers = new ArrayList<>();

    private @Nullable Thread thread;
    private boolean running;

    @Activate
    synchronized void activate() {
        final Thread thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.setName(SERVICE_THREAD_NAME);
        thread.start();
        running = true;
        this.thread = thread;
    }

    @Deactivate
    synchronized void deactivate() {
        final Thread thread = this.thread;

        if (thread != null) {
            running = false;
            this.thread = null;
        }
    }

    @Override
    public void addHandler(final YamahaMusiccastUdpMessageHandler handler) {
        handlers.add(handler);
    }

    @Override
    public void removeHandler(final YamahaMusiccastUdpMessageHandler handler) {
        handlers.remove(handler);
    }

    private void run() {
        while (running) {
            try {
                connect().ifPresent(this::receivePackets);
            } catch (final RuntimeException e) {
                logger.debug("UDP Serive received an unexpected exception: ", e);
            }
            try {
                if (running) {
                    Thread.sleep(RESTART_TIMEOUT_MILLISECONDS);
                }
            } catch (final InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
        logger.trace("UDP Listener exiting");
    }

    private Optional<DatagramSocket> connect() {
        try {
            final DatagramSocket socket = new DatagramSocket(null);
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
            socket.setReuseAddress(true);
            final InetSocketAddress address = new InetSocketAddress(UDP_PORT);
            socket.bind(address);
            logger.trace("UDP Listener got socket on port {} with timeout {}", UDP_PORT, SOCKET_TIMEOUT_MILLISECONDS);
            return Optional.of(socket);
        } catch (final SocketException e) {
            logger.trace("UDP Listener got SocketException: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private void receivePackets(final DatagramSocket socket) {
        final DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);

        while (running && !socket.isClosed()) {
            try {
                socket.receive(packet);
                final String received = new String(packet.getData(), 0, packet.getLength());
                final String trackingID = UUID.randomUUID().toString().replace("-", "").substring(0, 32);

                logger.trace("Received packet: {} (Tracking: {})", received, trackingID);
                handleUDPEvent(received, trackingID);
            } catch (final SocketTimeoutException e) {
                // Nothing to do on socket timeout
            } catch (final IOException e) {
                logger.trace("UDP Listener got IOException waiting for datagram: {}", e.getMessage());
                socket.close();
            }
        }
        socket.close();
    }

    void handleUDPEvent(final String json, final String trackingID) {
        final UdpMessage udpMessage = gson.fromJson(json, UdpMessage.class);

        if (udpMessage == null) {
            logger.trace("Could not parse UDP json message: {}", json);
        } else {
            for (final YamahaMusiccastUdpMessageHandler handler : handlers) {
                if (handler.isDeviceId(udpMessage.getDeviceId())) {
                    handler.processUDPEvent(udpMessage, trackingID);
                }
            }
        }
    }
}
