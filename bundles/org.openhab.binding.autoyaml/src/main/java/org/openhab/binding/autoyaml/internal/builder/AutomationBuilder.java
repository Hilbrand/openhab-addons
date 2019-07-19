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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.autoyaml.internal.model.YamlAutomation;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.util.RuleBuilder;

@NonNullByDefault
public class AutomationBuilder {

    public Rule build(YamlAutomation automation) {
        return RuleBuilder.create(automation.getAlias()) //
                .withName(automation.getAlias()) //
                .withTriggers(build(automation.getTriggers(), TriggerFactory::createTrigger)) //
                .withConditions(build(automation.getConditions(), ConditionFactory::createCondition)) //
                .withActions(build(automation.getActions(), ActionFactory::createAction)) //
                .withDescription(automation.getDescription()) //
                .withName(automation.getName()) //
                .withTags(automation.getTags()) //
                .build();

        /*
         * SimpleAction({
         * execute: function(module, input) {
         * logger.info("This is a 'hello world!' from a Javascript rule.");
         * })
         */
    }

    private <R, Y> List<R> build(List<Y> yamlList, Function<Y, R> function) {
        return yamlList.stream().map(function).collect(Collectors.toList());
    }

}
