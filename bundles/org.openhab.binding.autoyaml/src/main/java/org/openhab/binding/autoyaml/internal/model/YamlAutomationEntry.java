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

import org.eclipse.smarthome.config.core.Configuration;

class YamlAutomationEntry {

    private final Map<String, Object> map;

    public YamlAutomationEntry(Map<String, Object> map) {
        this.map = map;
    }

    public Configuration createConfiguration() {
        final Configuration configuration = new Configuration();

        map.forEach((k, v) -> configuration.put(k, v));
        return configuration;
    }

    public String getStringRequired(String key, String haKey) {
        final String value = getString(key, haKey);

        if (value == null) {
            // log or throw?
        }
        return value;
    }

    public String getString(String key, String haKey) {
        final String value = getString(key);

        return value == null ? getString(haKey) : value;
    }

    public String getStringRequired(String key) {
        final String value = getString(key);

        if (value == null) {
            // log or throw?
        }
        return value;
    }

    public String getString(String key) {
        final Object tt = map.get(key);

        if (tt instanceof String) {
            return (String) tt;
        } else {
            // log.warn Not of type string
            return null;
        }
    }

    public Object getObject(String key) {
        return map.get(key);
    }

    public Set<Entry<String, Object>> getAll() {
        return map.entrySet();
    }
}
