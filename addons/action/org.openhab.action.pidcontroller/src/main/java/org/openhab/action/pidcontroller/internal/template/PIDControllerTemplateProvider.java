/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.template;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.RuleTemplateProvider;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

@Component
public class PIDControllerTemplateProvider implements RuleTemplateProvider {

    private final Map<String, RuleTemplate> providedRuleTemplates = new HashMap<String, RuleTemplate>();
    @SuppressWarnings("rawtypes")
    private ServiceRegistration providerReg;

    public PIDControllerTemplateProvider() {
        providedRuleTemplates.put(PIDControllerRuleTemplate.UID, PIDControllerRuleTemplate.initialize());
    }

    /**
     * To provide the {@link Template}s should register the WelcomeHomeTemplateProvider as {@link TemplateProvider}
     * service.
     *
     * @param bc is a bundle's execution context within the Framework.
     */
    public void register(BundleContext bc) {
        providerReg = bc.registerService(RuleTemplateProvider.class.getName(), this, null);
    }

    /**
     * This method unregisters the WelcomeHomeTemplateProvider as {@link TemplateProvider}
     * service.
     */
    public void unregister() {
        providerReg.unregister();
        providerReg = null;
    }

    @Override
    public RuleTemplate getTemplate(String UID, Locale locale) {
        return providedRuleTemplates.get(UID);
    }

    @Override
    public Collection<RuleTemplate> getTemplates(Locale locale) {
        return providedRuleTemplates.values();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
        // does nothing because this provider does not change
    }

    @Override
    public Collection<RuleTemplate> getAll() {
        return Collections.unmodifiableCollection(providedRuleTemplates.values());
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
        // does nothing because this provider does not change
    }

}
