/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.extensionservice.marketplace.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.addonservice.marketplace.MarketplaceHandlerException;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.EventPublisher;
import org.openhab.extensionservice.marketplace.internal.model.AddonDTO;
import org.openhab.extensionservice.marketplace.internal.model.PackageDTO;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an {@link AddonService}, which accesses the openHAB Marketplace and makes its content available as Add-ons.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Component(configurationPid = "org.openhab.marketplace", property = {
        Constants.SERVICE_PID + "=org.openhab.marketplace"
})
@ConfigurableService(category = "system", description_uri = "system:marketplace", label = "Addon Marketplace")
@NonNullByDefault
public class MarketplaceExtensionService implements AddonService {
    /**
     * Enumeration of supported Addon package types plus associated attributes.
     */
    private enum PackageType {
        BINDING("binding", Addon., "bindings", "Bindings"),
        RULE_TEMPLATE("rule_template", MarketplaceAddon.EXT_TYPE_RULE_TEMPLATE, "ruletemplates", "Rule Templates"),
        VOICE("voice", MarketplaceAddon.EXT_TYPE_VOICE, "voice", "Voice");

        /**
         * Constant used in marketplace nodes.
         */
        final String typeName;

        /**
         * MarketplaceAddon.EXT_TYPE_ symbolic name.
         */
        final String extType;

        /**
         * Key used in config file for setting visibility property.
         */
        final String configKey;

        /**
         * Label to display on Paper UI tab.
         */
        final String label;

        private PackageType(final String typeName, final String extType, final String configKey, final String label) {
            this.typeName = typeName;
            this.extType = extType;
            this.configKey = configKey;
            this.label = label;
        }
    }

    private static final String MARKETPLACE_HOST = "marketplace.eclipse.org";
    private static final Pattern Addon_ID_PATTERN = Pattern.compile(".*?mpc_install=([^&]+?)(&.*)?");

    private final Logger logger = LoggerFactory.getLogger(MarketplaceAddonService.class);

    // increased visibility for unit tests
    MarketplaceProxy proxy;
    private EventPublisher eventPublisher;
    private final Pattern labelPattern = Pattern.compile("<.*>"); // checks for the existence of any xml element
    private final Pattern descriptionPattern = Pattern.compile("<(javascript|div|font)"); // checks for the existence of
    // some
    // invalid elements

    // configured package type inclusion settings, keyed by package typeName
    private Map<String, Boolean> packageTypeInclusions = new HashMap<>();

    private int maturityLevel = 1;
    private final Set<MarketplaceAddonHandler> AddonHandlers = new HashSet<>();

    @Activate
    protected void activate(final Map<String, Object> config) {
        this.proxy = new MarketplaceProxy();
        modified(config);
    }

    @Deactivate
    protected void deactivate() {
        this.proxy.dispose();
        this.proxy = null;
    }

    @Modified
    protected void modified(final Map<String, Object> config) {
        for (final PackageType packageType : PackageType.values()) {
            final Object inclusionCfg = config.get(packageType.configKey);
            if (inclusionCfg != null) {
                packageTypeInclusions.put(packageType.typeName,
                        inclusionCfg.toString().equals(Boolean.TRUE.toString()));
            }
        }
        final Object cfgMaturityLevel = config.get("maturity");
        if (cfgMaturityLevel != null) {
            try {
                this.maturityLevel = Integer.valueOf(cfgMaturityLevel.toString());
            } catch (final NumberFormatException e) {
                logger.warn("Ignoring invalid value '{}' for configuration parameter '{}'", cfgMaturityLevel.toString(),
                        "maturity");
            }
        }
    }

    @Reference
    protected void setEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonHandler(final MarketplaceAddonHandler handler) {
        this.AddonHandlers.add(handler);
    }

    protected void removeAddonHandler(final MarketplaceAddonHandler handler) {
        this.AddonHandlers.remove(handler);
    }

    @Override
    public List<Addon> getAddons(final Locale locale) {
        final List<AddonDTO> addons = proxy.getNodes();
        final List<Addon> exts = new ArrayList<>(addons.size());
        for (final AddonDTO addon : addons) {
            //            if (toMaturityLevel(node.status) < this.maturityLevel) {
            //                continue;
            //            }
            if (!packageTypeInclusions.getOrDefault(addon.packagetypes, true)) {
                continue;
            }

            final addon.
            final MarketplaceAddon ext = convertToAddon(addon);
            if (ext != null) {
                if (setInstalledFlag(ext)) {
                    exts.add(ext);
                }
            }
        }
        return exts;
    }

    private boolean setInstalledFlag(final MarketplaceAddon ext) {
        for (final MarketplaceAddonHandler handler : AddonHandlers) {
            if (handler.supports(ext)) {
                ext.setInstalled(handler.isInstalled(ext));
                return true;
            }
        }
        return false;
    }

