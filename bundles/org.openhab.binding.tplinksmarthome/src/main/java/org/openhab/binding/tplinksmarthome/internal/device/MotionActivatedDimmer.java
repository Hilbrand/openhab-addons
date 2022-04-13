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
package org.openhab.binding.tplinksmarthome.internal.device;

import static org.openhab.binding.tplinksmarthome.internal.TPLinkSmartHomeBindingConstants.CHANNELS_MOTION_ACTIVATED_CHANNELS;
import static org.openhab.binding.tplinksmarthome.internal.TPLinkSmartHomeBindingConstants.CHANNEL_AMBIENT_LIGHT_SENSOR;
import static org.openhab.binding.tplinksmarthome.internal.TPLinkSmartHomeBindingConstants.CHANNEL_CURRENT_AMBIANCE;
import static org.openhab.binding.tplinksmarthome.internal.TPLinkSmartHomeBindingConstants.CHANNEL_MOTION_SENSOR;
import static org.openhab.binding.tplinksmarthome.internal.TPLinkSmartHomeBindingConstants.CHANNEL_MOTION_SENSOR_RANGE;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tplinksmarthome.internal.model.HasErrorResponse;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * TP-Link Smart Home motion activated dimmer and/or switch device.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class MotionActivatedDimmer extends DimmerDevice {

    @Override
    public boolean handleCommand(final ChannelUID channelUid, final Command command) throws IOException {
        final String channelId = channelUid.getId();

        return CHANNELS_MOTION_ACTIVATED_CHANNELS.contains(channelId) ? handleMotionSensorChannels(channelId, command)
                : super.handleCommand(channelUid, command);
    }

    private boolean handleMotionSensorChannels(final String channelId, final Command command) throws IOException {
        HasErrorResponse response = null;
        final boolean onOff = command instanceof OnOffType;
        final boolean decimal = command instanceof DecimalType;

        switch (channelId) {
            case CHANNEL_MOTION_SENSOR:
                response = onOff
                        ? commands.setMotionSensorResponse(
                                connection.sendCommand(commands.setMotionSensor((OnOffType) command)))
                        : null;
                break;
            case CHANNEL_MOTION_SENSOR_RANGE:
                response = decimal ? commands.setMotionSensorRangeResponse(
                        connection.sendCommand(commands.setMotionSensorRange(((DecimalType) command).intValue())))
                        : null;
                break;
            case CHANNEL_AMBIENT_LIGHT_SENSOR:
                response = onOff ? commands.setAmbientLightSensorResponse(
                        connection.sendCommand(commands.setAmbientLightSensor((OnOffType) command))) : null;
                break;
            default:
                return false;
        }
        checkErrors(response);
        return response != null;
    }

    @Override
    public State updateChannel(final ChannelUID channelUid, final DeviceState deviceState) {
        final State value;

        switch (channelUid.getId()) {
            case CHANNEL_MOTION_SENSOR:
                value = OnOffType.from("");
                break;
            case CHANNEL_MOTION_SENSOR_RANGE:
                value = new DecimalType(1);
                break;
            case CHANNEL_AMBIENT_LIGHT_SENSOR:
                value = OnOffType.from("");
                break;
            case CHANNEL_CURRENT_AMBIANCE:
                value = new DecimalType(1);
                break;
            default:
                value = super.updateChannel(channelUid, deviceState);
        }
        return value;
    }
}
