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
package org.openhab.binding.smappee.internal.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthFactory;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.binding.smappee.internal.exception.CommunicationException;
import org.openhab.binding.smappee.internal.exception.InvalidConfigurationException;
import org.openhab.binding.smappee.internal.model.SmappeeApplianceEvent;
import org.openhab.binding.smappee.internal.model.SmappeeDeviceReading;
import org.openhab.binding.smappee.internal.model.SmappeeDeviceReadingConsumption;
import org.openhab.binding.smappee.internal.model.SmappeeSensorConsumption;
import org.openhab.binding.smappee.internal.model.SmappeeSensorConsumptionRecord;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocation;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationInfo;
import org.openhab.binding.smappee.internal.model.SmappeeServiceLocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link SamppeeService} handles the connection to the Smappee API
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class SmappeeService {

    private static final int MAX_RETRIES = 1;
    private static final String API_ROOT_URL = "https://app1pub.smappee.net/dev/v1/";
    private static final String OAUTH2_URL = API_ROOT_URL + "oauth2/token";
    private static final String DEVICE_READINGS_URL = String
            .format("servicelocation/%s/consumption?aggregation=1&from=%d&to=%d");
    private static final String AGGREGATION_PART = "aggregation=1&from=%d&to=%d";

    private final Logger logger = LoggerFactory.getLogger(SmappeeService.class);
    private final Gson gson = new Gson();

    private final SmappeeConfigurationParameters config;
    private final HttpClient httpClient;

    private @Nullable String serviceLocationId;
    private boolean initialized;
    private OAuthClientService oAuthService;

    public SmappeeService(final String thingUID, final SmappeeConfigurationParameters config,
            final HttpClient httpClient, final OAuthFactory oautFactory) {
        this.config = config;
        this.httpClient = httpClient;
        this.initialized = false;
        oAuthService = oautFactory.createOAuthClientService(thingUID, OAUTH2_URL, OAUTH2_URL, config.clientId,
                config.clientSecret, null, false);
    }

    public SmappeeConfigurationParameters getConfig() {
        return config;
    }

    /**
     * Gets the smappee energy consumption readings.
     *
     * @return the device readings
     */
    public @Nullable SmappeeDeviceReading getDeviceReadings() {
        int currentTry = 0;
        do {
            try {
                final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneId.of("UTC")).minusMinutes(1);
                final OffsetDateTime nowUtcMinus20Min = OffsetDateTime.now(ZoneId.of("UTC")).minusMinutes(20);

                final String nowUtcMillis = String.valueOf(nowUtc.toInstant().toEpochMilli());
                final String nowUtcMinus20MinMillis = String.valueOf(nowUtcMinus20Min.toInstant().toEpochMilli());

                // sample API method to call :
                // https://app1pub.smappee.net/dev/v1/servicelocation/123/consumption?aggregation=1&from=1388534400000&to=1391212800000

                final String responseReadings = getData("servicelocation/" + this.serviceLocationId
                        + "/consumption?aggregation=1&from=" + nowUtcMinus20MinMillis + "&to=" + nowUtcMillis);

                if (responseReadings.isEmpty()) {
                    return null;
                }

                final SmappeeDeviceReading readings = gson.fromJson(responseReadings, SmappeeDeviceReading.class);

                // Sum of 5 minutes in Wh so average power is 60/5 x

                for (final SmappeeDeviceReadingConsumption consumption : readings.consumptions) {
                    consumption.consumption = consumption.consumption * 12;
                    consumption.solar = consumption.solar * 12;
                }

                logger.debug("smappee'{}' read", this.serviceLocationId);

                return readings;

            } catch (final CommunicationException se) {
                logger.debug("failed to read smappee '{}'", se.getMessage());
            } catch (final JsonSyntaxException pe) {
                logger.warn("failed to read response from smappee : {}", pe.getMessage());
            }
        } while (currentTry++ < MAX_RETRIES);

        return null;
    }

    public @Nullable SmappeeServiceLocationInfo getServiceLocationInfo() {
        int currentTry = 0;
        do {
            try {
                // sample API method to call :
                // https://app1pub.smappee.net/dev/v1/servicelocation/123/info
                final String responseReadings = getData("servicelocation/" + this.serviceLocationId + "/info");

                if (responseReadings.isEmpty()) {
                    return null;
                }

                final SmappeeServiceLocationInfo readings = gson.fromJson(responseReadings,
                        SmappeeServiceLocationInfo.class);

                logger.debug("servicelocationinfo '{}' read", this.serviceLocationId);

                return readings;

            } catch (final CommunicationException se) {
                logger.debug("failed to read servicelocationinfo '{}'", se.getMessage());
            } catch (final JsonSyntaxException pe) {
                logger.warn("failed to read response from smappee : {}", pe.getMessage());
            }
        } while (currentTry++ < MAX_RETRIES);

        return null;
    }

    /**
     * Gets the smappee energy consumption readings for a specific appliance.
     *
     * @return the device readings
     */
    public @Nullable SmappeeApplianceEvent getLatestApplianceReading(final String applianceId) {
        int currentTry = 0;
        do {
            try {
                final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneId.of("UTC")).minusMinutes(1);
                final OffsetDateTime nowUtcMinus1Year = OffsetDateTime.now(ZoneId.of("UTC")).minusYears(1);

                final String nowUtcMillis = String.valueOf(nowUtc.toInstant().toEpochMilli());
                final String nowUtcMinus1YearMillis = String.valueOf(nowUtcMinus1Year.toInstant().toEpochMilli());

                // sample API method to call :
                // https://app1pub.smappee.net/dev/v1/servicelocation/123123/events?
                // applianceId=1&applianceId=2&maxNumber=100&from=1388534400000&to=1391212800000

                final String responseReadings = getData(
                        "servicelocation/" + this.serviceLocationId + "/events?applianceId=" + applianceId
                                + "&maxNumber=1&from=" + nowUtcMinus1YearMillis + "&to=" + nowUtcMillis);

                if (responseReadings.isEmpty()) {
                    return null;
                }

                final SmappeeApplianceEvent[] readings = gson.fromJson(responseReadings, SmappeeApplianceEvent[].class);

                logger.debug("smappee '{}' - appliance '{}' read", this.serviceLocationId, applianceId);

                if (readings.length == 1) {
                    return readings[0];
                }
                return null;

            } catch (final CommunicationException se) {
                logger.debug("failed to read smappee '{}' - appliance '{}' : {}", this.serviceLocationId, applianceId,
                        se.getMessage());
            } catch (final JsonSyntaxException pe) {
                logger.warn("failed to read response from smappee '{}' - appliance '{}' : {}", this.serviceLocationId,
                        applianceId, pe.getMessage());
            }
        } while (currentTry++ < MAX_RETRIES);

        return null;
    }

    /**
     * Gets the smappee energy consumption readings for a specific sensor.
     * Nullable
     *
     * @return the device readings
     */
    public @Nullable SmappeeSensorConsumptionRecord getLatestSensorConsumption(final String sensorId) {
        int currentTry = 0;
        do {
            try {
                final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
                final OffsetDateTime nowUtcMinus1Year = OffsetDateTime.now(ZoneOffset.UTC).minusYears(1);

                final String nowUtcMillis = String.valueOf(nowUtc.toInstant().toEpochMilli());
                final String nowUtcMinus1YearMillis = String.valueOf(nowUtcMinus1Year.toInstant().toEpochMilli());

                // sample API method to call :
                // https://app1pub.smappee.net/dev/v1/servicelocation/1/sensor/4/consumption?
                // from=1457597400000&to=1458666049000&aggregation=1
                final String responseReadings = getData(
                        "servicelocation/" + this.serviceLocationId + "/sensor" + sensorId + "/consumption?"
                                + "aggregation=1&from=" + nowUtcMinus1YearMillis + "&to=" + nowUtcMillis);

                if (responseReadings.isEmpty()) {
                    return null;
                }

                final SmappeeSensorConsumption readings = gson.fromJson(responseReadings,
                        SmappeeSensorConsumption.class);

                logger.debug("smappee '{}' - sensor '{}' read", this.serviceLocationId, sensorId);

                if (readings.records != null && readings.records.length > 0) {
                    return readings.records[0];
                }
                return null;

            } catch (final CommunicationException ce) {
                logger.debug("failed to read smappee '{}' - sensorId '{}' : {}, Retry ({}/{})", this.serviceLocationId,
                        sensorId, ce.getMessage(), currentTry + 1, MAX_RETRIES);
            } catch (final JsonSyntaxException pe) {
                logger.warn("failed to read response from smappee '{}' - sensorId '{}' : {}", this.serviceLocationId,
                        sensorId, pe.getMessage());
            }
        } while (currentTry++ < MAX_RETRIES);

        return null;
    }

    /**
     * Put a plug at a specific location on or off.
     *
     * @param serviceLocationID the service location ID
     * @param actuatorID the actuator ID
     * @param turnOn turn on / off
     */
    public void putPlugOnOff(final String actuatorID, final boolean turnOn) {
        // duration is not specified, so smappee will turn on/off the plug or an
        // undetermined period of time

        // sample API method to call :
        // https://app1pub.smappee.net/dev/v1/servicelocation/[SERVICELOCATIONID]/actuator/[ACTUATORID]/on

        int currentTry = 0;
        do {
            try {
                postData(String.format("servicelocation/%s/actuator/%s/%s", this.serviceLocationId, actuatorID,
                        turnOn ? "on" : "off"), "{}");
                return;
            } catch (final CommunicationException se) {
                logger.warn("failed to set smappee plug '{}' - sensorId '{}' : {}, Retry ({}/{})",
                        this.serviceLocationId, actuatorID, se.getMessage(), currentTry + 1, MAX_RETRIES);
            }
        } while (currentTry++ < MAX_RETRIES);
    }

    public void initialize() throws CommunicationException, InvalidConfigurationException {
        // get service locations
        final String response = getData("servicelocation");

        try {
            final SmappeeServiceLocationResponse smappeeServiceLocationResponse = gson.fromJson(response,
                    SmappeeServiceLocationResponse.class);

            for (final SmappeeServiceLocation smappeeServiceLocation : smappeeServiceLocationResponse.serviceLocations) {
                if (smappeeServiceLocation.name.equals(config.serviceLocationName)) {
                    this.serviceLocationId = Integer.toString(smappeeServiceLocation.serviceLocationId);
                    initialized = true;
                    return;
                }
            }

            throw new InvalidConfigurationException("Could not find a valid servicelotion for "
                    + config.serviceLocationName + ", check binding configuration");
        } catch (final JsonSyntaxException pe) {
            throw new CommunicationException("Failed to parse servicelocation response", pe);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private String getData(final String request) throws CommunicationException {
        final String url = API_ROOT_URL + request;

        return makeRequest(url, r -> {
        });
    }

    private void postData(final String request, final String jsonContent) throws CommunicationException {
        makeRequest(request, r -> {
            r.method(HttpMethod.POST);
            r.content(new StringContentProvider(jsonContent));
        });
    }

    private String makeRequest(final String request, final Consumer<Request> requestAdder)
            throws CommunicationException {
        final String url = API_ROOT_URL + request;

        final Request requestMethod = httpClient.newRequest(url);
        requestMethod.agent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; .NET CLR 1.0.3705;)");
        requestMethod.accept("application/json");

        try {
            final AccessTokenResponse accessTokenResponse = oAuthService.getAccessTokenResponse();

            if (accessTokenResponse == null) {
                throw new CommunicationException("Not authenticated. No access token");
            }
            requestMethod.header(HttpHeader.AUTHORIZATION, "Bearer " + accessTokenResponse.getAccessToken());
            requestAdder.accept(requestMethod);
            final ContentResponse response = requestMethod.send();

            if (response.getStatus() != HttpStatus.OK_200) {
                throw new CommunicationException("Get data method failed : " + response.getReason());
            }

            return response.getContentAsString();
        } catch (final InterruptedException e) {
            throw new CommunicationException("Request aborted", e);
        } catch (final TimeoutException e) {
            throw new CommunicationException("Timeout error", e);
        } catch (final ExecutionException e) {
            throw new CommunicationException("Communication error", e.getCause());
        } catch (final OAuthException e) {
            throw new CommunicationException("OAuthException", e.getCause());
        } catch (final IOException e) {
            throw new CommunicationException("IO Exception", e.getCause());
        } catch (final OAuthResponseException e) {
            throw new CommunicationException("OAuthResponseException", e.getCause());
        }
    }
}
