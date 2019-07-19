/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.autoyaml.internal.builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.Configuration;
import org.openhab.binding.autoyaml.internal.model.YamlTrigger;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.util.TriggerBuilder;

/**
 * <pre>
   trigger:
     platform: state
     entity_id: device_tracker.paulus, device_tracker.anne_therese
     # Optional
     from: 'not_home'
     # Optional
     to: 'home'

     # If given, will trigger when state has been the to state for X time.
     for: '01:10:05'
 * </pre>
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class StateTriggerBuilder extends AbstractTriggerBuilder {

    public static final String PLATFORM = "state";

    private static final String ITEM_NAME = "itemName";
    private static final String PREVIOUS_STATE = "previousState";
    private static final String STATE = "state";

    private static final String HA_ITEM_NAME = "entity_id";
    private static final String HA_PREVIOUS_STATE = "from";
    private static final String HA_STATE = "to";

    public static Trigger build(final YamlTrigger trigger) {
        return TriggerBuilder.create() //
                .withTypeUID(ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID) //
                .withId(trigger.getId()) //
                .withConfiguration(buildConfiguration(trigger)) //
                .build();
    }

    private static Configuration buildConfiguration(final YamlTrigger trigger) {
        final Configuration configuration = new Configuration();
        put(configuration, ITEM_NAME, trigger.getStringRequired(ITEM_NAME, HA_ITEM_NAME));
        put(configuration, PREVIOUS_STATE, trigger.getString(PREVIOUS_STATE, HA_PREVIOUS_STATE));
        put(configuration, STATE, trigger.getString(PREVIOUS_STATE, HA_STATE));
        return configuration;
    }
}
