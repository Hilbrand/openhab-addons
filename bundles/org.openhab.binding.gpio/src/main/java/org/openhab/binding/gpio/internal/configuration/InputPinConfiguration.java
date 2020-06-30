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
package org.openhab.binding.gpio.internal.configuration;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.pi4j.io.gpio.PinPullResistance;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class InputPinConfiguration {

    public String pullMode = PullMode.OFF.name();
    public boolean activeLow;

    @Override
    public String toString() {
        return "InputPinConfiguration [pullResistance=" + pullMode + ", active_low=" + activeLow + "]";
    }

    public PinPullResistance getPullMode() {
        try {
            return PullMode.valueOf(pullMode).pullResistance;
        } catch (final IllegalArgumentException e) {
            return PullMode.OFF.pullResistance;
        }

    }

    private enum PullMode {
        OFF(PinPullResistance.OFF),
        PULL_DOWN(PinPullResistance.PULL_DOWN),
        PULL_UP(PinPullResistance.PULL_UP);

        private PinPullResistance pullResistance;

        PullMode(final PinPullResistance pullResistance) {
            this.pullResistance = pullResistance;
        }
    }
}