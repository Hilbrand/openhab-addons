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
package org.openhab.binding.spotify.internal.api;

import static org.eclipse.jetty.http.HttpMethod.*;
import static org.openhab.binding.spotify.internal.SpotifyBindingConstants.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openhab.binding.spotify.internal.api.exception.SpotifyAuthorizationException;
import org.openhab.binding.spotify.internal.api.exception.SpotifyException;
import org.openhab.binding.spotify.internal.api.exception.SpotifyTokenExpiredException;
import org.openhab.binding.spotify.internal.api.model.CurrentlyPlayingContext;
import org.openhab.binding.spotify.internal.api.model.Device;
import org.openhab.binding.spotify.internal.api.model.Devices;
import org.openhab.binding.spotify.internal.api.model.Me;
import org.openhab.binding.spotify.internal.api.model.ModelUtil;
import org.openhab.binding.spotify.internal.api.model.Playlist;
import org.openhab.binding.spotify.internal.api.model.Playlists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle Spotify Web Api calls.
 *
 * @author Andreas Stenlund - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored calling Web Api and simplified code
 */
@NonNullByDefault
public class SpotifyApi {

    private static final String CONTENT_TYPE = "application/json";
    private static final String BEARER = "Bearer ";
    private static final char AMP = '&';
    private static final char QSM = '?';
    private static final CurrentlyPlayingContext EMPTY_CURRENTLYPLAYINGCONTEXT = new CurrentlyPlayingContext();
    private static final String PLAY_TRACK_URIS = "{\"uris\":[%s],\"position_ms\":%d}";
    private static final String PLAY_TRACK_CONTEXT_URI = "{\"context_uri\":\"%s\",\"offset\":{\"position\":%d}}";
    private static final String TRANSFER_PLAY = "{\"device_ids\":[\"%s\"],\"play\":%b}";

    private final Logger logger = LoggerFactory.getLogger(SpotifyApi.class);

    private final OAuthClientService oAuthClientService;
    private final SpotifyConnector connector;

    /**
     * Constructor.
     *
     * @param authorizer The authorizer used to refresh the access token when expired
     * @param connector The Spotify connector handling the Web Api calls to Spotify
     */
    public SpotifyApi(final OAuthClientService oAuthClientService, final ScheduledExecutorService scheduler,
            final HttpClient httpClient) {
        this.oAuthClientService = oAuthClientService;
        connector = new SpotifyConnector(scheduler, httpClient);
    }

    /**
     * @return Returns the Spotify user information
     */
    public Me getMe() {
        final ContentResponse response = request(GET, SPOTIFY_API_URL, "");

        return ModelUtil.gsonInstance().fromJson(response.getContentAsString(), Me.class);
    }

    /**
     * Call Spotify Api to play the given track on the given device. If the device id is empty it will be played on
     * the active device.
     *
     * @param deviceId device to play on or empty if play on the active device
     * @param uri id of the track, album, artist or playlist to play
     */
    public void playTrack(final String deviceId, final String uri) {
        final String url = "play" + optionalDeviceId(deviceId, QSM);
        final int offsetIndex = uri.indexOf("#");
        final int position = offsetIndex < 0 ? 0 : Integer.parseInt(uri.substring(offsetIndex));
        final String trackId = offsetIndex < 0 ? uri : uri.substring(0, offsetIndex);
        final String play;

        if (trackId.contains(":track:")) {
            play = String.format(PLAY_TRACK_URIS,
                    Arrays.asList(trackId.split(",")).stream().map(t -> '"' + t + '"').collect(Collectors.joining(",")),
                    position);
        } else {
            play = String.format(PLAY_TRACK_CONTEXT_URI, trackId, position);
        }

        requestPlayer(PUT, url, play);
    }

    /**
     * Call Spotify Api to start playing. If the device id is empty it will start play of the active device.
     *
     * @param deviceId device to play on or empty if play on the active device
     */
    public void play(final String deviceId) {
        requestPlayer(PUT, "play" + optionalDeviceId(deviceId, QSM));
    }

    /**
     * Call Spotify Api to transfer playing to. Depending on play value is start play or pause.
     *
     * @param deviceId device to play on. It can not be empty.
     * @param play if true transfers and starts to play, else transfers but pauses.
     */
    public void transferPlay(final String deviceId, final boolean play) {
        requestPlayer(PUT, "", String.format(TRANSFER_PLAY, deviceId, play));
    }

    /**
     * Call Spotify Api to pause playing. If the device id is empty it will pause play of the active device.
     *
     * @param deviceId device to pause on or empty if pause on the active device
     */
    public void pause(final String deviceId) {
        requestPlayer(PUT, "pause" + optionalDeviceId(deviceId, QSM));
    }

    /**
     * Call Spotify Api to play the next song. If the device id is empty it will play the next song on the active
     * device.
     *
     * @param deviceId device to play next track on or empty if play next track on the active device
     */
    public void next(final String deviceId) {
        requestPlayer(POST, "next" + optionalDeviceId(deviceId, QSM));
    }

    /**
     * Call Spotify Api to play the previous song. If the device id is empty it will play the previous song on the
     * active device.
     *
     * @param deviceId device to play previous track on or empty if play previous track on the active device
     */
    public void previous(final String deviceId) {
        requestPlayer(POST, "previous" + optionalDeviceId(deviceId, QSM));
    }

