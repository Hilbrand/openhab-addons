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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.openhab.binding.autoyaml.internal.model.YamlAutomation;
import org.yaml.snakeyaml.Yaml;

public class YamlAutomationParser {

    private static final String AUTOMATION = "automation";

    private final Yaml yaml;

    public YamlAutomationParser() {
        yaml = new Yaml();
    }

    public List<YamlAutomation> parse(final String yamlString, final String name) {
        final Map<String, Object> rawAutomation = (Map<String, Object>) yaml.load(yamlString);
        final List<YamlAutomation> automations = new ArrayList<YamlAutomation>();
        final int idx = 0;

        for (final Entry<String, Object> entry : rawAutomation.entrySet()) {
            x(name, automations, idx, entry);
        }
        return automations;
    }

    private void x(final String name, final List<YamlAutomation> automations, final int idx,
            final Entry<String, Object> rawAutomation) {
        if (rawAutomation.getKey().startsWith(AUTOMATION)) {
            if (rawAutomation.getValue() instanceof List) {
                automations.addAll((Collection<? extends YamlAutomation>) ((List) rawAutomation.getValue()).stream()
                        .map(v -> parse(name, idx, v)).collect(Collectors.toList()));
            } else {
                automations.add(parse(name, idx, rawAutomation.getValue()));
            }
        }
    }

    private YamlAutomation parse(final String name, int idx, final Object object) {
        return new AutomationParser(name + (idx++), toMap(object)).parse();
    }

    private Map toMap(final Object value) {
        if (value instanceof Map) {
            return (Map) value;
        } else {
            return null;
        }
    }

}
