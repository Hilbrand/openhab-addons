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
package org.openhab.binding.autoyaml.internal.yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ModelParserTest {

    private final YamlAutomationParser parser = new YamlAutomationParser();

    @Test
    public void testStateTrigger() throws FileNotFoundException, IOException {
        // final List<YamlAutomation> automation = parser.parse(readYamlFile("statetrigger.yaml"));

        // assertNotNull(automation);
    }

    @Test
    public void testTriggers() throws FileNotFoundException, IOException {
        // final List<YamlAutomation> automation = parser.parse(readYamlFile("triggers.yaml"));

        // assertNotNull(automation);
    }

    // @Test
    public void testTimePattenrTrigger() throws FileNotFoundException, IOException {
        // YamlAutomation automation = parser.parse(readYamlFile("timepatterntrigger.yaml"));
        // assertNotNull(automation);
    }

    private String readYamlFile(final String name) throws FileNotFoundException, IOException {
        return new String(Files.readAllBytes(Paths.get(getClass().getResource(name).getFile())),
                StandardCharsets.UTF_8);
    }
}
