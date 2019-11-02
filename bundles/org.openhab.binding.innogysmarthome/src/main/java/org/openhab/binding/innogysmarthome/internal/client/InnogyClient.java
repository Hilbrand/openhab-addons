/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.innogysmarthome.internal.client;

import static org.openhab.binding.innogysmarthome.internal.client.Constants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.binding.innogysmarthome.internal.client.entity.StatusResponse;
import org.openhab.binding.innogysmarthome.internal.client.entity.action.Action;
import org.openhab.binding.innogysmarthome.internal.client.entity.action.SetStateAction;
import org.openhab.binding.innogysmarthome.internal.client.entity.capability.Capability;
import org.openhab.binding.innogysmarthome.internal.client.entity.capability.CapabilityState;
import org.openhab.binding.innogysmarthome.internal.client.entity.device.Device;
import org.openhab.binding.innogysmarthome.internal.client.entity.device.DeviceState;
import org.openhab.binding.innogysmarthome.internal.client.entity.device.Gateway;
import org.openhab.binding.innogysmarthome.internal.client.entity.device.State;
import org.openhab.binding.innogysmarthome.internal.client.entity.error.ErrorResponse;
import org.openhab.binding.innogysmarthome.internal.client.entity.link.Link;
import org.openhab.binding.innogysmarthome.internal.client.entity.location.Location;
import org.openhab.binding.innogysmarthome.internal.client.entity.message.Message;
import org.openhab.binding.innogysmarthome.internal.client.exception.ApiException;
import org.openhab.binding.innogysmarthome.internal.client.exception.AuthenticationException;
import org.openhab.binding.innogysmarthome.internal.client.exception.ControllerOfflineException;
import org.openhab.binding.innogysmarthome.internal.client.exception.InvalidActionTriggeredException;
import org.openhab.binding.innogysmarthome.internal.client.exception.RemoteAccessNotAllowedException;
import org.openhab.binding.innogysmarthome.internal.client.exception.ServiceUnavailableException;
import org.openhab.binding.innogysmarthome.internal.client.exception.SessionExistsException;
import org.openhab.binding.innogysmarthome.internal.client.exception.SessionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The main client that handles the communication with the innogy SmartHome API service.
 *
 * @author Oliver Kuhl - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored to use openHAB http and oauth2 libraries
 *
 */
@NonNullByDefault
public class InnogyClient {
    private static final String BEARER = "Bearer ";
    private static final String CONTENT_TYPE = "application/json";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final int HTTP_CLIENT_TIMEOUT_SECONDS = 10;

    private final Logger logger = LoggerFactory.getLogger(InnogyClient.class);

    /**
     * date format as used in json in API. Example: 2016-07-11T10:55:52.3863424Z
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();

    private final OAuthClientService oAuthService;
    private final HttpClient httpClient;
    private @Nullable Gateway bridgeDetails;
    private String configVersion = "";
    private long apiCallCounter;

    public InnogyClient(OAuthClientService oAuthService, HttpClient httpClient) {
        this.oAuthService = oAuthService;
        this.httpClient = httpClient;
    }

    /**
     * @return the bridgeInfo
     */
    public @Nullable Gateway getBridgeDetails() {
        return bridgeDetails;
    }

    /**
     * Gets the status
     *
     * As the API returns the details of the SmartHome controller (SHC), the data is saved in {@link #bridgeDetails} and
     * the {@link #configVersion} is set.
     *
     * @throws SessionExistsException thrown, if a session already exists
     * @throws IOException
     * @throws ApiException
     */
    public void refreshStatus() throws IOException, ApiException, AuthenticationException {
        logger.debug("Get innogy SmartHome status...");
        final ContentResponse response = executeGet(API_URL_STATUS);

        final StatusResponse status = gson.fromJson(response.getContentAsString(), StatusResponse.class);
        bridgeDetails = status.gateway;
        configVersion = bridgeDetails.getConfigVersion();

        logger.debug("innogy SmartHome Status loaded. Configuration version is {}.", configVersion);
    }

    /**
     * Executes a HTTP GET request with default headers.
     *
     * @param url
     * @return
     * @throws IOException
     * @throws AuthenticationException
     * @throws ApiException
     */
    private ContentResponse executeGet(String url) throws IOException, AuthenticationException, ApiException {
        apiCallCounter++;
        return request(httpClient.newRequest(url).method(HttpMethod.GET));
    }

