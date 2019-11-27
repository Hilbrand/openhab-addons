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
package org.openhab.binding.smappee.internal.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * There was an error in the smappee configuration
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class InvalidConfigurationException extends Exception {

    private static final long serialVersionUID = -1359248384278371957L;

    public InvalidConfigurationException(final String message) {
        super(message);
    }
}
