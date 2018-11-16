/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.action.pidcontroller.internal.handler.PIDControllerActionHandler;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@NonNullByDefault
public class PIDControllerActionType extends ActionType {
    public static final String UID = PIDControllerActionHandler.MODULE_TYPE_ID;
    public static final String CONFIG_OUTPUT_ITEM = "output";
    public static final String INPUT = "input";
    // public static final String OUTPUT = "output";

    public static PIDControllerActionType initialize() {
        final ConfigDescriptionParameter itemName = ConfigDescriptionParameterBuilder
                .create(CONFIG_OUTPUT_ITEM, Type.TEXT).withRequired(true).withMultiple(false).withReadOnly(true)
                .withContext("item").withLabel("Item").withDescription("Item to send output").build();
        List<ConfigDescriptionParameter> config = new ArrayList<ConfigDescriptionParameter>();
        config.add(itemName);

        List<Input> inputs = Collections.singletonList(new Input(INPUT, BigDecimal.class.getName()));

        return new PIDControllerActionType(config, inputs);
    }

    public PIDControllerActionType(List<ConfigDescriptionParameter> configDescriptions, List<Input> inputs) {
        super(UID, configDescriptions, "PIDController Action", "PIDController Action Description.", null,
                Visibility.VISIBLE, inputs, null);
    }
}
