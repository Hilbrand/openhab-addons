/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.thingstatus.internal;

import static org.openhab.binding.thingstatus.internal.ThingStatusBindingConstants.THING_TYPE_THING;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ThingStatusHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.thingstatus", service = ThingHandlerFactory.class)
public class ThingStatusHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_THING);

    private final Map<ThingUID, ServiceRegistration<?>> registrations = new HashMap<>();

    private @NonNullByDefault({}) BundleContext bundleContext;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;

    @Activate
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        super.activate(componentContext);
        bundleContext = componentContext.getBundleContext();
    }

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_THING.equals(thingTypeUID)) {
            final ThingStatusHandler thingStatusHandler = new ThingStatusHandler(thing, thingRegistry);

            registrations.put(thing.getUID(),
                    this.bundleContext.registerService(EventSubscriber.class.getName(), thingStatusHandler, null));
            return thingStatusHandler;
        }
        return null;
    }

    @Override
    protected void removeHandler(final ThingHandler thingHandler) {

        final ServiceRegistration<?> registration = registrations.remove(thingHandler.getThing().getUID());

        if (registration != null) {
            registration.unregister();
        }
    }

    @Reference
    protected void setThingRegistry(final ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(final ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }
}
