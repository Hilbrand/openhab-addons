/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication.Result;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.DigestAuthentication;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.enphase.internal.dto.EnvoyEnergyDTO;
import org.openhab.binding.enphase.internal.dto.EnvoyErrorDTO;
import org.openhab.binding.enphase.internal.dto.InverterDTO;
import org.openhab.binding.enphase.internal.dto.ProductionJsonDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Methods to make API calls to the Envoy gateway.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
class EnvoyConnector {

    private static final String HTTP = "http://";
    private static final String PRODUCTION_JSON_URL = "/production.json";
    private static final String PRODUCTION_URL = "/api/v1/production";
    private static final String CONSUMPTION_URL = "/api/v1/consumption";
    private static final String INVERTERS_URL = PRODUCTION_URL + "/inverters";
    private static final String REALM = "enphaseenergy.com";
    private static final long CONNECT_TIMEOUT_SECONDS = 3;
    private static final TypeToken<List<InverterDTO>> INVERTER_TYPE_TOKEN_LIST = new TypeToken<List<InverterDTO>>() {
    };

    private final Logger logger = LoggerFactory.getLogger(EnvoyConnector.class);
    private final Gson gson = new GsonBuilder().create();
    private final HttpClient httpClient;
    private String hostname = "";
    private @Nullable DigestAuthentication inverterAuthn;
    private @Nullable URI invetersURI;

    public EnvoyConnector(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Sets the Envoy connection configuration.
     *
     * @param configuration the configuration to set
     */
    public void setConfiguration(final EnvoyConfiguration configuration) {
        hostname = configuration.hostname;
        if (hostname.isEmpty()) {
            return;
        }
        final String password = configuration.password.isEmpty()
                ? EnphaseBindingConstants.defaultPassword(configuration.serialnumber)
                : configuration.password;
        final String username = configuration.username.isEmpty() ? EnvoyConfiguration.DEFAULT_USERNAME
                : configuration.username;
        final AuthenticationStore store = httpClient.getAuthenticationStore();

        if (inverterAuthn != null) {
            store.removeAuthentication(inverterAuthn);
        }
        invetersURI = URI.create(HTTP + hostname + INVERTERS_URL);
        inverterAuthn = new DigestAuthentication(invetersURI, REALM, username, password);
        store.addAuthentication(inverterAuthn);
    }

    /**
     * @return Returns the production data from the Envoy gateway.
     */
    public synchronized EnvoyEnergyDTO getProduction() {
        return getData(PRODUCTION_URL, null, this::jsonToEnvoyEnergyDTO);
    }

    /**
     * @return Returns the consumption data from the Envoy gateway.
     */
    public @Nullable EnvoyEnergyDTO getConsumption() {
        return getData(CONSUMPTION_URL, null, this::jsonToEnvoyEnergyDTO);
    }

    private EnvoyEnergyDTO jsonToEnvoyEnergyDTO(final String json) {
        return gson.fromJson(json, EnvoyEnergyDTO.class);
    }

    /**
     * @return Returns the production/consumption data from the Envoy gateway.
     */
    public synchronized ProductionJsonDTO getProductionJson() {
        return getData(PRODUCTION_JSON_URL, null, this::jsonToEnvoyProductionJson);
    }

    private ProductionJsonDTO jsonToEnvoyProductionJson(final String json) {
        return gson.fromJson(json, ProductionJsonDTO.class);
    }

    /**
     * @return Returns the production data for the inverters.
     */
    public List<InverterDTO> getInverters() {
        synchronized (this) {
            final AuthenticationStore store = httpClient.getAuthenticationStore();
            final Result invertersResult = store.findAuthenticationResult(invetersURI);

            if (invertersResult != null) {
                store.removeAuthenticationResult(invertersResult);
            }
        }
        return getData(INVERTERS_URL, inverterAuthn, this::jsonToEnvoyInverters);
    }

    private List<InverterDTO> jsonToEnvoyInverters(final String json) {
        return gson.fromJson(json, INVERTER_TYPE_TOKEN_LIST.getType());
    }

    private synchronized <T> T getData(final String urlPath, final @Nullable DigestAuthentication authn,
            final Function<String, T> jsonConverter) {
        try {
            if (hostname == null || hostname.isEmpty()) {
                throw new EnvoyNoHostnameException("No host name/ip address known (yet)");
            }
            final URI uri = URI.create(HTTP + hostname + urlPath);
            final Request request = httpClient.newRequest(uri).timeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            final ContentResponse response = request.send();
            final String content = response.getContentAsString();

            try {
                if (response.getStatus() == HttpStatus.OK_200) {
                    return jsonConverter.apply(content);
                } else {
                    final EnvoyErrorDTO error = gson.fromJson(content, EnvoyErrorDTO.class);
                    logger.trace("Envoy returned an error: {}", error);
                    throw new EnvoyConnectionException(error.info);
                }
            } catch (final JsonSyntaxException e) {
                logger.trace("Error parsing json: {}", content, e);
                throw new EnvoyConnectionException(e.getMessage());
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnvoyConnectionException("Interrupted");
        } catch (TimeoutException | ExecutionException e) {
            throw new EnvoyConnectionException("Connection problem", e);
        }
    }
}
