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
package org.openhab.binding.autoyaml.internal.yaml;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.openhab.binding.autoyaml.internal.model.YamlAutomation;

class AutomationParser {

    private static final String DESCRIPTION = "description";
    private static final String NAME = "name";
    private static final String ALIAS = "alias";
    private static final String TAGS = "tags";
    private static final String TRIGGER = "trigger";
    private static final String CONDITION = "condition";
    private static final String ACTION = "action";

    private final YamlAutomation automation = new YamlAutomation();
    private final Map<Object, Object> rawAutomation;
    private final String name;

    public AutomationParser(final String name, final Map<Object, Object> rawAutomation) {
        this.name = name;
        this.rawAutomation = rawAutomation;
    }

    public YamlAutomation parse() {
        if (rawAutomation == null) {
            return null;
        }
        int idx = 0;
        for (final Entry<Object, Object> entry : rawAutomation.entrySet()) {
            if (entry.getKey() instanceof String) {
                collectAutomationEntry(name + '-' + (idx++), (String) entry.getKey(), entry.getValue());
            } else {
            }
        }
        return automation;
    }

    private void collectAutomationEntry(final String id, final String key, final Object value) {
        switch (key) {
            case NAME:
            case ALIAS:
                automation.setName(value.toString().trim());
                break;
            case DESCRIPTION:
                automation.setDescription(value.toString().trim());
                break;
            case TAGS:
                automation.setTags(value.toString().trim());
                break;
            case TRIGGER:
                addEntry(value, id, automation::addTrigger);
                break;
            case CONDITION:
                // addEntry(value, automation::addCondition);
                break;
            case ACTION:
                addEntry(value, id, automation::addAction);
                break;
            default:
                // TODO log
                break;
        }
    }

    private void addEntry(final Object entry, final String id, final BiConsumer<String, Map<String, Object>> function) {
        if (entry instanceof List) {
            ((List) entry).forEach(l -> function.accept(id, (Map) l));
        } else if (entry instanceof Map) {
            function.accept(id, (Map<String, Object>) entry);
        }
    }
}
