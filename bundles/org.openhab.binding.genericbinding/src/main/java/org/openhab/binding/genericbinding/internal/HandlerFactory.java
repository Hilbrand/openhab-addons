package org.openhab.binding.genericbinding.internal;

import static org.openhab.binding.tplinksmarthome.TPLinkSmartHomeBindingConstants.*;
import static org.openhab.binding.tplinksmarthome.internal.TPLinkSmartHomeThingType.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.genericbinding.handler.Hs100HandlerImpl;
import org.osgi.service.component.annotations.Component;

@NonNullByDefault
@Component(service = ThingHandlerFactory.class, immediate = true)
public class HandlerFactory extends BaseThingHandlerFactory {

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Nullable
    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        switch (thingTypeUID.getId()) {
            case HS100:
                return new Hs100HandlerImpl(thing, new Hs100HandlerImpl2());
        }
    }
}
