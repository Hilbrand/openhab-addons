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
 * There was an error while communicating with the smappee API.
 * This will be mostly temporary communication errors.
 *
 * @author Niko Tanghe - Initial contribution
 */
@NonNullByDefault
public class CommunicationException extends Exception {

    private static final long serialVersionUID = -1397248504578142737L;

    public CommunicationException(final String message) {
        super(message);
    }

    public CommunicationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
