/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.script.automagic.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.core.util.TriggerBuilder;
import org.eclipse.smarthome.automation.module.core.handler.ChannelEventTriggerHandler;
import org.eclipse.smarthome.automation.module.core.handler.ItemStateTriggerHandler;
import org.eclipse.smarthome.automation.module.script.ScriptExtensionProvider;
import org.eclipse.smarthome.automation.module.timer.handler.GenericCronTriggerHandler;
import org.eclipse.smarthome.config.core.Configuration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@Component(immediate = true)
public class AutomagicScriptExtensionProvider implements ScriptExtensionProvider {

    private static final String KEY = "AutoMagic";

    private Map<String, Object> elements;

    @Activate
    protected void activate() {
        elements = new HashMap<>();
        // Util methods/classes
        final Function<String, Logger> logger = name -> LoggerFactory.getLogger(name);
        elements.put("NamedLogger", logger);
        elements.put("RuleBuilder", RuleBuilder.class);
        // Triggers
        final QuadFuction<String, String, String, String, Trigger> thingChangeEventTrigger = //
            (thingUID, status, previousStatus, label) -> TriggerBuilder.create() //
                .withTypeUID(ThingStatusChangedEventTriggerHandler.CHANGE_MODULE_TYPE_ID) //
                .withId(getIdFromLabel(label)) //
                .withLabel(label) //
                .withConfiguration(ConfigurationBuilder.create() //
                    .put("thingUID", thingUID) //
                    .put("status", status) //
                    .put("previousStatus", previousStatus) //
                    .build())
                .build();
        elements.put("ThingStatusChangedEventTrigger", thingChangeEventTrigger);
        final TriFuction<String, String, String, Trigger> thingUpdateEventTrigger = //
            (thingUID, status, label) -> TriggerBuilder.create() //
                .withTypeUID(ThingStatusChangedEventTriggerHandler.UPDATE_MODULE_TYPE_ID) //
                .withId(getIdFromLabel(label)) //
                .withLabel(label) //
                .withConfiguration(ConfigurationBuilder.create() //
                    .put("thingUID", thingUID) //
                    .put("status", status) //
                    .build())
                .build();
        elements.put("ThingStatusUpdateEventTrigger", thingUpdateEventTrigger);
        // ChannelEventTrigger
        final TriFuction<String, String, String, Trigger> channelEventTrigger = //
            (channelUID, event, label) -> TriggerBuilder.create() //
                .withTypeUID(ChannelEventTriggerHandler.MODULE_TYPE_ID) //
                .withId(getIdFromLabel(label)) //
                .withLabel(label) //
                .withConfiguration(ConfigurationBuilder.create() //
                    .put("channelUID", channelUID) //
                    .put("event", event) //
                    .build())
                .build();
        elements.put("ChannelEventTrigger", channelEventTrigger);
        // ItemStateChangeTrigger
        final QuadFuction<String, String, String, String, Trigger> itemStateChangeTrigger = //
            (itemName, state, previousState, label) -> TriggerBuilder.create() //
                .withTypeUID(ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID) //
                .withId(getIdFromLabel(label)) //
                .withLabel(label) //
                .withConfiguration(ConfigurationBuilder.create() //
                    .put("itemName", itemName) //
                    .put("state", state) //
                    .put("previousState", previousState) //
                    .build()) //
                .build();
        elements.put("ItemStateChangeTrigger", itemStateChangeTrigger);

        // ItemStateUpdateTrigger
        final TriFuction<String, String, String, Trigger> itemStateUpdateTrigger = //
            (itemName, state, label) -> TriggerBuilder.create() //
                .withTypeUID(ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID) //
                .withLabel(label) //
                .withConfiguration(ConfigurationBuilder.create() //
                    .put("itemName", itemName) //
                    .put("state", state) //
                    .build()) //
                .build();
        elements.put("ItemStateUpdateTrigger", itemStateUpdateTrigger);

        // CronTrigger
        final BiFunction<String, String, Trigger> cronTrigger = (cronExpression, label) -> {
            return TriggerBuilder.create() //
                .withTypeUID(GenericCronTriggerHandler.MODULE_TYPE_ID) //
                .withId(getIdFromLabel(label))
                .withConfiguration(ConfigurationBuilder.create().put("cronExpression", cronExpression).build()) //
                .withLabel(label) //
                .build();
        };
        elements.put("CronTrigger", cronTrigger);

        // Conditions

        // Actions
        // Function<BiFunction<Action, Map<String, ?>, Object>, Action> simpleAction = (function) ->
        // ActionBuilder.create() //
        // .withTypeUID(SimpleActionHandler.UID) //
        // .withId(getIdFromLabel(null))
        // .withConfiguration(ConfigurationBuilder.create().put("execute", function).build()) //
        // .build();
        // Function<String, SimpleAction> sa = id -> new SimpleAction(id);
        // elements.put("SimpleAction", sa);
    }

    private String getIdFromLabel(String label) {
        return label == null || label.isEmpty() ? UUID.randomUUID().toString() : label.replaceAll("[\\W]", "-");
    }

    @Deactivate
    protected void deactivate() {
        elements = null;
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Collections.singleton(KEY);
    }

    @Override
    public Collection<String> getPresets() {
        return Collections.singleton(KEY);
    }

    @Override
    public Collection<String> getTypes() {
        return elements.keySet();
    }

    @Override
    public Object get(String scriptIdentifier, String type) {
        return elements.get(type);
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        return elements;
    }

    @Override
    public void unload(String scriptIdentifier) {
        // nothing todo
    }

    private static class ConfigurationBuilder {
        private final Configuration configuration = new Configuration();

        public static ConfigurationBuilder create() {
            return new ConfigurationBuilder();
        }

        public Configuration build() {
            return configuration;
        }

        public ConfigurationBuilder put(String key, Object value) {
            configuration.put(key, value);
            return this;
        }
    }

    @FunctionalInterface
    public interface TriFuction<A, B, C, R> {
        R apply(A arg1, B arg2, C arg3);
    }

    @FunctionalInterface
    public interface QuadFuction<A, B, C, D, R> {
        R apply(A arg1, B arg2, C arg3, D arg4);
    }
}
