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
package org.openhab.binding.upnpcontrol.internal.discovery;

import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.SUPPORTED_THING_TYPES_UIDS;
import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.THING_TYPE_RENDERER;
import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.THING_TYPE_SERVER;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark Herwege - Initial contribution
 */
@Component(service = { UpnpDiscoveryParticipant.class })
@NonNullByDefault
public class UpnpControlDiscoveryParticipant implements UpnpDiscoveryParticipant {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int removalGracePeriodSeconds;

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public @Nullable DiscoveryResult createResult(final RemoteDevice device) {
        DiscoveryResult result = null;
        final ThingUID thingUid = getThingUID(device);
        if (thingUid != null) {
            final String label = device.getDetails().getFriendlyName().isEmpty() ? device.getDisplayString()
                    : device.getDetails().getFriendlyName();
            final Map<String, Object> properties = new HashMap<>();
            final URL descriptorURL = device.getIdentity().getDescriptorURL();
            properties.put("ipAddress", descriptorURL.getHost());
            properties.put("udn", device.getIdentity().getUdn().getIdentifierString());
            properties.put("deviceDescrURL", descriptorURL.toString());
            final URL baseURL = device.getDetails().getBaseURL();
            if (baseURL != null) {
                properties.put("baseURL", device.getDetails().getBaseURL().toString());
            }
            for (final RemoteService service : device.getServices()) {
                properties.put(service.getServiceType().getType() + "DescrURI", service.getDescriptorURI().toString());
            }
            result = DiscoveryResultBuilder.create(thingUid).withLabel(label).withProperties(properties)
                    .withRepresentationProperty("udn").build();
        }
        return result;
    }

    @Override
    public @Nullable ThingUID getThingUID(final RemoteDevice device) {
        ThingUID result = null;
        final String deviceType = device.getType().getType();
        final String manufacturer = device.getDetails().getManufacturerDetails().getManufacturer();
        final String model = device.getDetails().getModelDetails().getModelName();
        final String serialNumber = device.getDetails().getSerialNumber();
        final String udn = device.getIdentity().getUdn().getIdentifierString();

        logger.debug("Device type {}, manufacturer {}, model {}, SN# {}, UDN {}", deviceType, manufacturer, model,
                serialNumber, udn);

        if ("MediaRenderer".equalsIgnoreCase(deviceType)) {
            this.logger.debug("Media renderer found: {}, {}", manufacturer, model);
            final ThingTypeUID thingTypeUID = THING_TYPE_RENDERER;
            result = new ThingUID(thingTypeUID, device.getIdentity().getUdn().getIdentifierString());
        } else if ("MediaServer".equalsIgnoreCase(deviceType)) {
            this.logger.debug("Media server found: {}, {}", manufacturer, model);
            final ThingTypeUID thingTypeUID = THING_TYPE_SERVER;
            result = new ThingUID(thingTypeUID, device.getIdentity().getUdn().getIdentifierString());
        }
        return result;
    }

    public void setRemovalGracePeriodSeconds(final int removalGracePeriodSeconds) {
        this.removalGracePeriodSeconds = removalGracePeriodSeconds;
    }

    @Override
    public long getRemovalGracePeriodSeconds(final RemoteDevice device) {
        return removalGracePeriodSeconds;
    }
}
