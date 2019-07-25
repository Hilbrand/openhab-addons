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
package org.openhab.binding.autoyaml.internal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class YamlAutomation {

    private String name;
    private String description;
    private Set<String> tags = Collections.emptySet();
    private final List<YamlTrigger> triggers = new ArrayList<>();
    private final List<YamlCondition> conditions = new ArrayList<>();
    private final List<YamlAction> actions = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(String tagString) {
        tags = Arrays.asList(tagString.split(",")).stream().map(String::trim).collect(Collectors.toSet());
    }

    public String getAlias() {
        return null;
    }

    public void addTrigger(String id, Map<String, Object> trigger) {
        triggers.add(new YamlTrigger(id, trigger));
    }

    public List<YamlTrigger> getTriggers() {
        return triggers;
    }

    public void addCondition(String id, Map<String, Object> condition) {
        conditions.add(new YamlCondition(id, condition));
    }

    public List<YamlCondition> getConditions() {
        return conditions;
    }

    public void addAction(String id, Map<String, Object> action) {
        actions.add(new YamlAction(id, action));
    }

    public List<YamlAction> getActions() {
        return actions;
    }
}
