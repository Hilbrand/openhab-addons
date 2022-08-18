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

import static org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastBindingConstants.THING_DEVICE;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.yamahamusiccast.internal.YamahaMusiccastUdpService.YamahaMusiccastUdpMessageHandler;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link YamahamusiccastHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Lennert Coopman - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.yamahamusiccast", service = ThingHandlerFactory.class)
public class YamahaMusiccastHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set
            .of(YamahaMusiccastBindingConstants.THING_DEVICE);

    private final YamahaMusiccastStateDescriptionProvider stateDescriptionProvider;
    private final YamahaMusiccastUdpService udpService;

    @Activate
    public YamahaMusiccastHandlerFactory(
            @Reference final YamahaMusiccastStateDescriptionProvider stateDescriptionProvider,
            @Reference final YamahaMusiccastUdpService udpService) {
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.udpService = udpService;
    }

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_DEVICE.equals(thingTypeUID)) {
            final YamahaMusiccastHandler handler = new YamahaMusiccastHandler(thing, stateDescriptionProvider);

            udpService.addHandler(handler);
            return handler;
        }
        return null;
    }

    @Override
    protected void removeHandler(final ThingHandler thingHandler) {
        if (thingHandler instanceof YamahaMusiccastHandler) {
            udpService.removeHandler((YamahaMusiccastUdpMessageHandler) thingHandler);
        }
    }
}
