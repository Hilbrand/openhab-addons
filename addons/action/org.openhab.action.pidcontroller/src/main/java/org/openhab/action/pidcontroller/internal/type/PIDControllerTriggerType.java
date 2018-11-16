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
import org.eclipse.smarthome.automation.type.Output;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.action.pidcontroller.internal.handler.PIDControllerTriggerHandler;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@NonNullByDefault
public class PIDControllerTriggerType extends TriggerType {

    private static final String DEFAULT_LOOPTIME_MS = "1000";
    public static final String UID = PIDControllerTriggerHandler.MODULE_TYPE_ID;
    public static final String CONFIG_INPUT_ITEM = "input";
    public static final String CONFIG_SETPOINT_ITEM = "setpoint";
    public static final String CONFIG_OUTPUT_LOWER_LIMIT = "outputLowerLimit";
    public static final String CONFIG_OUTPUT_UPPER_LIMIT = "outputUpperLimit";
    public static final String CONFIG_LOOP_TIME = "loopTime";
    public static final String CONFIG_KP_ADJUSTER = "kpAjuster";
    public static final String CONFIG_KI_ADJUSTER = "kiAdjuster";
    public static final String CONFIG_KD_ADJUSTER = "kdAjuster";
    public static final String OUTPUT = "output";

    public static PIDControllerTriggerType initialize() {
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<>();
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_INPUT_ITEM, Type.TEXT).withRequired(true)
                .withReadOnly(true).withMultiple(false).withContext("item").withLabel("Input Item")
                .withDescription("Item to monitor").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_SETPOINT_ITEM, Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withContext("item").withLabel("Setpoint")
                .withDescription("Targeted setpoint").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_OUTPUT_LOWER_LIMIT, Type.DECIMAL)
                .withRequired(true).withMultiple(false).withLabel("Output Lower Limit").withDescription("").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_OUTPUT_UPPER_LIMIT, Type.DECIMAL)
                .withRequired(true).withMultiple(false).withLabel("Output Upper Limit").withDescription("").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_LOOP_TIME, Type.INTEGER)
                .withRequired(true).withMultiple(false).withDefault(DEFAULT_LOOPTIME_MS).withLabel("Loop Time")
                .withDescription("").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_KP_ADJUSTER, Type.DECIMAL)
                .withRequired(true).withMultiple(false).withDefault("1.0").withLabel("Proportional Gain (Kp)")
                .withDescription("Change to output propertional to current error value.").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_KI_ADJUSTER, Type.DECIMAL)
                .withRequired(true).withMultiple(false).withDefault("1.0").withLabel("Integral Gain (Ki)")
                .withDescription("Accelerate movement towards the setpoint.").build());
        configDescriptions.add(ConfigDescriptionParameterBuilder.create(CONFIG_KD_ADJUSTER, Type.DECIMAL)
                .withRequired(true).withMultiple(false).withDefault("1.0").withLabel("Derivative Gain (Kd)")
                .withDescription("Slows the rate of change of the output.").build());

        List<Output> outputs = Collections.singletonList(new Output(OUTPUT, BigDecimal.class.getName(), "Output",
                "Output value of the pid controller", null, null, null));

        return new PIDControllerTriggerType(configDescriptions, outputs);
    }

    public PIDControllerTriggerType(List<ConfigDescriptionParameter> configDescriptions, List<Output> outputs) {
        super(UID, configDescriptions, "PIDController Trigger", "PIDController Trigger Description.", null,
                Visibility.VISIBLE, outputs);
    }

}
