/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.stookwijzer.internal;

import static org.openhab.binding.stookwijzer.internal.StookwijzerBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.stookwijzer.internal.handler.StookwijzerHandler;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * {@link StookwijzerHandlerFactory} is the OSGi component factory for the Stookwijzer binding.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.stookwijzer", service = ThingHandlerFactory.class)
public class StookwijzerHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_STOOKWIJZER);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        if (THING_TYPE_STOOKWIJZER.equals(thing.getThingTypeUID())) {
            return new StookwijzerHandler(thing);
        }
        return null;
    }
}
