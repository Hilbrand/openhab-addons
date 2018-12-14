/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.script.automagic.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.config.core.Configuration;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
public class SimpleActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    public static final String UID = "automagic.SimpleAction";

    private static final String FUNCTION = "execute";

    private final BiFunction<Action, Map<String, ?>, Object> function;

    public SimpleActionHandler(final Action module) {
        super(module);
        final Configuration config = module.getConfiguration();
        if (config.getProperties().isEmpty()) {
            throw new IllegalArgumentException("'Configuration' can not be empty.");
        }

        function = (BiFunction<Action, Map<String, ?>, Object>) config.get(FUNCTION);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        final Map<String, Object> resultMap = new HashMap<>();

        resultMap.put("result", function.apply(module, context));
        return resultMap;
    }
}
