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
package org.openhab.binding.enphase.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.enphase.internal.EnvoyConfiguration;
import org.openhab.binding.enphase.internal.exception.EnvoyConnectionException;
import org.openhab.binding.enphase.internal.exception.EnvoyNoHostnameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps around the specific Envoy connector and provides methods to determine which connector to use.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class EnvoyConnectorWrapper {

    private final Logger logger = LoggerFactory.getLogger(EnvoyConnectorWrapper.class);
    private final HttpClient httpClient;
    private @Nullable EnvoyConnector connector;
    private @Nullable String version;

    public EnvoyConnectorWrapper(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     *
     * @param version
     */
    public void setVersion(final @Nullable String version) {
        this.version = version;
    }

    /**
     *
     * @param configuration
     * @return
     * @throws EnvoyNoHostnameException
     * @throws EnvoyConnectionException
     */
    public synchronized String setConnector(final EnvoyConfiguration configuration)
            throws EnvoyNoHostnameException, EnvoyConnectionException {
        final EnvoyConnector connector = determineConnector(configuration.hostname);

        this.connector = connector;
        return connector.setConfiguration(configuration);
    }

    /**
     *
     * @return
     */
    public boolean hasConnection() {
        return connector != null;
    }

    /**
     *
     * @return
     * @throws EnvoyConnectionException
     */
    public EnvoyConnector getConnector() throws EnvoyConnectionException {
        final EnvoyConnector connector = this.connector;

        if (connector == null) {
            throw new EnvoyConnectionException("");
        }
        return connector;
    }

    private EnvoyConnector determineConnector(final String hostname)
            throws EnvoyNoHostnameException, EnvoyConnectionException {
        final EnvoyConnector connectorByVersion = determineConnectorOnVersion();

        if (connectorByVersion != null) {
            return connectorByVersion;
        }

        if (hostname.isBlank()) {
            throw new EnvoyNoHostnameException("");
        }
        final EnvoyConnector envoyConnector = new EnvoyConnector(httpClient);

        if (envoyConnector.checkConnection(hostname)) {
            logger.info("Connect to Envoy determined by getting a reply from the Envoy using standard connector");
            return envoyConnector;
        } else {
            final EnvoyEntrezConnector envoyEntrezConnector = new EnvoyEntrezConnector(httpClient);

            if (envoyConnector.checkConnection(hostname)) {
                logger.info("Connect to Envoy determined by getting a reply from the Envoy using entrez connector");
                return envoyEntrezConnector;
            }
        }
        throw new EnvoyConnectionException("");
    }

    private @Nullable EnvoyConnector determineConnectorOnVersion() {
        final String version = this.version;

        if (version == null) {
            return null;
        }
        logger.debug("Envoy version: {}", version);
        final int marjorVersionIndex = version.indexOf('.');

        if (marjorVersionIndex < 0) {
            return null;
        }
        final int majorVersionNumber;

        try {
            majorVersionNumber = Integer.parseInt(version.substring(0, marjorVersionIndex));
        } catch (final NumberFormatException e) {
            logger.trace("Could not parse major version number in {}, error message: {}", version, e.getMessage());
            return null;
        }
        if (majorVersionNumber < 7) {
            logger.info("Connect to Envoy based on version number {} using standard connector", version);
            return new EnvoyConnector(httpClient);
        } else {
            logger.info("Connect to Envoy based on version number {} using entrez connector", version);
            return new EnvoyEntrezConnector(httpClient);
        }
    }
}
