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
package org.openhab.binding.yamahamusiccast.internal.api;

import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.HTTP;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.LONG_CONNECTION_TIMEOUT_MILLISEC;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MakeRequest} is responsible for making http calls to the Yamaha device.
 *
 * @author Lennert Coopman - Initial contribution
 * @author Florian Hotze - Add volume in decibel
 * @author Hilbrand Bouwkamp - Moved make request methods to own class.
 */
@NonNullByDefault
public class MakeRequest {

    public static final String URL = "http://%s/YamahaExtendedControl/v1/";

    private final Logger logger = LoggerFactory.getLogger(MakeRequest.class);

    private String baseUrl = "";

    public void setHost(final String host) {
        baseUrl = String.format(URL, host);
    }

    // API calls to AVR

    // Start Zone Related

    public @Nullable String getStatus(final String zone) {
        return makeRequest("Status", zone + "/getStatus");
    }

    public @Nullable String setPower(final String value, @Nullable final String zone) {
        return makeRequest("Power", zone + "/setPower?power=" + value);
    }

    public @Nullable String setMute(final String value, @Nullable final String zone) {
        return makeRequest("Mute", zone + "/setMute?enable=" + value);
    }

    public @Nullable String setVolume(final int value, @Nullable final String zone) {
        return makeRequest("Volume", zone + "/setVolume?volume=" + value);
    }

    public @Nullable String setVolumeDb(final float value, @Nullable final String zone) {
        return makeRequest("Volume", zone + "/setActualVolume?mode=db&value=" + value);
    }

    public @Nullable String setInput(final String value, @Nullable final String zone) {
        return makeRequest("setInput", zone + "/setInput?input=" + value);
    }

    public @Nullable String setSoundProgram(final String value, @Nullable final String zone) {
        return makeRequest("setSoundProgram", zone + "/setSoundProgram?program=" + value);
    }

    public @Nullable String setPreset(final String value, @Nullable final String zone) {
        return makeRequest("setPreset", "netusb/recallPreset?zone=" + zone + "&num=" + value);
    }

    public @Nullable String setSleep(final String value, @Nullable final String zone) {
        return makeRequest("setSleep", zone + "/setSleep?sleep=" + value);
    }

    public @Nullable String recallScene(final String value, @Nullable final String zone) {
        return makeRequest("recallScene", zone + "/recallScene?num=" + value);
    }
    // End Zone Related

    // Start Net Radio/USB Related

    public @Nullable String getPresetInfo() {
        return makeRequest("PresetInfo", "netusb/getPresetInfo");
    }

    public @Nullable String getRecentInfo() {
        return makeRequest("RecentInfo", "netusb/getRecentInfo");
    }

    public @Nullable String getPlayInfo() {
        return makeRequest("PlayInfo", "netusb/getPlayInfo");
    }

    public @Nullable String setPlayback(final String value) {
        return makeRequest("Playback", "netusb/setPlayback?playback=" + value);
    }

    public @Nullable String setRepeat(final String value) {
        return makeRequest("Repeat", "netusb/setRepeat?mode=" + value);
    }

    public @Nullable String setShuffle(final String value) {
        return makeRequest("Shuffle", "netusb/setShuffle?mode=" + value);
    }

    // End Net Radio/USB Related

    // Start Music Cast API calls
    public @Nullable String getDistributionInfo() {
        return makeRequest("DistributionInfo", "dist/getDistributionInfo");
    }

    public @Nullable String getDistributionInfo(final @Nullable String host) {
        return makeRequest("DistributionInfo", String.format(URL, host), "dist/getDistributionInfo");
    }

    public @Nullable String setClientServerInfo(final String json, final String type) {
        return setClientServerInfo(baseUrl, json, type);
    }

    public @Nullable String setClientServerInfo(final @Nullable String host, final String json, final String type) {
        final InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        try {
            final String url = host + "dist/" + type;
            final String httpResponse = HttpUtil.executeUrl("POST", url, is, "", LONG_CONNECTION_TIMEOUT_MILLISEC);
            logger.trace("MC Link/Unlink Client {}", httpResponse);
            return httpResponse;
        } catch (final IOException e) {
            logger.trace("IO Exception - {} - {}", type, e.getMessage());
            return "{\"response_code\":\"999\"}";
        }
    }

    public @Nullable String startDistribution(@Nullable final String mclinkSetupServer) {
        final Random ran = new Random();
        final int nxt = ran.nextInt(200000);
        return makeRequest("StartDistribution", String.format(URL, mclinkSetupServer),
                "dist/startDistribution?num=" + nxt);
    }

    // End Music Cast API calls

    // Start General/System API calls

    public @Nullable String getFeatures() {
        return makeRequest("Features", "system/getFeatures");
    }

    public @Nullable String getDeviceInfo() {
        return makeRequest("DeviceInfo", "system/getDeviceInfo");
    }

    public void keepUdpEventsAlive() {
        final Properties appProps = new Properties();
        appProps.setProperty("X-AppName", "MusicCast/1");
        appProps.setProperty("X-AppPort", "41100");
        try {
            final String httpResponse = HttpUtil.executeUrl("GET", HTTP + "netusb/getPlayInfo", appProps, null, "",
                    LONG_CONNECTION_TIMEOUT_MILLISEC);
            logger.trace("{} - {}", "UDP task", httpResponse);
        } catch (final IOException e) {
            logger.trace("UDP refresh failed - {}", e.getMessage());
        }
    }
    // End General/System API calls

    private String makeRequest(@Nullable final String topicAVR, final String url) {
        return makeRequest(topicAVR, baseUrl, url);
    }

    private String makeRequest(@Nullable final String topicAVR, final String baseUrl, final String url) {
        try {
            final String response = HttpUtil.executeUrl("GET", baseUrl + url, LONG_CONNECTION_TIMEOUT_MILLISEC);
            logger.trace("{} - {}", topicAVR, response);
            return response;
        } catch (final IOException e) {
            logger.trace("IO Exception - {} - {}", topicAVR, e.getMessage());
            return "{\"response_code\":\"999\"}";
        }
    }
}
