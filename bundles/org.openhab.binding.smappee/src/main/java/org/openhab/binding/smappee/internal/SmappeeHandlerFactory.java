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
package org.openhab.binding.smappee.internal;

import static org.openhab.binding.smappee.internal.SmappeeBindingConstants.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthFactory;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.smappee.internal.handler.SmappeeActuatorHandler;
import org.openhab.binding.smappee.internal.handler.SmappeeApplianceHandler;
import org.openhab.binding.smappee.internal.handler.SmappeeHandler;
import org.openhab.binding.smappee.internal.handler.SmappeeSensorHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link SmappeeHandlerFactory} is responsible for creating things and thing handlers.
 *
 * @author Niko Tanghe - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.smappee")
@NonNullByDefault
public class SmappeeHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .concat(SmappeeBindingConstants.SUPPORTED_BRIDGE_TYPES_UIDS.stream(),
                    SmappeeBindingConstants.SUPPORTED_THING_TYPES_UIDS.stream())
            .collect(Collectors.toSet());

    private final HttpClient httpClient;
    private OAuthFactory oAuthFactory;

    @Activate
    public SmappeeHandlerFactory(@Reference final HttpClientFactory httpClientFactory,
            @Reference final OAuthFactory oAuthFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.oAuthFactory = oAuthFactory;
    }

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_SMAPPEE.equals(thingTypeUID)) {
            return new SmappeeHandler((Bridge) thing, httpClient, oAuthFactory);
        } else if (THING_TYPE_APPLIANCE.equals(thingTypeUID)) {
            return new SmappeeApplianceHandler(thing);
        } else if (THING_TYPE_ACTUATOR.equals(thingTypeUID)) {
            return new SmappeeActuatorHandler(thing);
        } else if (THING_TYPE_SENSOR.equals(thingTypeUID)) {
            return new SmappeeSensorHandler(thing);
        } else {
            return null;
        }
    }
}
