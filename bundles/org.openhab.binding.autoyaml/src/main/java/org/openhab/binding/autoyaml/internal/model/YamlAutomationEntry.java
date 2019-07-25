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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;

@NonNullByDefault
class YamlAutomationEntry {

    private static final String TRIGGER_TYPE_ID = "typeUID";
    private static final String HA_TRIGGER_TYPE_ID = "platform";

    private static final String CFG_STATE = "state";
    private static final String CFG_PREVIOUS_STATE = "previousState";
    private static final String CFG_COMMAND = "command";
    private static final Set<String> FIX_ON_OFF = Stream.of(CFG_STATE, CFG_PREVIOUS_STATE, CFG_COMMAND)
            .collect(Collectors.toSet());

    private final String id;
    private final String eventType;
    private @Nullable final String platform;
    private @Nullable final String label;
    private @Nullable final String description;

    private final Map<String, Object> map;

    public YamlAutomationEntry(final String id, final Map<String, Object> map) {
        this.id = id;
        this.map = map;
        label = getString("label", "alias");
        description = getString("description");
        eventType = getStringRequired(TRIGGER_TYPE_ID, HA_TRIGGER_TYPE_ID);
        platform = getString(HA_TRIGGER_TYPE_ID);
    }

    public String getId() {
        return id;
    }

    public @Nullable String getLabel() {
        return label;
    }

    public String getTypeUID() {
        return eventType;
    }

    public boolean isHATrigger() {
        return platform != null;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public Configuration createConfiguration() {
        final Configuration configuration = new Configuration();

        map.forEach((k, v) -> configuration.put(k, fix(k, v)));
        return configuration;
    }

    /**
     * yaml parses ON and OFF as true and false. But we want ON and OFF.
     * For known keys where it's expected to be OnOff this method converts the boolean to a OnOff value.
     *
     * @param key
     * @param value
     * @return
     */
    private Object fix(final String key, final Object value) {
        if (FIX_ON_OFF.contains(key) && value instanceof Boolean) {
            return OnOffType.from((Boolean) value).toString();
        }
        return value;
    }

    public String getStringRequired(final String key, final String haKey) {
        final String value = getString(key, haKey);

        if (value == null) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public @Nullable String getString(final String key, final String alternativeKey) {
        final String value = getString(key);

        return value == null ? getString(alternativeKey) : value;
    }

    public @Nullable String getStringRequired(final String key) {
        final String value = getString(key);

        if (value == null) {
            // log or throw?
        }
        return value;
    }

    public @Nullable String getString(final String key) {
        final Object tt = map.get(key);

        if (tt instanceof String) {
            return (String) tt;
        } else {
            // log.warn Not of type string
            return null;
        }
    }

    public @Nullable Object getObject(final String key) {
        return map.get(key);
    }

    public Set<Entry<String, Object>> getAll() {
        return map.entrySet();
    }
}
