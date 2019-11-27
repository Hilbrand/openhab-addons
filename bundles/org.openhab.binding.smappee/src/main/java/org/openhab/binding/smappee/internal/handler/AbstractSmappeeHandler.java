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
package org.openhab.binding.smappee.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.openhab.binding.smappee.internal.service.SmappeeService;

/**
 * The {@link AbstractSmappeeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
abstract class AbstractSmappeeHandler extends BaseThingHandler {

    protected AbstractSmappeeHandler(final Thing thing) {
        super(thing);
    }

    protected @Nullable SmappeeHandler getBridgeHandler() {
        return (SmappeeHandler) this.getBridge().getHandler();
    }

    protected @Nullable SmappeeService getSmappeeService() {
        final SmappeeHandler smappeeHandler = getBridgeHandler();

        if (smappeeHandler == null) {
            return null;
        }

        return smappeeHandler.getSmappeeService();
    }
}
