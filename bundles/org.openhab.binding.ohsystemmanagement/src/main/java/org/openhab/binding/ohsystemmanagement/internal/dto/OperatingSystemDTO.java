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
package org.openhab.binding.ohsystemmanagement.internal.dto;

import java.lang.management.OperatingSystemMXBean;

public class OperatingSystemDTO {

    private String arch;
    private int availableProcessors;
    private String name;
    private double systemLoadAverage;
    private String version;

    public static OperatingSystemDTO build(final OperatingSystemMXBean operatingSystemMXBean) {
        final OperatingSystemDTO dto = new OperatingSystemDTO();
        dto.arch = operatingSystemMXBean.getArch();
        dto.availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        dto.name = operatingSystemMXBean.getName();
        dto.systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();
        dto.version = operatingSystemMXBean.getVersion();
        return dto;
    }
}