    /**
     * Executes a HTTP POST request with the given {@link Action} as content.
     *
     * @param url
     * @param action
     * @return
     * @throws IOException
     * @throws AuthenticationException
     * @throws ApiException
     */
    private ContentResponse executePost(String url, Action action)
            throws IOException, AuthenticationException, ApiException {
        apiCallCounter++;
        return executePost(url, gson.toJson(action));
    }

    /**
     * Executes a HTTP POST request with JSON formatted content.
     *
     * @param url
     * @param content
     * @return
     * @throws IOException
     * @throws AuthenticationException
     * @throws ApiException
     */
    private ContentResponse executePost(String url, String content)
            throws IOException, AuthenticationException, ApiException {
        apiCallCounter++;
        return request(httpClient.newRequest(url).method(HttpMethod.POST)
                .content(new StringContentProvider(content), CONTENT_TYPE).accept("application/json"));
    }

    private ContentResponse request(Request request) throws IOException, AuthenticationException, ApiException {
        final ContentResponse response;
        try {
            final AccessTokenResponse accessTokenResponse = getAccessTokenResponse();

            response = request.header("Accept", CONTENT_TYPE)
                    .header(AUTHORIZATION_HEADER, BEARER + accessTokenResponse.getAccessToken())
                    .timeout(HTTP_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new IOException(e);
        }
        handleResponseErrors(response);
        return response;
    }

    public AccessTokenResponse getAccessTokenResponse() throws AuthenticationException, IOException {
        final AccessTokenResponse accessTokenResponse;
        try {
            accessTokenResponse = oAuthService.getAccessTokenResponse();
        } catch (OAuthException | OAuthResponseException e) {
            throw new AuthenticationException("Error fetching access token: " + e.getMessage());
        }
        if (accessTokenResponse == null || StringUtils.isBlank(accessTokenResponse.getAccessToken())) {
            throw new AuthenticationException("No innogy accesstoken. Is this thing authorized?");
        }
        return accessTokenResponse;
    }

    /**
     * Handles errors from the {@link ContentResponse} and throws the following errors:
     *
     * @param response
     * @throws SessionExistsException
     * @throws SessionNotFoundException
     * @throws ControllerOfflineException thrown, if the innogy SmartHome controller (SHC) is offline.
     * @throws IOException
     * @throws ApiException
     * @throws AuthenticationException
     */
    private void handleResponseErrors(ContentResponse response)
            throws IOException, ApiException, AuthenticationException {
        String content = "";

        switch (response.getStatus()) {
            case HttpStatus.OK_200:
                logger.debug("[{}] Statuscode is OK.", apiCallCounter);
                return;
            case HttpStatus.SERVICE_UNAVAILABLE_503:
                logger.debug("innogy service is unavailabe (503).");
                throw new ServiceUnavailableException("innogy service is unavailabe (503).");
            default:
                logger.debug("[{}] Statuscode is NOT OK: {}", apiCallCounter, response.getStatus());
                try {
                    content = response.getContentAsString();
                    logger.trace("Response error content: {}", content);
                    final ErrorResponse error = gson.fromJson(content, ErrorResponse.class);

                    if (error == null) {
                        logger.debug("Error without JSON message, code: {} / message: {}", response.getStatus(),
                                response.getReason());
                        throw new ApiException("Error code: " + response.getStatus());
                    }

                    switch (error.getCode()) {
                        case ErrorResponse.ERR_SESSION_EXISTS:
                            logger.debug("Session exists: {}", error.toString());
                            throw new SessionExistsException(error.getDescription());
                        case ErrorResponse.ERR_SESSION_NOT_FOUND:
                            logger.debug("Session not found: {}", error.toString());
                            throw new SessionNotFoundException(error.getDescription());
                        case ErrorResponse.ERR_CONTROLLER_OFFLINE:
                            logger.debug("Controller offline: {}", error.toString());
                            throw new ControllerOfflineException(error.getDescription());
                        case ErrorResponse.ERR_REMOTE_ACCESS_NOT_ALLOWED:
                            logger.debug(
                                    "Remote access not allowed. Access is allowed only from the SHC device network.");
                            throw new RemoteAccessNotAllowedException(
                                    "Remote access not allowed. Access is allowed only from the SHC device network.");
                        case ErrorResponse.ERR_INVALID_ACTION_TRIGGERED:
                            logger.debug("Invalid action triggered. Message: {}", error.getMessages());
                            throw new InvalidActionTriggeredException(error.getDescription());
                        default:
                            logger.debug("Unknown error: {}", error.toString());
                            throw new ApiException("Unknown error: " + error.toString());
                    }
                } catch (final JsonSyntaxException e) {
                    throw new ApiException("Invalid JSON syntax in error response: " + content);
                }
        }
    }

    /**
     * Sets a new state of a SwitchActuator.
     *
     * @param capabilityId
     * @param state
     * @throws IOException
     * @throws ApiException
     */
    public void setSwitchActuatorState(String capabilityId, boolean state)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_SWITCHACTUATOR, state);

