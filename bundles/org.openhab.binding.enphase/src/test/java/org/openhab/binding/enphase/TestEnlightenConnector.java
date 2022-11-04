/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.enphase;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;
import org.openhab.binding.enphase.internal.EnvoyConnectionException;
import org.openhab.binding.enphase.internal.handler.EntrezConnector;

/**
 *
 * Methods to make API calls to to the Enlighten Cloud API.
 *
 * @author Joe Inkenbrandt - Initial contribution
 *
 */
public class TestEnlightenConnector {

    private HttpClient getClient() {
        HttpClient httpClient = new HttpClient(new SslContextFactory.Client(true));
        try {
            httpClient.start();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not start HttpClient.", ex);
        }

        return httpClient;
    }

    @Test
    public void testEnlightenConnector() throws EnvoyConnectionException {

        EntrezConnector entrez = new EntrezConnector(getClient());
        // EnvoyConnector envoy = new EnvoyConnector
        // ec.setConfiguration(new EnvoyConfiguration());

        String jwt = entrez.getJwt("joe@inkenbrandt.com", "Wsto!961", "2339377", "122103062465");

        String[] parts = jwt.split("\\.", 0);

        for (String part : parts) {
            byte[] bytes = Base64.getUrlDecoder().decode(part);
            String decodedString = new String(bytes, StandardCharsets.UTF_8);

            System.out.println("Decoded: " + decodedString);
        }

        // System.out.println(ec.getSystems());

    }

}