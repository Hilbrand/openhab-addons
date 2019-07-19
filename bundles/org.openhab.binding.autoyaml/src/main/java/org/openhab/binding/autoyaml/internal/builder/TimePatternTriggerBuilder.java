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

import org.eclipse.smarthome.config.core.Configuration;
import org.openhab.binding.autoyaml.internal.model.YamlTrigger;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.util.TriggerBuilder;

public class TimePatternTriggerBuilder {
    // cronExpression
    public static final String TYPE = "time_pattern";

    public static Trigger build(YamlTrigger trigger) {
        return TriggerBuilder.create() //
                .withTypeUID(ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID) //
                // .withId(getIdFromLabel("")) //
                .withConfiguration(buildConfiguration(trigger)) //
                .build();
    }

    private static Configuration buildConfiguration(YamlTrigger trigger) {
        final Configuration configuration = new Configuration();
        // put(configuration, "itemName", trigger.getItem());
        // put(configuration, "cronExpression", trigger.getFrom());
        // put(configuration, "state", trigger.getTo());
        return configuration;
    }

    // seconds == null -> 0
    // minutes == null -> seconds is set * (set=true) else 0 (set=false)
    // hours == null -> previous is set * else 0

}
