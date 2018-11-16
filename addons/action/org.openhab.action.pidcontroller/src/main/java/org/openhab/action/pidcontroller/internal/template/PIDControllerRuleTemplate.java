/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.openhab.action.pidcontroller.internal.type.PIDControllerActionType;
import org.openhab.action.pidcontroller.internal.type.PIDControllerTriggerType;

@NonNullByDefault
public class PIDControllerRuleTemplate extends RuleTemplate {

    public static final String UID = "PIDControllerRuleTemplate";

    public static PIDControllerRuleTemplate initialize() {
        final String triggerId = UUID.randomUUID().toString();

        final List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId(triggerId)
                .withTypeUID(PIDControllerTriggerType.UID).withLabel("PIDController Trigger").build());

        final Map<String, String> actionInputs = new HashMap<String, String>();
        actionInputs.put(PIDControllerActionType.INPUT, triggerId + "." + PIDControllerTriggerType.OUTPUT);

        final List<Action> actions = Collections.singletonList(ModuleBuilder.createAction()
                .withId(UUID.randomUUID().toString()).withTypeUID(PIDControllerActionType.UID)
                .withLabel("PIDController Action").withInputs(actionInputs).build());

        Set<String> tags = new HashSet<String>();
        tags.add("PIDController");

        return new PIDControllerRuleTemplate(tags, triggers, Collections.emptyList(), actions, Collections.emptyList());
    }

    public PIDControllerRuleTemplate(Set<String> tags, List<Trigger> triggers, List<Condition> conditions,
            List<Action> actions, List<ConfigDescriptionParameter> configDescriptions) {
        super(UID, "PID Controller", "Template for a PID controlled rule", tags, triggers, conditions, actions,
                configDescriptions, Visibility.VISIBLE);
    }

}