    private MarketplaceAddon convertToAddon(final AddonDTO addon, final PackageDTO packageDTO) {
        final String extId = "";//getAddonId(addon);

        final String name = addon.name;
        final String desc = addon.description;
        final String version = packageDTO.version == null || packageDTO.version.isEmpty() ? "1.0" : packageDTO.version;

        if (!validName(name) || !validDescription(desc)) {
            //            logger.debug("Ignoring node {} due to invalid content.", node.id);
            return null;
        }
        for (final PackageType packageType : PackageType.values()) {
            if (packageType.typeName.equals(addon.packageType)) {
                final MarketplaceAddon ext = new MarketplaceAddon(extId, packageType.extType, name, version,
                        addon.url, false, desc, null, null/*node.image*/, packageDTO.packageUrl, addon.packageFormat);
                return ext;
            }
        }
        return null;
    }

    @Override
    public Addon getAddon(final String id, final Locale locale) {
        for (final Addon Addon : getAddons(locale)) {
            if (Addon.getId().equals(id)) {
                return Addon;
            }
        }
        return null;
    }

    @Override
    public List<AddonType> getTypes(final Locale locale) {
        final ArrayList<AddonType> types = new ArrayList<>(2);
        final List<Addon> exts = getAddons(locale);
        for (final PackageType packageType : PackageType.values()) {
            if (packageTypeInclusions.getOrDefault(packageType.typeName, true)) {
                for (final Addon ext : exts) {
                    if (ext.getType().equals(packageType.extType)) {
                        types.add(new AddonType(packageType.extType, packageType.label));
                        break;
                    }
                }
            }
        }
        return Collections.unmodifiableList(types);
    }

    @Override
    public void install(final String AddonId) {
        final Addon ext = getAddon(AddonId, null);
        if (ext instanceof MarketplaceAddon) {
            final MarketplaceAddon mpExt = (MarketplaceAddon) ext;
            for (final MarketplaceAddonHandler handler : AddonHandlers) {
                if (handler.supports(mpExt)) {
                    if (!handler.isInstalled(mpExt)) {
                        try {
                            handler.install(mpExt);
                            postInstalledEvent(AddonId);
                        } catch (final MarketplaceHandlerException e) {
                            postFailureEvent(AddonId, e.getMessage());
                        }
                    } else {
                        postFailureEvent(AddonId, "Addon is already installed.");
                    }
                    return;
                }
            }
        }
        postFailureEvent(AddonId, "Addon not known.");
    }

    @Override
    public void uninstall(final String AddonId) {
        final Addon ext = getAddon(AddonId, null);
        if (ext instanceof MarketplaceAddon) {
            final MarketplaceAddon mpExt = (MarketplaceAddon) ext;
            for (final MarketplaceAddonHandler handler : AddonHandlers) {
                if (handler.supports(mpExt)) {
                    if (handler.isInstalled(mpExt)) {
                        try {
                            handler.uninstall(mpExt);
                            postUninstalledEvent(AddonId);
                        } catch (final MarketplaceHandlerException e) {
                            postFailureEvent(AddonId, e.getMessage());
                        }
                    } else {
                        postFailureEvent(AddonId, "Addon is not installed.");
                    }
                    return;
                }
            }
        }
        postFailureEvent(AddonId, "Addon not known.");
    }

    @Override
    public String getAddonId(final URI AddonURI) {
        if (AddonURI != null && AddonURI.getHost().equals(MARKETPLACE_HOST)) {
            return extractExensionId(AddonURI);
        }

        return null;
    }

    private void postInstalledEvent(final String AddonId) {
        final Event event = AddonEventFactory.createAddonInstalledEvent(AddonId);
        eventPublisher.post(event);
    }

    private void postUninstalledEvent(final String AddonId) {
        final Event event = AddonEventFactory.createAddonUninstalledEvent(AddonId);
        eventPublisher.post(event);
    }

    private void postFailureEvent(final String AddonId, final String msg) {
        final Event event = AddonEventFactory.createAddonFailureEvent(AddonId, msg);
        eventPublisher.post(event);
    }

    private String getAddonId(final AddonDTO addon) {
        final StringBuilder sb = new StringBuilder(MarketplaceAddon.EXT_PREFIX);

        for (final PackageType packageType : PackageType.values()) {
            if (packageType.typeName.equals(addon.packageType)) {
                sb.append(packageType.extType).append("-");
                sb.append(addon.name.replaceAll("[^a-zA-Z0-9_]", ""));
                return sb.toString();
            }
        }
        return null;
    }

    private int toMaturityLevel(final String maturity) {
        switch (maturity) {
            case "Alpha":
                return 0;
            case "Beta":
                return 1;
            case "Production/Stable":
                return 2;
            case "Mature":
                return 3;
            default:
                logger.debug("Unknown maturity level value '{}' - using 'Alpha' instead.", maturity);
                return 0;
        }
    }

    private boolean validName(final String name) {
        return !labelPattern.matcher(name).find();
    }

    private boolean validDescription(final String desc) {
        return !descriptionPattern.matcher(desc).find();
    }

    private String extractExensionId(final URI uri) {
        final Matcher idMatcher = Addon_ID_PATTERN.matcher(uri.getQuery());
        String id = null;
        if (idMatcher.matches() && idMatcher.groupCount() > 1) {
            id = idMatcher.group(1);
        }

        final Optional<Node> AddonNode = getAddonNode(id);

        return AddonNode.isPresent() ? getAddonId(AddonNode.get()) : null;
    }

    private Optional<Node> getAddonNode(final String id) {
        return proxy.getNodes().stream().filter(node -> node != null && node.id.equals(id)).findFirst();
    }
}
