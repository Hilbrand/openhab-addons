package org.openhab.binding.genericbinding.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;

@NonNullByDefault
public class ThingStatusException extends RuntimeException {

    private static final long serialVersionUID = -497732975585864933L;

    private final ThingStatusDetail thingStatusDetail;

    public ThingStatusException(ThingStatusDetail thingStatusDetail, String message) {
        super(message);
        this.thingStatusDetail = thingStatusDetail;
    }

    public ThingStatusException(ThingStatusDetail thingStatusDetail, String message, Exception e) {
        super(message, e);
        this.thingStatusDetail = thingStatusDetail;
    }

    public ThingStatusDetail getThingStatusDetail() {
        return thingStatusDetail;
    }

}
