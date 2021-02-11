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
package org.openhab.extensionservice.marketplace.automation.internal;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.storage.Storage;
import org.openhab.addonservice.marketplace.MarketplaceAddon;
import org.openhab.addonservice.marketplace.MarketplaceAddonHandler;
import org.openhab.addonservice.marketplace.MarketplaceHandlerException;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles rule templates as JSON files and installs
 * them by adding them to a {@link Storage}. The templates are then served from this storage through a dedicated
 * {@link RuleTemplateProvider}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Component
public class AutomationExtensionHandler implements MarketplaceAddonHandler {

    private final Logger logger = LoggerFactory.getLogger(AutomationExtensionHandler.class);

    private MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider;

    @Reference
    protected void setMarketplaceRuleTemplateProvider(MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider) {
        this.marketplaceRuleTemplateProvider = marketplaceRuleTemplateProvider;
    }

    protected void unsetMarketplaceRuleTemplateProvider(
            MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider) {
        this.marketplaceRuleTemplateProvider = null;
    }

    @Override
    public boolean supports(MarketplaceAddon ext) {
        // we support only rule templates in JSON format so far
        return ext.getType().equals(MarketplaceAddon.EXT_TYPE_RULE_TEMPLATE)
                && ext.getPackageFormat().equals(MarketplaceAddon.EXT_FORMAT_JSON);
    }

    @Override
    public boolean isInstalled(MarketplaceAddon ext) {
        return marketplaceRuleTemplateProvider.get(ext.getId()) != null;
    }

    @Override
    public void install(MarketplaceAddon ext) throws MarketplaceHandlerException {
        String url = ext.getDownloadUrl();
        try {
            String template = getTemplate(url);
            marketplaceRuleTemplateProvider.addTemplateAsJSON(ext.getId(), template);
        } catch (IOException e) {
            logger.error("Rule template from marketplace cannot be downloaded: {}", e.getMessage());
            throw new MarketplaceHandlerException("Template cannot be downloaded.");
        } catch (Exception e) {
            logger.error("Rule template from marketplace is invalid: {}", e.getMessage());
            throw new MarketplaceHandlerException("Template is not valid.");
        }
    }

    @Override
    public void uninstall(MarketplaceAddon ext) throws MarketplaceHandlerException {
        marketplaceRuleTemplateProvider.remove(ext.getId());
    }

    private String getTemplate(String urlString) throws IOException {
        URL url = new URL(urlString);
        return IOUtils.toString(url);
    }
}
