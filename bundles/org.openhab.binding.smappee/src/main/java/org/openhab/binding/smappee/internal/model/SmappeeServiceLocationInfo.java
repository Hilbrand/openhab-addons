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
 * The result of a service location reading
 * This will list the detected appliances and actuators (plugs)
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeServiceLocationInfo {

    public int serviceLocationId;
    public String name;
    public String timezone;
    public String lon;
    public String lat;
    public String electricityCost;
    public String electricityCurrency;
    public SmappeeServiceLocationInfoAppliance[] appliances;
    public SmappeeServiceLocationInfoActuator[] actuators;
    public SmappeeServiceLocationInfoSensor[] sensors;
}
