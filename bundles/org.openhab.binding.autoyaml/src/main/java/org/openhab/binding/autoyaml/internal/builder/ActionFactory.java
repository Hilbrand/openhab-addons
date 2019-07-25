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

import org.openhab.binding.autoyaml.internal.model.YamlAction;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.util.ActionBuilder;

class ActionFactory {

    public static Action createAction(final YamlAction yAction) {
        return defaultAction(yAction);
    }

    private static Action defaultAction(final YamlAction yAction) {
        return ActionBuilder.create() //
                .withTypeUID(yAction.getTypeUID()) //
                .withId(yAction.getId()) //
                .withLabel(yAction.getLabel()) //
                .withDescription(yAction.getDescription()) //
                .withConfiguration(yAction.createConfiguration()) //
                .build();
    }
}
