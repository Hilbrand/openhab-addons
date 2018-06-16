/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.connector;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface DSMRPortListener {

    /**
     * Callback for DSMRPortEvent events
     *
     * @param portEvent {@link DSMRPortErrorEvent} that has occurred
     */
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent);

    /**
     *
     * @param buffer
     * @param length
     */
    void handleData(byte[] buffer, int length);
}
