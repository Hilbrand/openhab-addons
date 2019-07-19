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

abstract class AbstractTriggerBuilder {

    protected static void put(Configuration configuration, String key, String value) {
        if (value != null) {
            configuration.put(key, value);
        }
    }
}
