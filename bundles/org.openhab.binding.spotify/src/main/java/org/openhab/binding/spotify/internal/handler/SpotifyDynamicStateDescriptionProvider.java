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
package org.openhab.binding.spotify.internal.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.openhab.binding.spotify.internal.api.model.Device;
import org.openhab.binding.spotify.internal.api.model.Playlist;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Dynamically create the users list of devices and playlists.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@Component(service = { DynamicStateDescriptionProvider.class, SpotifyDynamicStateDescriptionProvider.class })
@NonNullByDefault
public class SpotifyDynamicStateDescriptionProvider implements DynamicStateDescriptionProvider {

    private final Map<ChannelUID, List<Device>> devicesByChannel = new HashMap<>();
    private final Map<ChannelUID, List<Playlist>> playlistsByChannel = new HashMap<>();
    private final Map<ChannelUID, @Nullable List<StateOption>> channelOptionsMap = new ConcurrentHashMap<>();

    public void setDevices(ChannelUID channelUID, List<Device> spotifyDevices) {
        final List<Device> devices = devicesByChannel.get(channelUID);

        if (devices == null || (spotifyDevices.size() != devices.size()
                || !spotifyDevices.stream().allMatch(sd -> devices.stream().anyMatch(
                        d -> sd.getId() == d.getId() && d.getName() != null && d.getName().equals(sd.getName()))))) {
            devicesByChannel.put(channelUID, spotifyDevices);
            setStateOptions(channelUID, spotifyDevices.stream()
                    .map(device -> new StateOption(device.getId(), device.getName())).collect(Collectors.toList()));
        }
    }

    public void setPlayLists(ChannelUID channelUID, List<Playlist> spotifyPlaylists) {
        final List<Playlist> playlists = playlistsByChannel.get(channelUID);

        if (playlists == null || (spotifyPlaylists.size() != playlists.size() || !spotifyPlaylists.stream()
                .allMatch(sp -> playlists.stream().anyMatch(p -> p.getUri() != null && p.getUri().equals(sp.getUri())
                        && p.getName() != null && p.getName().equals(sp.getName()))))) {
            playlistsByChannel.put(channelUID, spotifyPlaylists);
            setStateOptions(channelUID,
                    spotifyPlaylists.stream().map(playlist -> new StateOption(playlist.getUri(), playlist.getName()))
                            .collect(Collectors.toList()));
        }
    }

    @Deactivate
    public void deactivate() {
        devicesByChannel.clear();
        playlistsByChannel.clear();
    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel, @Nullable StateDescription original,
            @Nullable Locale locale) {
        List<StateOption> list = channelOptionsMap.get(channel.getUID());
        return list == null ? null
                : (original == null ? StateDescriptionFragmentBuilder.create()
                        : StateDescriptionFragmentBuilder.create(original)).withOptions(list).build()
                                .toStateDescription();
    }

    public void setStateOptions(ChannelUID channelUID, List<StateOption> options) {
        channelOptionsMap.put(channelUID, options);
    }
}
