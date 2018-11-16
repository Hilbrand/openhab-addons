/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.type;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@Component
@NonNullByDefault
public class PIDControllerModuleTypeProvider implements ModuleTypeProvider {

    private final Map<String, ModuleType> providedModuleTypes = new HashMap<String, ModuleType>();
    @SuppressWarnings("rawtypes")
    private @NonNullByDefault({}) ServiceRegistration providerReg;

    public PIDControllerModuleTypeProvider() {
        providedModuleTypes.put(PIDControllerActionType.UID, PIDControllerActionType.initialize());
        providedModuleTypes.put(PIDControllerTriggerType.UID, PIDControllerTriggerType.initialize());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> T getModuleType(@Nullable String UID, @Nullable Locale locale) {
        return (T) providedModuleTypes.get(UID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(@Nullable Locale locale) {
        return (Collection<T>) providedModuleTypes.values();
    }

    /**
     * To provide the {@link ModuleType}s should register the WelcomeHomeModuleTypeProvider as
     * {@link ModuleTypeProvider} service.
     *
     * @param bc is a bundle's execution context within the Framework.
     */
    @Activate
    public void activate(BundleContext bc) {
        providerReg = bc.registerService(ModuleTypeProvider.class.getName(), this, null);
    }

    /**
     * This method unregisters the WelcomeHomeModuleTypeProvider as {@link ModuleTypeProvider}
     * service.
     */
    @Deactivate
    public void deactivate() {
        providerReg.unregister();
        providerReg = null;
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // does nothing because this provider does not change
    }

    @Override
    public Collection<ModuleType> getAll() {
        return Collections.unmodifiableCollection(providedModuleTypes.values());
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // does nothing because this provider does not change
    }

}
