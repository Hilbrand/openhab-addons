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
package org.openhab.binding.enphase.internal.handler;

import static org.openhab.binding.enphase.internal.EnphaseBindingConstants.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.enphase.internal.EnphaseBindingConstants;
import org.openhab.binding.enphase.internal.dto.InventoryJsonDTO.DeviceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic base Thing handler for different Enphase devices.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
abstract class EnphaseDeviceHandler extends BaseThingHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String serialNumber = "";
    protected @Nullable DeviceDTO lastKnownDeviceState;

    public EnphaseDeviceHandler(final Thing thing) {
        super(thing);
    }

    /**
     * @return the serialNumber
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    protected void handleCommandRefresh(final String channelId) {
        switch (channelId) {
            case DEVICE_CHANNEL_PRODUCING:
                refreshProducing(lastKnownDeviceState);
                break;
            case DEVICE_CHANNEL_COMMUNICATING:
                refreshCommunicating(lastKnownDeviceState);
                break;
            case DEVICE_CHANNEL_PROVISIONED:
                refreshProvisioned(lastKnownDeviceState);
                break;
            case DEVICE_CHANNEL_OPERATING:
                refreshOperating(lastKnownDeviceState);
                break;
        }
    }

    private void refreshProducing(final @Nullable DeviceDTO deviceDTO) {
        updateState(DEVICE_CHANNEL_PRODUCING,
                deviceDTO == null ? UnDefType.UNDEF : OnOffType.from(deviceDTO.isProducing()));
    }

    private void refreshCommunicating(final @Nullable DeviceDTO deviceDTO) {
        updateState(DEVICE_CHANNEL_COMMUNICATING,
                deviceDTO == null ? UnDefType.UNDEF : OnOffType.from(deviceDTO.isCommunicating()));
    }

    private void refreshProvisioned(final @Nullable DeviceDTO deviceDTO) {
        updateState(DEVICE_CHANNEL_PROVISIONED,
                deviceDTO == null ? UnDefType.UNDEF : OnOffType.from(deviceDTO.isProvisioned()));
    }

    private void refreshOperating(final @Nullable DeviceDTO deviceDTO) {
        updateState(DEVICE_CHANNEL_OPERATING,
                deviceDTO == null ? UnDefType.UNDEF : OnOffType.from(deviceDTO.isOperating()));
    }

    public void refreshDeviceState(final @Nullable DeviceDTO deviceDTO) {
        refreshProducing(deviceDTO);
        refreshCommunicating(deviceDTO);
        refreshProvisioned(deviceDTO);
        refreshOperating(deviceDTO);
        refreshProperties(deviceDTO);
        refreshDeviceStatus(deviceDTO == null ? ERROR_NODATA : deviceDTO.getDeviceStatus());
    }

    public void refreshDeviceStatus(final String deviceStatus) {
        if (isInitialized()) {
            if (DEVICE_STATUS_OK.equals(deviceStatus)) {
                if (getThing().getStatus() != ThingStatus.ONLINE) {
                    updateStatus(ThingStatus.ONLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "@text/" + deviceStatus);
            }
        }
    }

    private void refreshProperties(@Nullable final DeviceDTO deviceDTO) {
        if (deviceDTO != null) {
            final Map<String, String> properties = editProperties();

            properties.put(DEVICE_PROPERTY_PART_NUMBER, deviceDTO.getPartNum());
            updateProperties(properties);
        }
    }

    @Override
    public void initialize() {
        serialNumber = (String) getConfig().get(EnphaseBindingConstants.CONFIG_SERIAL_NUMBER);
        updateStatus(ThingStatus.UNKNOWN);
    }
}
