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

import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_ALBUM;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_ALBUMART;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_ARTIST;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_INPUT;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_MCLINKSTATUS;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_MUTE;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_PLAYER;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_PLAYTIME;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_POWER;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_RECALLSCENE;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_REPEAT;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_SELECTPRESET;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_SHUFFLE;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_SLEEP;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_SOUNDPROGRAM;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TOTALTIME;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TRACK;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_INPUT;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_MCLINKSTATUS;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_MUTE;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_POWER;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_RECALLSCENE;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_SELECTPRESET;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_SLEEP;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_SOUNDPROGRAM;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_VOLUME;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_VOLUMEABS;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_TYPE_UID_VOLUMEDB;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_VOLUME;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_VOLUMEABS;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.CHANNEL_VOLUMEDB;
import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.HTTP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastUdpService.YamahaMusiccastUdpMessageHandler;
import org.openhab.binding.yamahamusiccast.internal.api.MakeRequest;
import org.openhab.binding.yamahamusiccast.internal.dto.ActualVolume;
import org.openhab.binding.yamahamusiccast.internal.dto.DeviceInfo;
import org.openhab.binding.yamahamusiccast.internal.dto.DistributionInfo;
import org.openhab.binding.yamahamusiccast.internal.dto.Features;
import org.openhab.binding.yamahamusiccast.internal.dto.PlayInfo;
import org.openhab.binding.yamahamusiccast.internal.dto.PresetInfo;
import org.openhab.binding.yamahamusiccast.internal.dto.RecentInfo;
import org.openhab.binding.yamahamusiccast.internal.dto.Response;
import org.openhab.binding.yamahamusiccast.internal.dto.Status;
import org.openhab.binding.yamahamusiccast.internal.dto.UdpMessage;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link YamahaMusiccastHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Lennert Coopman - Initial contribution
 * @author Florian Hotze - Add volume in decibel
 * @author Hilbrand Bouwkamp -
 */
@NonNullByDefault
public class YamahaMusiccastHandler extends BaseThingHandler implements YamahaMusiccastUdpMessageHandler {
    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(YamahaMusiccastHandler.class);

    private final YamahaMusiccastStateDescriptionProvider stateDescriptionProvider;
    private final MakeRequest makeRequest = new MakeRequest();
    private YamahaMusiccastConfiguration configuration = new YamahaMusiccastConfiguration();

    private @Nullable ScheduledFuture<?> generalHousekeepingTask;
    private @Nullable String tmpString = "";
    private int volumePercent = 0;
    private int volumeAbsValue = 0;
    private @Nullable String responseCode = "";
    private int volumeState = 0;
    private int maxVolumeState = 0;
    private @Nullable String inputState = "";
    private @Nullable String soundProgramState = "";
    private int sleepState = 0;
    private @Nullable String artistState = "";
    private @Nullable String trackState = "";
    private @Nullable String albumState = "";
    private @Nullable String repeatState = "";
    private @Nullable String shuffleState = "";
    private int playTimeState = 0;
    private int totalTimeState = 0;
    private @Nullable String zone = "main";
    private String channelWithoutGroup = "";
    private @Nullable String thingLabel = "";
    private @Nullable String mclinkSetupServer = "";
    private @Nullable String mclinkSetupZone = "";
    private String json = "";
    private String action = "";
    private int zoneNum = 0;
    private @Nullable String groupId = "";
    public @Nullable String deviceId = "";


