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

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.service.AbstractWatchService;
import org.openhab.binding.autoyaml.internal.builder.AutomationBuilder;
import org.openhab.binding.autoyaml.internal.model.YamlAutomation;
import org.openhab.binding.autoyaml.internal.yaml.YamlAutomationParser;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@NonNullByDefault
@Component(immediate = true)
public class YamlAutomationWatcher extends AbstractWatchService /* implements ModelParser */ {

    private static final String YAML_AUTOMATION = "yautomation";

    private final YamlAutomationParser parser = new YamlAutomationParser();
    private final AutomationBuilder builder = new AutomationBuilder();

    private @NonNullByDefault({}) RuleRegistry ruleRegistry;

    private final Map<URL, List<Rule>> loaded = new HashMap<>();

    public YamlAutomationWatcher() {
        super(ConfigConstants.getConfigFolder() + File.separator + YAML_AUTOMATION);
    }

    // @Override
    public String getExtension() {
        return "yaml";
    }

    @Reference
    public void setRuleRegistry(final RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    public void unsetRuleRegistry(final RuleRegistry ruleRegistry) {
        this.ruleRegistry = null;
    }

    @Override
    protected boolean watchSubDirectories() {
        return false;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(@Nullable final Path directory) {
        return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(@Nullable final WatchEvent<?> event, @Nullable final Kind<?> kind,
            @Nullable final Path path) {
        final File file = path.toFile();

        if (!file.isHidden()) {
            try {
                final URL fileUrl = file.toURI().toURL();
                if (kind.equals(ENTRY_DELETE)) {
                    removeFile(fileUrl);
                }

                if (file.canRead() && (kind.equals(ENTRY_CREATE) || kind.equals(ENTRY_MODIFY))) {
                    importFile(fileUrl);
                }
            } catch (final MalformedURLException e) {
                logger.warn("malformed", e);
            }
        }
    }

    private void importFile(final URL fileUrl) {
        String yamlString;
        try {
            final File file = new File(fileUrl.toURI());
            logger.info("Loading yaml automation file '{}'", file.getName());
            yamlString = FileUtils.readFileToString(file);

            final List<YamlAutomation> automations = parser.parse(yamlString,
                    FilenameUtils.removeExtension(file.getName()));
            final List<Rule> rules = automations.stream().map(builder::build).collect(Collectors.toList());
            // TODO fix modify
            rules.forEach(ruleRegistry::add);
            loaded.put(fileUrl, rules);
        } catch (IOException | URISyntaxException e) {
        }
    }

    private void removeFile(final URL fileUrl) {
        final List<Rule> automations = loaded.get(fileUrl);

        if (automations != null) {
            // automations.forEach(r -> ruleRegistry.remove(r.getName()));
        }
        loaded.remove(fileUrl);
    }
}
