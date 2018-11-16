/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.handler;

import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.action.pidcontroller.internal.type.PIDControllerActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
public class PIDControllerActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    public static final String MODULE_TYPE_ID = "pidcontroller.PIDControllerAction";
    // public static final String ITEM_COMMAND_ACTION = "core.ItemCommandAction";

    private final Logger logger = LoggerFactory.getLogger(PIDControllerActionHandler.class);

    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;

    /**
     * Constructs a new {@link PIDControllerActionHandler}
     *
     * @param module
     */
    public PIDControllerActionHandler(Action module) {
        super(module);
    }

    /**
     * setter for itemRegistry, used by DS
     *
     * @param itemRegistry
     */
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * unsetter for itemRegistry, used by DS
     *
     * @param itemRegistry
     */
    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    /**
     * setter for eventPublisher used by DS
     *
     * @param eventPublisher
     */
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * unsetter for eventPublisher used by DS
     *
     * @param eventPublisher
     */
    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Override
    public void dispose() {
        this.eventPublisher = null;
        this.itemRegistry = null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        final String itemName = (String) module.getConfiguration().get(PIDControllerActionType.CONFIG_OUTPUT_ITEM);
        final BigDecimal command = (BigDecimal) context.get(PIDControllerActionType.INPUT);

        if (itemName != null && command != null && eventPublisher != null && itemRegistry != null) {
            try {
                final Item item = itemRegistry.getItem(itemName);
                final DecimalType outputValue = new DecimalType(command);
                final ItemCommandEvent itemCommandEvent = ItemEventFactory.createCommandEvent(itemName, outputValue);

                logger.debug("Executing ItemCommandAction on Item {} with Command {}", itemCommandEvent.getItemName(),
                        itemCommandEvent.getItemCommand());
                eventPublisher.post(itemCommandEvent);
            } catch (ItemNotFoundException e) {
                logger.warn("Item with name {} not found in ItemRegistry.", itemName);
            }
        } else {
            logger.warn(
                    "Command was not posted because either the configuration was not correct or a service was missing: ItemName: {}, Command: {}, eventPublisher: {}, ItemRegistry: {}",
                    itemName, command, eventPublisher, itemRegistry);
        }
        return null;
    }

}