    /**
     * Call Spotify Api to play set the volume. If the device id is empty it will set the volume on the active device.
     *
     * @param deviceId device to set the Volume on or empty if set volume on the active device
     * @param volumePercent volume percentage value to set
     */
    public void setVolume(final String deviceId, final int volumePercent) {
        requestPlayer(PUT, String.format("volume?volume_percent=%1d", volumePercent) + optionalDeviceId(deviceId, AMP));
    }

    /**
     * Call Spotify Api to play set the repeat state. If the device id is empty it will set the repeat state on the
     * active device.
     *
     * @param deviceId device to set repeat state on or empty if set repeat on the active device
     * @param repeateState set the spotify repeat state
     */
    public void setRepeatState(final String deviceId, final String repeateState) {
        requestPlayer(PUT, String.format("repeat?state=%s", repeateState) + optionalDeviceId(deviceId, AMP));
    }

    /**
     * Call Spotify Api to play set the shuffle. If the device id is empty it will set shuffle state on the active
     * device.
     *
     * @param deviceId device to set shuffle state on or empty if set shuffle on the active device
     * @param state the shuffle state to set
     */
    public void setShuffleState(final String deviceId, final OnOffType state) {
        requestPlayer(PUT, String.format("shuffle?state=%s", state == OnOffType.OFF ? "false" : "true")
                + optionalDeviceId(deviceId, AMP));
    }

    /**
     * Method to return an optional device id url pattern. If device id is empty an empty string is returned else the
     * device id url query pattern prefixed with the given prefix char
     *
     * @param deviceId device to play on or empty if play on the active device
     * @param prefix char to prefix to the deviceId string if present
     * @return empty string or query string part for device id
     */
    private String optionalDeviceId(final String deviceId, final char prefix) {
        return deviceId.isEmpty() ? "" : String.format("%cdevice_id=%s", prefix, deviceId);
    }

    /**
     * @return Calls Spotify Api and returns the list of device or an empty list if nothing was returned
     */
    public List<Device> getDevices() {
        final ContentResponse response = requestPlayer(GET, "devices");
        final Devices deviceList = ModelUtil.gsonInstance().fromJson(response.getContentAsString(), Devices.class);

        return deviceList == null || deviceList.getDevices() == null ? Collections.emptyList()
                : deviceList.getDevices();
    }

    /**
     * @return Returns the playlists of the user.
     */
    public List<Playlist> getPlaylists() {
        final ContentResponse response = request(GET, SPOTIFY_API_URL + "/playlists", "");
        final Playlists playlists = ModelUtil.gsonInstance().fromJson(response.getContentAsString(), Playlists.class);

        return playlists == null || playlists.getItems() == null ? Collections.emptyList() : playlists.getItems();
    }

    /**
     * @return Calls Spotify Api and returns the current playing context of the user or an empty object if no context as
     *         returned by Spotify
     */
    public CurrentlyPlayingContext getPlayerInfo() {
        final ContentResponse response = requestPlayer(GET, "");
        final CurrentlyPlayingContext context = ModelUtil.gsonInstance().fromJson(response.getContentAsString(),
                CurrentlyPlayingContext.class);

        return context == null ? EMPTY_CURRENTLYPLAYINGCONTEXT : context;
    }

    /**
     * Calls the Spotify player Web Api with the given method and appends the given url as parameters of the call to
     * Spotify.
     *
     * @param method Http method to perform
     * @param url url path to call to spotify
     * @return the response give by Spotify
     */
    private ContentResponse requestPlayer(final HttpMethod method, final String url) {
        return requestPlayer(method, url, "");
    }

    /**
     * Calls the Spotify player Web Api with the given method and appends the given url as parameters of the call to
     * Spotify.
     *
     * @param method Http method to perform
     * @param url url path to call to spotify
     * @param requestData data to pass along with the call as content
     * @return the response give by Spotify
     */
    private ContentResponse requestPlayer(final HttpMethod method, final String url, final String requestData) {
        return request(method, SPOTIFY_API_PLAYER_URL + (url.isEmpty() ? "" : ('/' + url)), requestData);
    }

    /**
     * Calls the Spotify Web Api with the given method and given url as parameters of the call to Spotify.
     *
     * @param method Http method to perform
     * @param url url path to call to spotify
     * @param requestData data to pass along with the call as content
     * @return the response give by Spotify
     */
    private ContentResponse request(final HttpMethod method, final String url, final String requestData) {
        logger.debug("Request: ({}) {} - {}", method, url, requestData);
        final Function<HttpClient, Request> call = httpClient -> httpClient.newRequest(url).method(method)
                .header("Accept", CONTENT_TYPE).content(new StringContentProvider(requestData), CONTENT_TYPE);
        try {
            final AccessTokenResponse accessTokenResponse = oAuthClientService.getAccessTokenResponse();
            final String accessToken = accessTokenResponse == null ? null : accessTokenResponse.getAccessToken();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new SpotifyAuthorizationException(
                        "No spotify accesstoken. Did you authorize spotify via /connectspotify ?");
            } else {
                return requestWithRetry(call, accessToken);
            }
        } catch (final IOException e) {
            throw new SpotifyException(e.getMessage(), e);
        } catch (OAuthException | OAuthResponseException e) {
            throw new SpotifyAuthorizationException(e.getMessage(), e);
        }
    }

    private ContentResponse requestWithRetry(final Function<HttpClient, Request> call, final String accessToken)
            throws OAuthException, IOException, OAuthResponseException {
        try {
            return connector.request(call, BEARER + accessToken);
        } catch (final SpotifyTokenExpiredException e) {
            // Retry with new access token
            return connector.request(call, BEARER + oAuthClientService.refreshToken().getAccessToken());
        }
    }
}