    public YamahaMusiccastHandler(final Thing thing, final YamahaMusiccastStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        String localValueToCheck = "";
        String localRole = "";
        String localDefaultAfterMCLink = "";
        String localRoleSelectedThing = "";
        if (command != RefreshType.REFRESH) {
            logger.trace("Handling command {} for channel {}", command, channelUID);
            channelWithoutGroup = channelUID.getIdWithoutGroup();
            zone = channelUID.getGroupId();
            DistributionInfo distributioninfo = new DistributionInfo();
            Response response = new Response();
            String httpResponse;
            switch (channelWithoutGroup) {
                case CHANNEL_POWER:
                    if (command == OnOffType.ON) {
                        httpResponse = makeRequest.setPower("on", zone);
                        response = gson.fromJson(httpResponse, Response.class);
                        if (response != null) {
                            localValueToCheck = response.getResponseCode();
                            if (!"0".equals(localValueToCheck)) {
                                updateState(channelUID, OnOffType.OFF);
                            }
                        }
                        // check on scheduler task for UDP events
                        final ScheduledFuture<?> localGeneralHousekeepingTask = generalHousekeepingTask;
                        if (localGeneralHousekeepingTask == null) {
                            logger.trace("YXC - No scheduler task found!");
                            generalHousekeepingTask = scheduler.scheduleWithFixedDelay(this::generalHousekeeping, 5,
                                    300, TimeUnit.SECONDS);

                        } else {
                            logger.trace("Scheduler task found!");
                        }

                    } else if (command == OnOffType.OFF) {
                        httpResponse = makeRequest.setPower("standby", zone);
                        response = gson.fromJson(httpResponse, Response.class);
                        powerOffCleanup();
                        if (response != null) {
                            localValueToCheck = response.getResponseCode();
                            if (!"0".equals(localValueToCheck)) {
                                updateState(channelUID, OnOffType.ON);
                            }
                        }
                    }
                    break;
                case CHANNEL_MUTE:
                    if (command == OnOffType.ON) {
                        httpResponse = makeRequest.setMute("true", zone);
                        response = gson.fromJson(httpResponse, Response.class);
                        if (response != null) {
                            localValueToCheck = response.getResponseCode();
                            if (!"0".equals(localValueToCheck)) {
                                updateState(channelUID, OnOffType.OFF);
                            }
                        }
                    } else if (command == OnOffType.OFF) {
                        httpResponse = makeRequest.setMute("false", zone);
                        response = gson.fromJson(httpResponse, Response.class);
                        if (response != null) {
                            localValueToCheck = response.getResponseCode();
                            if (!"0".equals(localValueToCheck)) {
                                updateState(channelUID, OnOffType.ON);
                            }
                        }
                    }
                    break;
                case CHANNEL_VOLUME:
                    volumePercent = Integer.parseInt(command.toString().replace(".0", ""));
                    volumeAbsValue = (maxVolumeState * volumePercent) / 100;
                    makeRequest.setVolume(volumeAbsValue, zone);
                    if (configuration.syncVolume) {
                        tmpString = makeRequest.getDistributionInfo();
                        distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);
                        if (distributioninfo != null) {
                            localRole = distributioninfo.getRole();
                            if ("server".equals(localRole)) {
                                for (final JsonElement ip : distributioninfo.getClientList()) {
                                    final JsonObject clientObject = ip.getAsJsonObject();
                                    setVolumeLinkedDevice(volumePercent, zone,
                                            clientObject.get("ip_address").getAsString());
                                }
                            }
                        }
                    } // END config.syncVolume
                    break;
                case CHANNEL_VOLUMEABS:
                    volumeAbsValue = Integer.parseInt(command.toString().replace(".0", ""));
                    volumePercent = (volumeAbsValue / maxVolumeState) * 100;
                    makeRequest.setVolume(volumeAbsValue, zone);
                    if (configuration.syncVolume) {
                        tmpString = makeRequest.getDistributionInfo();
                        distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);
                        if (distributioninfo != null) {
                            localRole = distributioninfo.getRole();
                            if ("server".equals(localRole)) {
                                for (final JsonElement ip : distributioninfo.getClientList()) {
                                    final JsonObject clientObject = ip.getAsJsonObject();
                                    setVolumeLinkedDevice(volumePercent, zone,
                                            clientObject.get("ip_address").getAsString());
                                }
                            }
                        }
                    }
                    break;
                case CHANNEL_VOLUMEDB:
                    setVolumeDb(((QuantityType<?>) command).floatValue(), zone);
                    if (configuration.syncVolume) {
                        tmpString = makeRequest.getDistributionInfo();
                        distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);
                        if (distributioninfo != null) {
                            localRole = distributioninfo.getRole();
                            if ("server".equals(localRole)) {
                                for (final JsonElement ip : distributioninfo.getClientList()) {
                                    final JsonObject clientObject = ip.getAsJsonObject();
                                    setVolumeDbLinkedDevice(((DecimalType) command).floatValue(), zone,
                                            clientObject.get("ip_address").getAsString());
                                }
                            }
                        }
                    }
                    break;
                case CHANNEL_INPUT:
                    // if it is a client, disconnect it first.
                    tmpString = makeRequest.getDistributionInfo();
                    distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);
                    if (distributioninfo != null) {
                        localRole = distributioninfo.getRole();
                        if ("client".equals(localRole)) {
                            json = "{\"group_id\":\"\"}";
                            httpResponse = makeRequest.setClientServerInfo(json, "setClientInfo");
                        }
                    }
                    makeRequest.setInput(command.toString(), zone);
                    break;
                case CHANNEL_SOUNDPROGRAM:
                    makeRequest.setSoundProgram(command.toString(), zone);
                    break;
                case CHANNEL_SELECTPRESET:
                    makeRequest.setPreset(command.toString(), zone);
                    break;
                case CHANNEL_PLAYER:
                    if (command.equals(PlayPauseType.PLAY)) {
                        makeRequest.setPlayback("play");
                    } else if (command.equals(PlayPauseType.PAUSE)) {
                        makeRequest.setPlayback("pause");
                    } else if (command.equals(NextPreviousType.NEXT)) {
                        makeRequest.setPlayback("next");
                    } else if (command.equals(NextPreviousType.PREVIOUS)) {
                        makeRequest.setPlayback("previous");
                    } else if (command.equals(RewindFastforwardType.REWIND)) {
                        makeRequest.setPlayback("fast_reverse_start");
                    } else if (command.equals(RewindFastforwardType.FASTFORWARD)) {
                        makeRequest.setPlayback("fast_forward_end");
                    }
                    break;
                case CHANNEL_SLEEP:
                    makeRequest.setSleep(command.toString(), zone);
                    break;
                case CHANNEL_MCLINKSTATUS:
                    action = "";
                    json = "";
                    tmpString = makeRequest.getDistributionInfo();
                    distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);
                    if (distributioninfo != null) {
                        responseCode = distributioninfo.getResponseCode();
                        localRole = distributioninfo.getRole();
                        if (command.toString().equals("")) {
                            action = "unlink";
                            groupId = distributioninfo.getGroupId();
                        } else if (command.toString().contains("***")) {
                            action = "link";
                            final String[] parts = command.toString().split("\\*\\*\\*");
                            if (parts.length > 1) {
                                mclinkSetupServer = parts[0];
                                mclinkSetupZone = parts[1];
                                tmpString = makeRequest.getDistributionInfo(mclinkSetupServer);
                                distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);
                                if (distributioninfo != null) {
                                    responseCode = distributioninfo.getResponseCode();
                                    localRoleSelectedThing = distributioninfo.getRole();
                                    groupId = distributioninfo.getGroupId();
                                    if (localRoleSelectedThing != null) {
                                        if ("server".equals(localRoleSelectedThing)) {
                                            groupId = distributioninfo.getGroupId();
                                        } else if ("client".equals(localRoleSelectedThing)) {
                                            groupId = "";
                                        } else if ("none".equals(localRoleSelectedThing)) {
                                            groupId = generateGroupId();
                                        }
                                    }
                                }
                            }
                        }

                        if ("unlink".equals(action)) {
                            json = "{\"group_id\":\"\"}";
                            if (localRole != null) {
                                if ("server".equals(localRole)) {
                                    httpResponse = makeRequest.setClientServerInfo(json, "setServerInfo");
                                    // Set GroupId = "" for linked clients
                                    if (distributioninfo != null) {
                                        for (final JsonElement ip : distributioninfo.getClientList()) {
                                            final JsonObject clientObject = ip.getAsJsonObject();
                                            makeRequest.setClientServerInfo(
                                                    clientObject.get("ip_address").getAsString(), json,
                                                    "setClientInfo");
                                        }
                                    }
                                } else if ("client".equals(localRole)) {
                                    mclinkSetupServer = connectedServer();
                                    // Step 1. empty group on client
                                    httpResponse = makeRequest.setClientServerInfo(json, "setClientInfo");
                                    // empty zone to respect defaults
                                    if (!"".equals(mclinkSetupServer)) {
                                        // Step 2. remove client from server
                                        json = "{\"group_id\":\"" + groupId
                                                + "\", \"type\":\"remove\", \"client_list\":[\"" + configuration.host + "\"]}";
                                        httpResponse = makeRequest.setClientServerInfo(mclinkSetupServer, json,
                                                "setServerInfo");
                                        // Step 3. reflect changes to master
                                        httpResponse = makeRequest.startDistribution(mclinkSetupServer);
                                        localDefaultAfterMCLink = configuration.defaultAfterMCLink;
                                        httpResponse = makeRequest.setInput(localDefaultAfterMCLink.toString(), zone);
                                    } else if ("".equals(mclinkSetupServer)) {
                                        // fallback in case client is removed from group by ending group on server side
                                        localDefaultAfterMCLink = configuration.defaultAfterMCLink;
                                        httpResponse = makeRequest.setInput(localDefaultAfterMCLink.toString(), zone);
                                    }
                                }
                            }
                        } else if ("link".equals(action)) {
                            if (localRole != null) {
                                if ("none".equals(localRole)) {
                                    json = "{\"group_id\":\"" + groupId + "\", \"zone\":\"" + mclinkSetupZone
                                            + "\", \"type\":\"add\", \"client_list\":[\"" + configuration.host + "\"]}";
                                    logger.trace("setServerInfo json: {}", json);
                                    httpResponse = makeRequest.setClientServerInfo(mclinkSetupServer, json,
                                            "setServerInfo");
                                    // All zones of Model are required for MC Link
                                    tmpString = "";
                                    for (int i = 1; i <= zoneNum; i++) {
                                        switch (i) {
                                            case 1:
                                                tmpString = "\"main\"";
                                                break;
                                            case 2:
                                                tmpString = tmpString + ", \"zone2\"";
                                                break;
                                            case 3:
                                                tmpString = tmpString + ", \"zone3\"";
                                                break;
                                            case 4:
                                                tmpString = tmpString + ", \"zone4\"";
                                                break;
                                        }
                                    }
                                    json = "{\"group_id\":\"" + groupId + "\", \"zone\":[" + tmpString + "]}";
                                    logger.trace("setClientInfo json: {}", json);
                                    httpResponse = makeRequest.setClientServerInfo(json, "setClientInfo");
                                    httpResponse = makeRequest.startDistribution(mclinkSetupServer);
                                }
                            }
                        }
                    }
                    updateMCLinkStatus();
                    break;
                case CHANNEL_RECALLSCENE:
                    makeRequest.recallScene(command.toString(), zone);
                    break;
                case CHANNEL_REPEAT:
                    makeRequest.setRepeat(command.toString());
                    break;
                case CHANNEL_SHUFFLE:
                    makeRequest.setShuffle(command.toString());
                    break;
            } // END Switch Channel
        }
    }

    @Override
    public void initialize() {
        thingLabel = thing.getLabel();
        updateStatus(ThingStatus.UNKNOWN);
        final YamahaMusiccastConfiguration configuration = getConfigAs(YamahaMusiccastConfiguration.class);
        this.configuration = configuration;
        final String host = configuration.host;

        if (host != null && !host.isBlank()) {
            makeRequest.setHost(host);
            zoneNum = getNumberOfZones();
            logger.trace("Zones found: {} - {}", zoneNum, thingLabel);

            if (zoneNum > 0) {
                refreshOnStartup();
                generalHousekeepingTask = scheduler.scheduleWithFixedDelay(this::generalHousekeeping, 5, 300,
                        TimeUnit.SECONDS);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "No host found");
            }
        }
    }

    private void generalHousekeeping() {
        thingLabel = thing.getLabel();
        logger.trace("YXC - Start Keep Alive UDP events (5 minutes - {}) ", thingLabel);
        makeRequest.keepUdpEventsAlive();
        fillOptionsForMCLink();
        updateMCLinkStatus();
    }

    private void refreshOnStartup() {
        for (int i = 1; i <= zoneNum; i++) {
            switch (i) {
                case 1:
                    createChannels("main");
                    updateStatusZone("main");
                    break;
                case 2:
                    createChannels("zone2");
                    updateStatusZone("zone2");
                    break;
                case 3:
                    createChannels("zone3");
                    updateStatusZone("zone3");
                    break;
                case 4:
                    createChannels("zone4");
                    updateStatusZone("zone4");
                    break;
            }
        }
        updatePresets(0);
        updateNetUSBPlayer();
        fillOptionsForMCLink();
        updateMCLinkStatus();
    }

    @Override
    public void dispose() {
        final ScheduledFuture<?> localGeneralHousekeepingTask = generalHousekeepingTask;
        if (localGeneralHousekeepingTask != null) {
            localGeneralHousekeepingTask.cancel(true);
        }
    }

    // Various functions

    private void createChannels(final String zone) {
        createChannel(zone, CHANNEL_POWER, CHANNEL_TYPE_UID_POWER, "Switch");
        createChannel(zone, CHANNEL_MUTE, CHANNEL_TYPE_UID_MUTE, "Switch");
        createChannel(zone, CHANNEL_VOLUME, CHANNEL_TYPE_UID_VOLUME, "Dimmer");
        createChannel(zone, CHANNEL_VOLUMEABS, CHANNEL_TYPE_UID_VOLUMEABS, "Number");
        createChannel(zone, CHANNEL_VOLUMEDB, CHANNEL_TYPE_UID_VOLUMEDB, "Number:Dimensionless");
        createChannel(zone, CHANNEL_INPUT, CHANNEL_TYPE_UID_INPUT, "String");
        createChannel(zone, CHANNEL_SOUNDPROGRAM, CHANNEL_TYPE_UID_SOUNDPROGRAM, "String");
        createChannel(zone, CHANNEL_SLEEP, CHANNEL_TYPE_UID_SLEEP, "Number");
        createChannel(zone, CHANNEL_SELECTPRESET, CHANNEL_TYPE_UID_SELECTPRESET, "String");
        createChannel(zone, CHANNEL_RECALLSCENE, CHANNEL_TYPE_UID_RECALLSCENE, "Number");
        createChannel(zone, CHANNEL_MCLINKSTATUS, CHANNEL_TYPE_UID_MCLINKSTATUS, "String");
    }

    private void createChannel(final String zone, final String channel, final ChannelTypeUID channelTypeUID,
            final String itemType) {
        final ChannelUID channelToCheck = new ChannelUID(thing.getUID(), zone, channel);
        if (thing.getChannel(channelToCheck) == null) {
            final ThingBuilder thingBuilder = editThing();
            final Channel testchannel = ChannelBuilder
                    .create(new ChannelUID(getThing().getUID(), zone, channel), itemType).withType(channelTypeUID)
                    .build();
            thingBuilder.withChannel(testchannel);
            updateThing(thingBuilder.build());
        }
    }

    private void powerOffCleanup() {
        ChannelUID channel;
        channel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_ARTIST);
        updateState(channel, StringType.valueOf("-"));
        channel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_TRACK);
        updateState(channel, StringType.valueOf("-"));
        channel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_ALBUM);
        updateState(channel, StringType.valueOf("-"));
    }

    @Override
    public boolean isDeviceId(final String updDeviceId) {
        return updDeviceId.equals(deviceId);
    }

    @Override
    public void processUDPEvent(final UdpMessage udpMessage, final String trackingID) {
        if (!ThingHandlerHelper.isHandlerInitialized(getThing())) {
            return;
        }
        logger.trace("{}: UDP package: {} (Tracking: {})", getThing().getUID(), json, trackingID);
        if (Objects.nonNull(udpMessage.getMain())) {
            updateStateFromUDPEvent("main", udpMessage);
        }
        if (Objects.nonNull(udpMessage.getZone2())) {
            updateStateFromUDPEvent("zone2", udpMessage);
        }
        if (Objects.nonNull(udpMessage.getZone3())) {
            updateStateFromUDPEvent("zone3", udpMessage);
        }
        if (Objects.nonNull(udpMessage.getZone4())) {
            updateStateFromUDPEvent("zone4", udpMessage);
        }
        if (Objects.nonNull(udpMessage.getNetUSB())) {
            updateStateFromUDPEvent("netusb", udpMessage);
        }
        if (Objects.nonNull(udpMessage.getDist())) {
            updateStateFromUDPEvent("dist", udpMessage);
        }
    }

    private void updateStateFromUDPEvent(final String zoneToUpdate, final UdpMessage targetObject) {
        ChannelUID channel;
        String playInfoUpdated = "";
        String statusUpdated = "";
        String powerState = "";
        String muteState = "";
        String inputState = "";
        int volumeState = 0;
        ActualVolume actualVolume = null;
        int presetNumber = 0;
        int playTime = 0;
        String distInfoUpdated = "";
        logger.trace("Handling UDP for {}", zoneToUpdate);
        switch (zoneToUpdate) {
            case "main":
                powerState = targetObject.getMain().getPower();
                muteState = targetObject.getMain().getMute();
                inputState = targetObject.getMain().getInput();
                volumeState = targetObject.getMain().getVolume();
                actualVolume = targetObject.getMain().getActualVolume();
                statusUpdated = targetObject.getMain().getstatusUpdated();
                break;
            case "zone2":
                powerState = targetObject.getZone2().getPower();
                muteState = targetObject.getZone2().getMute();
                inputState = targetObject.getZone2().getInput();
                volumeState = targetObject.getZone2().getVolume();
                actualVolume = targetObject.getZone2().getActualVolume();
                statusUpdated = targetObject.getZone2().getstatusUpdated();
                break;
            case "zone3":
                powerState = targetObject.getZone3().getPower();
                muteState = targetObject.getZone3().getMute();
                inputState = targetObject.getZone3().getInput();
                volumeState = targetObject.getZone3().getVolume();
                actualVolume = targetObject.getZone3().getActualVolume();
                statusUpdated = targetObject.getZone3().getstatusUpdated();
                break;
            case "zone4":
                powerState = targetObject.getZone4().getPower();
                muteState = targetObject.getZone4().getMute();
                inputState = targetObject.getZone4().getInput();
                volumeState = targetObject.getZone4().getVolume();
                actualVolume = targetObject.getZone4().getActualVolume();
                statusUpdated = targetObject.getZone4().getstatusUpdated();
                break;
            case "netusb":
                if (Objects.isNull(targetObject.getNetUSB().getPresetControl())) {
                    presetNumber = 0;
                } else {
                    presetNumber = targetObject.getNetUSB().getPresetControl().getNum();
                }
                playInfoUpdated = targetObject.getNetUSB().getPlayInfoUpdated();
                playTime = targetObject.getNetUSB().getPlayTime();
                // totalTime is not in UDP event
                break;
            case "dist":
                distInfoUpdated = targetObject.getDist().getDistInfoUpdated();
                break;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("{} - Response: {}", zoneToUpdate, responseCode);
            logger.trace("{} - Power: {}", zoneToUpdate, powerState);
            logger.trace("{} - Mute: {}", zoneToUpdate, muteState);
            logger.trace("{} - Volume: {}", zoneToUpdate, volumeState);
            logger.trace("{} - Volume in dB: {}", zoneToUpdate, (actualVolume != null) ? actualVolume.getValue() : "");
            logger.trace("{} - Max Volume: {}", zoneToUpdate, maxVolumeState);
            logger.trace("{} - Input: {}", zoneToUpdate, inputState);
            logger.trace("{} - Soundprogram: {}", zoneToUpdate, soundProgramState);
            logger.trace("{} - Sleep: {}", zoneToUpdate, sleepState);
        }

        if (!powerState.isEmpty()) {
            channel = new ChannelUID(getThing().getUID(), zoneToUpdate, CHANNEL_POWER);
            if ("on".equals(powerState)) {
                updateState(channel, OnOffType.ON);
            } else if ("standby".equals(powerState)) {
                updateState(channel, OnOffType.OFF);
                powerOffCleanup();
            }
        }

        if (!muteState.isEmpty()) {
            channel = new ChannelUID(getThing().getUID(), zoneToUpdate, CHANNEL_MUTE);
            if ("true".equals(muteState)) {
                updateState(channel, OnOffType.ON);
            } else if ("false".equals(muteState)) {
                updateState(channel, OnOffType.OFF);
            }
        }

        if (!inputState.isEmpty()) {
            channel = new ChannelUID(getThing().getUID(), zoneToUpdate, CHANNEL_INPUT);
            updateState(channel, StringType.valueOf(inputState));
        }

        if (volumeState != 0) {
            channel = new ChannelUID(getThing().getUID(), zoneToUpdate, CHANNEL_VOLUME);
            updateState(channel, new PercentType((volumeState * 100) / maxVolumeState));
            channel = new ChannelUID(getThing().getUID(), zoneToUpdate, CHANNEL_VOLUMEABS);
            updateState(channel, new DecimalType(volumeState));
        }

        if (actualVolume != null) {
            channel = new ChannelUID(getThing().getUID(), zoneToUpdate, CHANNEL_VOLUMEDB);
            updateState(channel, new QuantityType<>(actualVolume.getValue(), Units.DECIBEL));
        }

        if (presetNumber != 0) {
            logger.trace("Preset detected: {}", presetNumber);
            updatePresets(presetNumber);
        }

        if ("true".equals(playInfoUpdated)) {
            updateNetUSBPlayer();
        }

        if (!statusUpdated.isEmpty()) {
            updateStatusZone(zoneToUpdate);
        }
        if (playTime != 0) {
            channel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_PLAYTIME);
            updateState(channel, StringType.valueOf(String.valueOf(playTime)));
        }
        if ("true".equals(distInfoUpdated)) {
            updateMCLinkStatus();
        }
    }

    private void updateStatusZone(final String zoneToUpdate) {
        String localZone = "";
        tmpString = makeRequest.getStatus(zoneToUpdate);
        @Nullable
        final Status targetObject = gson.fromJson(tmpString, Status.class);
        if (targetObject != null) {
            final String responseCode = targetObject.getResponseCode();
            final String powerState = targetObject.getPower();
            final String muteState = targetObject.getMute();
            volumeState = targetObject.getVolume();
            final ActualVolume actualVolume = targetObject.getActualVolume();
            maxVolumeState = targetObject.getMaxVolume();
            inputState = targetObject.getInput();
            soundProgramState = targetObject.getSoundProgram();
            sleepState = targetObject.getSleep();

            if (logger.isTraceEnabled()) {
                logger.trace("{} - Response: {}", zoneToUpdate, responseCode);
                logger.trace("{} - Power: {}", zoneToUpdate, powerState);
                logger.trace("{} - Mute: {}", zoneToUpdate, muteState);
                logger.trace("{} - Volume: {}", zoneToUpdate, volumeState);
                logger.trace("{} - Volume in dB: {}", zoneToUpdate,
                        (actualVolume != null) ? actualVolume.getValue() : "");
                logger.trace("{} - Max Volume: {}", zoneToUpdate, maxVolumeState);
                logger.trace("{} - Input: {}", zoneToUpdate, inputState);
                logger.trace("{} - Soundprogram: {}", zoneToUpdate, soundProgramState);
                logger.trace("{} - Sleep: {}", zoneToUpdate, sleepState);
            }

            switch (responseCode) {
                case "0":
                    for (final Channel channel : getThing().getChannels()) {
                        final ChannelUID channelUID = channel.getUID();
                        channelWithoutGroup = channelUID.getIdWithoutGroup();
                        localZone = channelUID.getGroupId();
                        if (localZone != null) {
                            if (isLinked(channelUID)) {
                                switch (channelWithoutGroup) {
                                    case CHANNEL_POWER:
                                        if ("on".equals(powerState)) {
                                            if (localZone.equals(zoneToUpdate)) {
                                                updateState(channelUID, OnOffType.ON);
                                            }
                                        } else if ("standby".equals(powerState)) {
                                            if (localZone.equals(zoneToUpdate)) {
                                                updateState(channelUID, OnOffType.OFF);
                                            }
                                        }
                                        break;
                                    case CHANNEL_MUTE:
                                        if ("true".equals(muteState)) {
                                            if (localZone.equals(zoneToUpdate)) {
                                                updateState(channelUID, OnOffType.ON);
                                            }
                                        } else if ("false".equals(muteState)) {
                                            if (localZone.equals(zoneToUpdate)) {
                                                updateState(channelUID, OnOffType.OFF);
                                            }
                                        }
                                        break;
                                    case CHANNEL_VOLUME:
                                        if (localZone.equals(zoneToUpdate)) {
                                            updateState(channelUID,
                                                    new PercentType((volumeState * 100) / maxVolumeState));
                                        }
                                        break;
                                    case CHANNEL_VOLUMEABS:
                                        if (localZone.equals(zoneToUpdate)) {
                                            updateState(channelUID, new DecimalType(volumeState));
                                        }
                                        break;
                                    case CHANNEL_VOLUMEDB:
                                        if (localZone.equals(zoneToUpdate)) {
                                            if (actualVolume != null) {
                                                updateState(channelUID,
                                                        new QuantityType<>(actualVolume.getValue(), Units.DECIBEL));
                                            } else {
                                                updateState(channelUID, UnDefType.UNDEF);
                                            }
                                        }
                                        break;
                                    case CHANNEL_INPUT:
                                        if (localZone.equals(zoneToUpdate)) {
                                            updateState(channelUID, StringType.valueOf(inputState));
                                        }
                                        break;
                                    case CHANNEL_SOUNDPROGRAM:
                                        if (localZone.equals(zoneToUpdate)) {
                                            updateState(channelUID, StringType.valueOf(soundProgramState));
                                        }
                                        break;
                                    case CHANNEL_SLEEP:
                                        if (localZone.equals(zoneToUpdate)) {
                                            updateState(channelUID, new DecimalType(sleepState));
                                        }
                                        break;
                                } // END switch (channelWithoutGroup)
                            } // END IsLinked
                        }
                    }
                    break;
                case "999":
                    logger.trace("Nothing to do! - {} ({})", thingLabel, zoneToUpdate);
                    break;
            }
        }
    }

    private void updatePresets(final int value) {
        String inputText = "";
        int presetCounter = 0;
        int currentPreset = 0;
        tmpString = makeRequest.getPresetInfo();

        final PresetInfo presetinfo = gson.fromJson(tmpString, PresetInfo.class);
        if (presetinfo != null) {
            final String responseCode = presetinfo.getResponseCode();
            if ("0".equals(responseCode)) {
                final List<StateOption> optionsPresets = new ArrayList<>();
                inputText = getLastInput();
                if (inputText != null) {
                    for (final JsonElement pr : presetinfo.getPresetInfo()) {
                        presetCounter = presetCounter + 1;
                        final JsonObject presetObject = pr.getAsJsonObject();
                        final String text = presetObject.get("text").getAsString();
                        if (!"".equals(text)) {
                            optionsPresets.add(new StateOption(String.valueOf(presetCounter),
                                    "#" + String.valueOf(presetCounter) + " " + text));
                            if (inputText.equals(text)) {
                                currentPreset = presetCounter;
                            }
                        }
                    }
                }
                if (value != 0) {
                    currentPreset = value;
                }
                for (final Channel channel : getThing().getChannels()) {
                    final ChannelUID channelUID = channel.getUID();
                    channelWithoutGroup = channelUID.getIdWithoutGroup();
                    if (isLinked(channelUID)) {
                        switch (channelWithoutGroup) {
                            case CHANNEL_SELECTPRESET:
                                stateDescriptionProvider.setStateOptions(channelUID, optionsPresets);
                                updateState(channelUID, StringType.valueOf(String.valueOf(currentPreset)));
                                break;
                        }
                    }
                }
            }
        }
    }

    private void updateNetUSBPlayer() {
        tmpString = makeRequest.getPlayInfo();

        @Nullable
        final PlayInfo targetObject = gson.fromJson(tmpString, PlayInfo.class);
        if (targetObject != null) {
            final String responseCode = targetObject.getResponseCode();
            final String playbackState = targetObject.getPlayback();
            artistState = targetObject.getArtist();
            trackState = targetObject.getTrack();
            albumState = targetObject.getAlbum();
            String albumArtUrlState = targetObject.getAlbumArtUrl();
            repeatState = targetObject.getRepeat();
            shuffleState = targetObject.getShuffle();
            playTimeState = targetObject.getPlayTime();
            totalTimeState = targetObject.getTotalTime();

            if ("0".equals(responseCode)) {
                ChannelUID testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_PLAYER);
                switch (playbackState) {
                    case "play":
                        updateState(testchannel, PlayPauseType.PLAY);
                        break;
                    case "stop":
                        updateState(testchannel, PlayPauseType.PAUSE);
                        break;
                    case "pause":
                        updateState(testchannel, PlayPauseType.PAUSE);
                        break;
                    case "fast_reverse":
                        updateState(testchannel, RewindFastforwardType.REWIND);
                        break;
                    case "fast_forward":
                        updateState(testchannel, RewindFastforwardType.FASTFORWARD);
                        break;
                }
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_ARTIST);
                updateState(testchannel, StringType.valueOf(artistState));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_TRACK);
                updateState(testchannel, StringType.valueOf(trackState));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_ALBUM);
                updateState(testchannel, StringType.valueOf(albumState));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_ALBUMART);
                if (!"".equals(albumArtUrlState)) {
                    albumArtUrlState = HTTP + configuration.host + albumArtUrlState;
                }
                updateState(testchannel, StringType.valueOf(albumArtUrlState));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_REPEAT);
                updateState(testchannel, StringType.valueOf(repeatState));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_SHUFFLE);
                updateState(testchannel, StringType.valueOf(shuffleState));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_PLAYTIME);
                updateState(testchannel, StringType.valueOf(String.valueOf(playTimeState)));
                testchannel = new ChannelUID(getThing().getUID(), "playerControls", CHANNEL_TOTALTIME);
                updateState(testchannel, StringType.valueOf(String.valueOf(totalTimeState)));
            }
        }
    }

    private @Nullable String getLastInput() {
        String text = "";
        tmpString = makeRequest.getRecentInfo();
        final RecentInfo recentinfo = gson.fromJson(tmpString, RecentInfo.class);
        if (recentinfo != null) {
            final String responseCode = recentinfo.getResponseCode();
            if ("0".equals(responseCode)) {
                for (final JsonElement ri : recentinfo.getRecentInfo()) {
                    final JsonObject recentObject = ri.getAsJsonObject();
                    text = recentObject.get("text").getAsString();
                    break;
                }
            }
        }
        return text;
    }

    private String connectedServer() {
        DistributionInfo distributioninfo = new DistributionInfo();
        String remotehost = "";
        String result = "";
        remotehost = thing.getConfiguration().get("host").toString();
        tmpString = makeRequest.getDistributionInfo(remotehost);
        distributioninfo = gson.fromJson(tmpString, DistributionInfo.class);

        if (distributioninfo != null) {
            final String localRole = distributioninfo.getRole();
            if ("server".equals(localRole)) {
                for (final JsonElement ip : distributioninfo.getClientList()) {
                    final JsonObject clientObject = ip.getAsJsonObject();
                    if (configuration.host != null && configuration.host.equals(clientObject.get("ip_address").getAsString())) {
                        result = remotehost;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private void fillOptionsForMCLink() {
        String host = "";
        String label = "";
        int zonesPerHost = 1;
        int clients = 0;
        tmpString = makeRequest.getDistributionInfo();
        final DistributionInfo targetObject = gson.fromJson(tmpString, DistributionInfo.class);
        if (targetObject != null) {
            clients = targetObject.getClientList().size();
        }

        final List<StateOption> options = new ArrayList<>();
        // first add 3 options for MC Link
        options.add(new StateOption("", "Standalone"));
        options.add(new StateOption("server", "Server: " + clients + " clients"));
        options.add(new StateOption("client", "Client"));

        label = thing.getLabel();
        host = thing.getConfiguration().get("host").toString();
        logger.trace("Thing found on Bridge: {} - {}", host, label);
        zonesPerHost = getNumberOfZones();
        for (int i = 1; i <= zonesPerHost; i++) {
            switch (i) {
                case 1:
                    options.add(new StateOption(host + "***main", label + " - main (" + host + ")"));
                    break;
                case 2:
                    options.add(new StateOption(host + "***zone2", label + " - zone2 (" + host + ")"));
                    break;
                case 3:
                    options.add(new StateOption(host + "***zone3", label + " - zone3 (" + host + ")"));
                    break;
                case 4:
                    options.add(new StateOption(host + "***zone4", label + " - zone4 (" + host + ")"));
                    break;

            }
        }
        // for each zone of the device, set all the possible combinations
        ChannelUID testchannel;
        for (int i = 1; i <= zoneNum; i++) {
            switch (i) {
                case 1:
                    testchannel = new ChannelUID(getThing().getUID(), "main", CHANNEL_MCLINKSTATUS);
                    if (isLinked(testchannel)) {
                        stateDescriptionProvider.setStateOptions(testchannel, options);
                    }
                    break;
                case 2:
                    testchannel = new ChannelUID(getThing().getUID(), "zone2", CHANNEL_MCLINKSTATUS);
                    if (isLinked(testchannel)) {
                        stateDescriptionProvider.setStateOptions(testchannel, options);
                    }
                    break;
                case 3:
                    testchannel = new ChannelUID(getThing().getUID(), "zone3", CHANNEL_MCLINKSTATUS);
                    if (isLinked(testchannel)) {
                        stateDescriptionProvider.setStateOptions(testchannel, options);
                    }
                    break;
                case 4:
                    testchannel = new ChannelUID(getThing().getUID(), "zone4", CHANNEL_MCLINKSTATUS);
                    if (isLinked(testchannel)) {
                        stateDescriptionProvider.setStateOptions(testchannel, options);
                    }
                    break;
            }
        }
    }

    private String generateGroupId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private int getNumberOfZones() {
        int numberOfZones = 0;
        tmpString = makeRequest.getFeatures();
        @Nullable
        final Features targetObject = gson.fromJson(tmpString, Features.class);
        if (targetObject != null) {
            responseCode = targetObject.getResponseCode();
            if ("0".equals(responseCode)) {
                numberOfZones = targetObject.getSystem().getZoneNum();
            }
        }
        return numberOfZones;
    }

    public @Nullable String getDeviceId() {
        tmpString = makeRequest.getDeviceInfo();
        String localValueToCheck = "";
        @Nullable
        final DeviceInfo targetObject = gson.fromJson(tmpString, DeviceInfo.class);
        if (targetObject != null) {
            localValueToCheck = targetObject.getDeviceId();
        }
        return localValueToCheck;
    }

    private void setVolumeLinkedDevice(final int value, @Nullable final String zone, final String host) {
        logger.trace("setVolumeLinkedDevice: {}", host);
        final int zoneNumLinkedDevice = getNumberOfZones();
        int maxVolumeLinkedDevice = 0;
        @Nullable
        Status targetObject = new Status();
        int newVolume = 0;
        for (int i = 1; i <= zoneNumLinkedDevice; i++) {
            switch (i) {
                case 1:
                    tmpString = makeRequest.getStatus("main");
                    targetObject = gson.fromJson(tmpString, Status.class);
                    if (targetObject != null) {
                        responseCode = targetObject.getResponseCode();
                        maxVolumeLinkedDevice = targetObject.getMaxVolume();
                        newVolume = maxVolumeLinkedDevice * value / 100;
                        makeRequest.setVolume(newVolume, "main");
                    }
                    break;
                case 2:
                    tmpString = makeRequest.getStatus("zone2");
                    targetObject = gson.fromJson(tmpString, Status.class);
                    if (targetObject != null) {
                        responseCode = targetObject.getResponseCode();
                        maxVolumeLinkedDevice = targetObject.getMaxVolume();
                        newVolume = maxVolumeLinkedDevice * value / 100;
                        makeRequest.setVolume(newVolume, "zone2");
                    }
                    break;
                case 3:
                    tmpString = makeRequest.getStatus("zone3");
                    targetObject = gson.fromJson(tmpString, Status.class);
                    if (targetObject != null) {
                        responseCode = targetObject.getResponseCode();
                        maxVolumeLinkedDevice = targetObject.getMaxVolume();
                        newVolume = maxVolumeLinkedDevice * value / 100;
                        makeRequest.setVolume(newVolume, "zone3");
                    }
                    break;
                case 4:
                    tmpString = makeRequest.getStatus("zone4");
                    targetObject = gson.fromJson(tmpString, Status.class);
                    if (targetObject != null) {
                        responseCode = targetObject.getResponseCode();
                        maxVolumeLinkedDevice = targetObject.getMaxVolume();
                        newVolume = maxVolumeLinkedDevice * value / 100;
                        makeRequest.setVolume(newVolume, "zone4");
                    }
                    break;
            }
        }
    }

    private void setVolumeDbLinkedDevice(final float value, @Nullable final String zone, final String host) {
        logger.trace("setVolumeDbLinkedDevice: {}", host);
        final int zoneNumLinkedDevice = getNumberOfZones();
        for (int i = 1; i <= zoneNumLinkedDevice; i++) {
            switch (i) {
                case 1:
                    setVolumeDb(value, "main");
                    break;
                case 2:
                    setVolumeDb(value, "zone2");
                    break;
                case 3:
                    setVolumeDb(value, "zone3");
                    break;
                case 4:
                    setVolumeDb(value, "zone4");
                    break;
            }
        }
    }

    public void updateMCLinkStatus() {
        tmpString = makeRequest.getDistributionInfo();
        @Nullable
        final DistributionInfo targetObject = gson.fromJson(tmpString, DistributionInfo.class);
        if (targetObject != null) {
            final String localRole = targetObject.getRole();
            groupId = targetObject.getGroupId();
            switch (localRole) {
                case "none":
                    setMCLinkToStandalone();
                    break;
                case "server":
                    setMCLinkToServer();
                    break;
                case "client":
                    setMCLinkToClient();
                    break;
            }
        }
    }

    private void setMCLinkToStandalone() {
        ChannelUID testchannel;
        for (int i = 1; i <= zoneNum; i++) {
            switch (i) {
                case 1:
                    testchannel = new ChannelUID(getThing().getUID(), "main", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf(""));
                    break;
                case 2:
                    testchannel = new ChannelUID(getThing().getUID(), "zone2", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf(""));
                    break;
                case 3:
                    testchannel = new ChannelUID(getThing().getUID(), "zone3", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf(""));
                    break;
                case 4:
                    testchannel = new ChannelUID(getThing().getUID(), "zone4", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf(""));
                    break;
            }
        }
    }

    private void setMCLinkToClient() {
        ChannelUID testchannel;
        for (int i = 1; i <= zoneNum; i++) {
            switch (i) {
                case 1:
                    testchannel = new ChannelUID(getThing().getUID(), "main", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("client"));
                    break;
                case 2:
                    testchannel = new ChannelUID(getThing().getUID(), "zone2", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("client"));
                    break;
                case 3:
                    testchannel = new ChannelUID(getThing().getUID(), "zone3", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("client"));
                    break;
                case 4:
                    testchannel = new ChannelUID(getThing().getUID(), "zone4", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("client"));
                    break;
            }
        }
    }

    private void setMCLinkToServer() {
        ChannelUID testchannel;
        for (int i = 1; i <= zoneNum; i++) {
            switch (i) {
                case 1:
                    testchannel = new ChannelUID(getThing().getUID(), "main", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("server"));
                    break;
                case 2:
                    testchannel = new ChannelUID(getThing().getUID(), "zone2", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("server"));
                    break;
                case 3:
                    testchannel = new ChannelUID(getThing().getUID(), "zone3", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("server"));
                    break;
                case 4:
                    testchannel = new ChannelUID(getThing().getUID(), "zone4", CHANNEL_MCLINKSTATUS);
                    updateState(testchannel, StringType.valueOf("server"));
                    break;
            }
        }
    }
    // End Various functions

    /**
     * Sets the volume in decibels (dB).
     *
     * @param value volume in dB (decibels)
     * @param zone name of zone
     * @param host hostname or ip address
     * @return HTTP request
     */
    private @Nullable String setVolumeDb(final float value, @Nullable final String zone) {
        final float actualVolumeDB = Math.min(configuration.volumeDbMax, Math.max(configuration.volumeDbMin, value));

        // Yamaha accepts only integer values with .0 or .5 at the end only (-20.5dB, -20.0dB) - at least on RX-S601D.
        // The order matters here. We want to cast to integer first and then scale by 10.
        // Effectively we're only allowing dB values with .0 at the end.
        logger.trace("setVolumeDb: {} dB", actualVolumeDB);
        return makeRequest.setVolumeDb(actualVolumeDB, zone);
    }
}