        final String json = gson.toJson(action);
        logger.debug("Action toggle JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Sets the dimmer level of a DimmerActuator.
     *
     * @param capabilityId
     * @param dimLevel
     * @throws IOException
     * @throws ApiException
     */
    public void setDimmerActuatorState(String capabilityId, int dimLevel)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_DIMMERACTUATOR, dimLevel);

        final String json = gson.toJson(action);
        logger.debug("Action dimm JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Sets the roller shutter level of a RollerShutterActuator.
     *
     * @param capabilityId
     * @param rollerShutterLevel
     * @throws IOException
     * @throws ApiException
     */
    public void setRollerShutterActuatorState(String capabilityId, int rollerShutterLevel)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_ROLLERSHUTTERACTUATOR,
                rollerShutterLevel);

        final String json = gson.toJson(action);
        logger.debug("Action rollershutter JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Sets a new state of a VariableActuator.
     *
     * @param capabilityId
     * @param state
     * @throws IOException
     * @throws ApiException
     */
    public void setVariableActuatorState(String capabilityId, boolean state)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_VARIABLEACTUATOR, state);

        final String json = gson.toJson(action);
        logger.debug("Action toggle JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Sets the point temperature.
     *
     * @param capabilityId
     * @param pointTemperature
     * @throws IOException
     * @throws ApiException
     */
    public void setPointTemperatureState(String capabilityId, double pointTemperature)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_THERMOSTATACTUATOR, pointTemperature);

        final String json = gson.toJson(action);
        logger.debug("Action toggle JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Sets the operation mode to "Auto" or "Manu".
     *
     * @param capabilityId
     * @param autoMode
     * @throws IOException
     * @throws ApiException
     */
    public void setOperationMode(String capabilityId, boolean autoMode)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_THERMOSTATACTUATOR,
                autoMode ? "Auto" : "Manu");

        final String json = gson.toJson(action);
        logger.debug("Action toggle JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Sets the alarm state.
     *
     * @param capabilityId
     * @param alarmState
     * @throws IOException
     * @throws ApiException
     */
    public void setAlarmActuatorState(String capabilityId, boolean alarmState)
            throws IOException, ApiException, AuthenticationException {
        final Action action = new SetStateAction(capabilityId, Capability.TYPE_ALARMACTUATOR, alarmState);

        final String json = gson.toJson(action);
        logger.debug("Action toggle JSON: {}", json);

        executePost(API_URL_ACTION, action);
    }

    /**
     * Load the device and returns a {@link List} of {@link Device}s..
     *
     * @return List of Devices
     * @throws IOException
     * @throws ApiException
     */
    public List<Device> getDevices() throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading innogy devices...");
        final ContentResponse response = executeGet(API_URL_DEVICE);

        return Arrays.asList(gson.fromJson(response.getContentAsString(), Device[].class));
    }

