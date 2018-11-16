/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.factory;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.openhab.action.pidcontroller.internal.handler.PIDControllerActionHandler;
import org.openhab.action.pidcontroller.internal.handler.PIDControllerTriggerHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@NonNullByDefault
public class PIDControllerModuleHandlerFactory extends BaseModuleHandlerFactory {

    public static final String THREADPOOLNAME = "pidcontroller";

    private static final String MODULE_HANDLER_FACTORY_NAME = '[' + PIDControllerModuleHandlerFactory.class.getName()
            + ']';
    private static final Collection<String> TYPES = Arrays.asList(new String[] {
            PIDControllerTriggerHandler.MODULE_TYPE_ID,
            /* PIDControllerTriggerHandler.UPDATE_MODULE_TYPE_ID, */PIDControllerActionHandler.MODULE_TYPE_ID });

    private final Logger logger = LoggerFactory.getLogger(PIDControllerModuleHandlerFactory.class);

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    @SuppressWarnings("rawtypes")
    private @NonNullByDefault({}) ServiceRegistration factoryRegistration;
    private @NonNullByDefault({}) BundleContext bundleContext;

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        factoryRegistration = bundleContext.registerService(ModuleHandlerFactory.class.getName(), this, null);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
        factoryRegistration.unregister();
        factoryRegistration = null;
    }

    /**
     * the itemRegistry was added (called by serviceTracker)
     *
     * @param itemRegistry
     */
    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof PIDControllerActionHandler) {
                ((PIDControllerActionHandler) handler).setItemRegistry(this.itemRegistry);
            }
        }
    }

    /**
     * unsetter for itemRegistry (called by serviceTracker)
     *
     * @param itemRegistry
     */
    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof PIDControllerActionHandler) {
                ((PIDControllerActionHandler) handler).unsetItemRegistry(this.itemRegistry);
            }
        }
        this.itemRegistry = null;
    }

    /**
     * setter for the eventPublisher (called by serviceTracker)
     *
     * @param eventPublisher
     */
    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof PIDControllerActionHandler) {
                ((PIDControllerActionHandler) handler).setEventPublisher(eventPublisher);
            }
        }
    }

    /**
     * unsetter for eventPublisher (called by serviceTracker)
     *
     * @param eventPublisher
     */
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof PIDControllerActionHandler) {
                ((PIDControllerActionHandler) handler).unsetEventPublisher(eventPublisher);
            }
        }
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        final String moduleTypeUID = module.getTypeUID();

        logger.trace("create {} -> {}", module.getId(), moduleTypeUID);
        ModuleHandler moduleHandler = null;

        switch (moduleTypeUID) {
            case PIDControllerTriggerHandler.MODULE_TYPE_ID:
                // case PIDControllerTriggerHandler.UPDATE_MODULE_TYPE_ID:
                moduleHandler = new PIDControllerTriggerHandler((Trigger) module, bundleContext);
                break;
            case PIDControllerActionHandler.MODULE_TYPE_ID:
                moduleHandler = new PIDControllerActionHandler((Action) module);
                ((PIDControllerActionHandler) moduleHandler).setItemRegistry(itemRegistry);
                ((PIDControllerActionHandler) moduleHandler).setEventPublisher(eventPublisher);
                break;
            default:
                logger.warn(MODULE_HANDLER_FACTORY_NAME + " Not supported moduleHandler: {}", moduleTypeUID);
        }
        return moduleHandler;
    }
}
