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

import static org.openhab.binding.smappee.internal.SmappeeBindingConstants.PARAMETER_ACTUATOR_ID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.smappee.internal.service.SmappeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SmappeeActuatorHandler} is responsible for handling commands and sets the actual status for an actuator.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class SmappeeActuatorHandler extends AbstractSmappeeHandler {

    private final Logger logger = LoggerFactory.getLogger(SmappeeActuatorHandler.class);

    private @NonNullByDefault({}) String applianceId;

    public SmappeeActuatorHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        final SmappeeService smappeeService = getSmappeeService();
        if (smappeeService == null || !smappeeService.isInitialized()) {
            return;
        }

        if (command instanceof RefreshType) {

        } else if (command instanceof OnOffType) {
            smappeeService.putPlugOnOff(applianceId, command == OnOffType.ON);
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    @Override
    public void initialize() {
        applianceId = thing.getConfiguration().get(PARAMETER_ACTUATOR_ID).toString();
    }
}