    /**
     * Loads the {@link Device} with the given deviceId.
     *
     * @param deviceId
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public Device getDeviceById(String deviceId) throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading device with id {}...", deviceId);
        final ContentResponse response = executeGet(API_URL_DEVICE_ID.replace("{id}", deviceId));

        return gson.fromJson(response.getContentAsString(), Device.class);
    }

    /**
     * Returns a {@link List} of all {@link Device}s with the full configuration details, {@link Capability}s and
     * states. Calling this may take a while...
     *
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public List<Device> getFullDevices() throws IOException, ApiException, AuthenticationException {
        // LOCATIONS
        final List<Location> locationList = getLocations();
        final Map<String, Location> locationMap = new HashMap<>();
        for (final Location l : locationList) {
            locationMap.put(l.getId(), l);
        }

        // CAPABILITIES
        final List<Capability> capabilityList = getCapabilities();
        final Map<String, Capability> capabilityMap = new HashMap<>();
        for (final Capability c : capabilityList) {
            capabilityMap.put(c.getId(), c);
        }

        // CAPABILITY STATES
        final List<CapabilityState> capabilityStateList = getCapabilityStates();
        final Map<String, CapabilityState> capabilityStateMap = new HashMap<>();
        for (final CapabilityState cs : capabilityStateList) {
            capabilityStateMap.put(cs.getId(), cs);
        }

        // DEVICE STATES
        final List<DeviceState> deviceStateList = getDeviceStates();
        final Map<String, DeviceState> deviceStateMap = new HashMap<>();
        for (final DeviceState es : deviceStateList) {
            deviceStateMap.put(es.getId(), es);
        }

        // MESSAGES
        final List<Message> messageList = getMessages();
        final Map<String, List<Message>> deviceMessageMap = new HashMap<>();
        for (final Message m : messageList) {
            if (m.getDeviceLinkList() != null && !m.getDeviceLinkList().isEmpty()) {
                final String deviceId = m.getDeviceLinkList().get(0).replace("/device/", "");
                List<Message> ml;
                if (deviceMessageMap.containsKey(deviceId)) {
                    ml = deviceMessageMap.get(deviceId);
                } else {
                    ml = new ArrayList<Message>();
                }
                ml.add(m);
                deviceMessageMap.put(deviceId, ml);
            }
        }

        // DEVICES
        final List<Device> deviceList = getDevices();
        for (final Device d : deviceList) {
            if (BATTERY_POWERED_DEVICES.contains(d.getType())) {
                d.setIsBatteryPowered(true);
            }

            // location
            d.setLocation(locationMap.get(d.getLocationId()));
            final HashMap<String, Capability> deviceCapabilityMap = new HashMap<>();

            // capabilities and their states
            for (final String cl : d.getCapabilityLinkList()) {
                final Capability c = capabilityMap.get(Link.getId(cl));
                final String capabilityId = c.getId();
                final CapabilityState capabilityState = capabilityStateMap.get(capabilityId);
                c.setCapabilityState(capabilityState);
                deviceCapabilityMap.put(capabilityId, c);
            }
            d.setCapabilityMap(deviceCapabilityMap);

            // device states
            d.setDeviceState(deviceStateMap.get(d.getId()));

            // messages
            if (deviceMessageMap.containsKey(d.getId())) {
                d.setMessageList(deviceMessageMap.get(d.getId()));
                for (final Message m : d.getMessageList()) {
                    switch (m.getType()) {
                        case Message.TYPE_DEVICE_LOW_BATTERY:
                            d.setLowBattery(true);
                            d.setLowBatteryMessageId(m.getId());
                            break;
                    }
                }
            }
        }

        return deviceList;
    }

    /**
     * Returns the {@link Device} with the given deviceId with full configuration details, {@link Capability}s and
     * states. Calling this may take a little bit longer...
     *
     * @param deviceId
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public Device getFullDeviceById(String deviceId) throws IOException, ApiException, AuthenticationException {
        // LOCATIONS
        final List<Location> locationList = getLocations();
        final Map<String, Location> locationMap = new HashMap<>();
        for (final Location l : locationList) {
            locationMap.put(l.getId(), l);
        }

        // CAPABILITIES FOR DEVICE
        final List<Capability> capabilityList = getCapabilitiesForDevice(deviceId);
        final Map<String, Capability> capabilityMap = new HashMap<>();
        for (final Capability c : capabilityList) {
            capabilityMap.put(c.getId(), c);
        }

        // CAPABILITY STATES
        final List<CapabilityState> capabilityStateList = getCapabilityStates();
        final Map<String, CapabilityState> capabilityStateMap = new HashMap<>();
        for (final CapabilityState cs : capabilityStateList) {
            capabilityStateMap.put(cs.getId(), cs);
        }

        // DEVICE STATE
        final State state = getDeviceStateByDeviceId(deviceId);
        final DeviceState deviceState = new DeviceState();
        deviceState.setId(deviceId);
        deviceState.setState(state);

        // deviceState.setStateList(deviceStateList);

        // MESSAGES
        final List<Message> messageList = getMessages();

        final List<Message> ml = new ArrayList<>();

        for (final Message m : messageList) {
            logger.trace("Message Type {} with ID {}", m.getType(), m.getId());
            if (m.getDeviceLinkList() != null && !m.getDeviceLinkList().isEmpty()) {
                for (final String li : m.getDeviceLinkList()) {
                    if (li.equals("/device/" + deviceId)) {
                        ml.add(m);
                    }
                }
            }
        }

        // DEVICE
        final Device d = getDeviceById(deviceId);
        if (BATTERY_POWERED_DEVICES.contains(d.getType())) {
            d.setIsBatteryPowered(true);
            d.setLowBattery(false);
        }

        // location
        d.setLocation(locationMap.get(d.getLocationId()));

        // capabilities and their states
        final HashMap<String, Capability> deviceCapabilityMap = new HashMap<>();
        for (final String cl : d.getCapabilityLinkList()) {

            final Capability c = capabilityMap.get(Link.getId(cl));
            c.setCapabilityState(capabilityStateMap.get(c.getId()));
            deviceCapabilityMap.put(c.getId(), c);

        }
        d.setCapabilityMap(deviceCapabilityMap);

        // device states
        d.setDeviceState(deviceState);

        // messages
        if (ml.size() > 0) {
            d.setMessageList(ml);
            for (final Message m : d.getMessageList()) {
                switch (m.getType()) {
                    case Message.TYPE_DEVICE_LOW_BATTERY:
                        d.setLowBattery(true);
                        d.setLowBatteryMessageId(m.getId());
                        break;
                }
            }
        }

        return d;
    }

    /**
     * Loads the states for all {@link Device}s.
     *
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public List<DeviceState> getDeviceStates() throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading device states...");
        final ContentResponse response = executeGet(API_URL_DEVICE_STATES);

        return Arrays.asList(gson.fromJson(response.getContentAsString(), DeviceState[].class));
    }

    /**
     * Loads the device state for the given deviceId.
     *
     * @param deviceId
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public State getDeviceStateByDeviceId(String deviceId) throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading device states for device id {}...", deviceId);
        final ContentResponse response = executeGet(API_URL_DEVICE_ID_STATE.replace("{id}", deviceId));

        return gson.fromJson(response.getContentAsString(), State.class);
    }

    /**
     * Loads the locations and returns a {@link List} of {@link Location}s.
     *
     * @return a List of Devices
     * @throws IOException
     * @throws ApiException
     */
    public List<Location> getLocations() throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading locations...");
        final ContentResponse response = executeGet(API_URL_LOCATION);

