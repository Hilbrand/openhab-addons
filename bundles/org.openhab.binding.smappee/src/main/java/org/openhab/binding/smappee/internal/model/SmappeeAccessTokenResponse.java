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
package org.openhab.binding.smappee.internal.model;

/**
 * Interface for OAuth access token response
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeAccessTokenResponse {

    public String access_token;
    public int expires_in;
    public String refresh_token;
}
