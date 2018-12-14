/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.script.automagic.internal;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@Component(immediate = true, service = ModuleHandlerFactory.class)
@NonNullByDefault
public class AutomagicModuleFactory extends BaseModuleHandlerFactory {

    private static final Collection<String> TYPES = Arrays.asList(
        ThingStatusChangedEventTriggerHandler.CHANGE_MODULE_TYPE_ID,
        ThingStatusChangedEventTriggerHandler.UPDATE_MODULE_TYPE_ID);

    private final Logger logger = LoggerFactory.getLogger(AutomagicModuleFactory.class);

    private @NonNullByDefault({}) BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    @Deactivate
    protected void deactivate() {
        bundleContext = null;
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        logger.debug("HILBRAND:create {} -> {}", module.getId(), module.getTypeUID());
        ModuleHandler moduleHandler = null;

        if (ThingStatusChangedEventTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            moduleHandler = new ThingStatusChangedEventTriggerHandler((Trigger) module, bundleContext);
        } else if (ThingStatusChangedEventTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            moduleHandler = new ThingStatusChangedEventTriggerHandler((Trigger) module, bundleContext);
        }
        return moduleHandler;
    }

}
