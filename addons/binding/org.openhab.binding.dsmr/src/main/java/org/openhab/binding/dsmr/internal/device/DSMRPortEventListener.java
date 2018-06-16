/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dsmr.internal.device.connector.DSMRPortErrorEvent;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;

/**
 * Interface for handling DSMRPortEvent events
 *
 * @author M. Volaart - Initial contribution
 */
@NonNullByDefault
public interface DSMRPortEventListener {
    /**
     * Callback for DSMRPortEvent events
     *
     * @param portEvent {@link DSMRPortErrorEvent} that has occurred
     */
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent);

    /**
     * Callback for received P1 telegrams
     *
     * @param cosemObjects List containing the individual data elements of a received P1 telegram
     * @param telegramDetails the details about the received telegram
     */
    public void handleTelegramReceived(List<CosemObject> cosemObjects, String telegramDetails);
}