        return Arrays.asList(gson.fromJson(response.getContentAsString(), Location[].class));
    }

    /**
     * Loads and returns a {@link List} of {@link Capability}s for the given deviceId.
     *
     * @param deviceId the id of the {@link Device}
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public List<Capability> getCapabilitiesForDevice(String deviceId)
            throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading capabilities for device {}...", deviceId);
        final ContentResponse response = executeGet(API_URL_DEVICE_CAPABILITIES.replace("{id}", deviceId));

        return Arrays.asList(gson.fromJson(response.getContentAsString(), Capability[].class));
    }

    /**
     * Loads and returns a {@link List} of all {@link Capability}s.
     *
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public List<Capability> getCapabilities() throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading capabilities...");
        final ContentResponse response = executeGet(API_URL_CAPABILITY);

        return Arrays.asList(gson.fromJson(response.getContentAsString(), Capability[].class));
    }

    /**
     * Loads and returns a {@link List} of all {@link Capability}States.
     *
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public List<CapabilityState> getCapabilityStates() throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading capability states...");
        final ContentResponse response = executeGet(API_URL_CAPABILITY_STATES);

        return Arrays.asList(gson.fromJson(response.getContentAsString(), CapabilityState[].class));
    }

    /**
     * Returns a {@link List} of all {@link Message}s.
     *
     * @return
     * @throws IOException
     * @throws ApiException
     */
    public List<Message> getMessages() throws IOException, ApiException, AuthenticationException {
        logger.debug("Loading messages...");
        final ContentResponse response = executeGet(API_URL_MESSAGE);

        return Arrays.asList(gson.fromJson(response.getContentAsString(), Message[].class));
    }

    /**
     * @return the configVersion
     */
    public String getConfigVersion() {
        return configVersion;
    }

    /**
     * @param configVersion the configVersion to set
     */
    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }
}
