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
 * Where is the Smappee located ? The response
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeServiceLocationResponse {

    public String appname;

    public SmappeeServiceLocation[] serviceLocations;
}
