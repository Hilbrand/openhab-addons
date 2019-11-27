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
package org.openhab.binding.smappee.internal.service;

/**
 * Contains the configuration parameters for the smappee device.
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeConfigurationParameters {

    /** The Smappee Api Oauth client id (obtain by mail from smappee support). */
    public String clientId;

    /** The Smappee Api Oauth client secret (obtain by mail from smappee support). */
    public String clientSecret;

    /** The username for your Smappee. */
    public String username;

    /** The password for your Smappee. */
    public String password;

    /** The name of your Smappee installation. */
    public String serviceLocationName;

    /** How often (in minutes) does the smappee needs to be checked ? */
    public int pollingInterval;
}
