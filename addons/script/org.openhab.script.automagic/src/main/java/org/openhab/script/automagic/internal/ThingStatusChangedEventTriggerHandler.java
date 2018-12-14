/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.script.automagic.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseTriggerModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.events.ThingStatusInfoChangedEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@NonNullByDefault
public class ThingStatusChangedEventTriggerHandler extends BaseTriggerModuleHandler
    implements EventSubscriber, EventFilter {

    private final Logger logger = LoggerFactory.getLogger(ThingStatusChangedEventTriggerHandler.class);

    public static final String UPDATE_MODULE_TYPE_ID = "automagic.ThingStatusUpdateTrigger";
    public static final String CHANGE_MODULE_TYPE_ID = "automagic.ThingStatusChangeTrigger";

    private final String CFG_THING_UID = "thingUID";
    private static final String CFG_STATUS = "status";
    private static final String CFG_PREVIOUS_STATUS = "previousStatus";

    private @Nullable final String status;
    private @Nullable final String previousStatus;
    private final String thingUID;
    private final String TOPIC = "smarthome/things/*/statuschanged";
    private final Set<String> types = new HashSet<String>();
    private final BundleContext bundleContext;

    @SuppressWarnings("rawtypes")
    private @Nullable ServiceRegistration eventSubscriberRegistration;

    public ThingStatusChangedEventTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);

        this.status = (String) module.getConfiguration().get(CFG_STATUS);
        this.previousStatus = (String) module.getConfiguration().get(CFG_PREVIOUS_STATUS);
        this.thingUID = (String) module.getConfiguration().get(CFG_THING_UID);
        this.bundleContext = bundleContext;
        this.types.add(ThingStatusInfoChangedEvent.TYPE);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("event.topics", TOPIC);
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
            properties);
    }

    @Override
    public void receive(Event event) {
        if (callback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                event.getTopic(), event.getType(), event.getPayload());

            Map<String, Object> values = new HashMap<>();

            if (event instanceof ThingStatusInfoChangedEvent && UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ThingStatusInfo statusInfo = ((ThingStatusInfoChangedEvent) event).getStatusInfo();
                if (stateMatches(this.status, statusInfo)) {
                    values.put("status", statusInfo);
                }
            } else if (event instanceof ThingStatusInfoChangedEvent
                && CHANGE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ThingStatusInfo statusInfo = ((ThingStatusInfoChangedEvent) event).getStatusInfo();
                ThingStatusInfo oldStatusInfo = ((ThingStatusInfoChangedEvent) event).getOldStatusInfo();

                if (stateMatches(this.status, statusInfo) && stateMatches(this.previousStatus, oldStatusInfo)) {
                    values.put("oldStatus", oldStatusInfo);
                    values.put("newStatus", statusInfo);
                }
            }
            if (!values.isEmpty()) {
                values.put("event", event);
                ((TriggerHandlerCallback) callback).triggered(this.module, values);
            }
        }
    }

    private boolean stateMatches(@Nullable String requiredState, ThingStatusInfo statusInfo) {
        if (requiredState == null) {
            return true;
        }

        String reqState = requiredState.trim();
        return reqState.isEmpty() || reqState.equals(statusInfo.getStatus().toString());
    }

    @Override
    public boolean apply(@Nullable Event event) {
        logger.trace("->FILTER: {}:{}", event.getTopic(), thingUID);
        return event.getTopic().contains("smarthome/things/" + thingUID + "/statuschanged");
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return this;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    /**
     * do the cleanup: unregistering eventSubscriber...
     */
    @Override
    public void dispose() {
        super.dispose();
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            eventSubscriberRegistration = null;
        }
    }
}
