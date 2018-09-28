/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.p1telegram;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface TelegramParser {

    /**
     *
     * @param data byte data to parse
     * @param length number of bytes to parse
     */
    void parse(byte[] data, int length);

    /**
     * Reset the current telegram state.
     */
    void reset();

    /**
     * @param lenientMode the lenientMode to set
     */
    void setLenientMode(boolean lenientMode);
}
