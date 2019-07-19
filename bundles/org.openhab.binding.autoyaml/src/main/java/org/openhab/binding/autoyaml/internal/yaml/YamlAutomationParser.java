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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openhab.binding.autoyaml.internal.model.YamlAutomation;
import org.yaml.snakeyaml.Yaml;

public class YamlAutomationParser {

    private final Yaml yaml;

    public YamlAutomationParser() {
        yaml = new Yaml();
    }

    public List<YamlAutomation> parse(final String yamlString, final String name) {
        final Map<String, Object> rawAutomation = (Map<String, Object>) yaml.load(yamlString);
        final List<YamlAutomation> automations = new ArrayList<YamlAutomation>();
        int idx = 0;

        for (final Entry<String, Object> entry : rawAutomation.entrySet()) {
            automations.add(new AutomationParser(name + (idx++), entry).parse());
        }
        return automations;
    }

}
