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
import org.openhab.binding.autoyaml.internal.model.YamlTrigger;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.util.TriggerBuilder;

@NonNullByDefault
class TriggerFactory {

    public static Trigger createTrigger(YamlTrigger yTrigger) {
        if (yTrigger.getPlatform() != null) {
            return createHATrigger(yTrigger);
        }
        return defaultTrigger(yTrigger);
    }

    public static Trigger createHATrigger(YamlTrigger yTrigger) {
        switch (yTrigger.getTriggerType()) {
            case StateTriggerBuilder.PLATFORM:
                return StateTriggerBuilder.build(yTrigger);
        }
        throw new RuntimeException();
    }

    private static Trigger defaultTrigger(YamlTrigger yTrigger) {
        final String typeUID = "";
        return TriggerBuilder.create() //
                .withTypeUID(typeUID) //
                .withConfiguration(yTrigger.createConfiguration()) //
                .build();
    }
}
