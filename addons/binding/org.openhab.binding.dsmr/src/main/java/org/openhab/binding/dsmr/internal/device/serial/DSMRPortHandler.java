/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.serial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dsmr.internal.device.DSMRPortEventListener;

/**
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface DSMRPortHandler extends DSMRPortEventListener {

    void push(byte[] buffer);

    /**
     * Callback for DSMRPortEvent events
     *
     * @param portEvent {@link DSMRPortEvent} that has occurred
     */
    @Override
    public void handlePortErrorEvent(DSMRPortEvent portEvent);

}
