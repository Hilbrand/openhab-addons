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
package org.openhab.binding.enphase.internal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.enphase.internal.dto.EntrezJwtDTO.EntrezJwtBodyDTO;
import org.openhab.binding.enphase.internal.dto.EntrezJwtDTO.EntrezJwtHeaderDTO;
import org.openhab.binding.enphase.internal.handler.EntrezConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Class to process and retrieve JWTs from Entrez
 *
 * @author Joe Inkenbrandt - Initial contribution
 */
public class EntrezJwt {

    private final Logger logger = LoggerFactory.getLogger(EntrezJwt.class);
    private final Gson gson = new GsonBuilder().create();

    private EntrezJwtHeaderDTO header = null;
    private EntrezJwtBodyDTO body = null;

    private String jwt = "";

    public EntrezJwt(String jwt) {
        this.jwt = jwt;
        if (!isEmpty()) {
            processJwt();
        }
    }

    public String getJwt() {
        return this.jwt;
    }

    public boolean isEmpty() {
        return jwt.isEmpty();
    }

    public boolean isValid() {
        return !(header == null) && !(body == null);
    }

    public boolean isExpired() {
        if (body == null) {
            return true;
        }

        Date expDate = new Date(body.getExp() * 1000);
        Date now = new Date();

        if (now.before(expDate)) {
            return false;
        }

        jwt = "";
        body = null;
        header = null;

        return true;
    }

    public boolean retrieveJwt(String login, String password, String siteId, String serialNumber) {
        EntrezConnector entrez = new EntrezConnector(getClient());

        try {
            jwt = entrez.getJwt(login, password, siteId, serialNumber);
        } catch (EnvoyConnectionException e) {
            // TODO: Add intelligent error handling
            e.printStackTrace();
            return false;
        }

        processJwt();

        return true;
    }

    private void processJwt() {
        String[] parts = jwt.split("\\.", 0);

        try {
            header = gson.fromJson(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8),
                    EntrezJwtHeaderDTO.class);

            body = gson.fromJson(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
                    EntrezJwtBodyDTO.class);

        } catch (JsonSyntaxException | IllegalArgumentException e) {
            logger.error("Invalid jwt");
            header = null;
            body = null;
        }
    }

    private HttpClient getClient() {
        HttpClient httpClient = new HttpClient(new SslContextFactory.Client(true));
        try {
            httpClient.start();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not start HttpClient.", ex);
        }

        return httpClient;
    }

}
