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

public class YamlTrigger extends YamlAutomationEntry {

    private static final String TRIGGER_TYPE_ID = "typeId";
    private static final String HA_TRIGGER_TYPE_ID = "platform";

    private final String id;
    private final String eventType;
    private final String platform;

    public YamlTrigger(final String id, final Map<String, Object> map) {
        super(map);
        // map.remove(TRIGGER_TYPE_ID);
        this.id = id;
        eventType = getStringRequired(TRIGGER_TYPE_ID, HA_TRIGGER_TYPE_ID);
        platform = getString(HA_TRIGGER_TYPE_ID);
    }

    public String getId() {
        return id;
    }

    public String getTriggerType() {
        return eventType;
    }

    public String getPlatform() {
        return platform;
    }
}
