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
package org.openhab.binding.autoyaml.internal;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;

import org.openhab.binding.autoyaml.internal.model.YamlAutomation;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = Parser.class, property = { "parser.type=parser.yamlrule", "format=yaml" })
public class YamlParser implements Parser<YamlAutomation> {

    @Override
    public Set<YamlAutomation> parse(InputStreamReader reader) throws ParsingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void serialize(Set<YamlAutomation> dataObjects, OutputStreamWriter writer) throws Exception {
        // TODO Auto-generated method stub

    }

}
