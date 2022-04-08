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
package org.openhab.binding.ohsystemmanagement.internal;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.openhab.binding.ohsystemmanagement.internal.dto.OperatingSystemDTO;
import org.openhab.binding.ohsystemmanagement.internal.dto.ThreadInfoDTO;
import org.osgi.service.component.annotations.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Component(configurationPid = "oh.system")
@Path(ThreadsResource.PATH_SYSTEM)
// @RolesAllowed({ Role.ADMIN })
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(value = ThreadsResource.PATH_SYSTEM)
@NonNullByDefault
public class ThreadsResource implements RESTResource {
    static final String PATH_SYSTEM = "system";

    @GET
    @Path("/threads")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all threads.", response = ThreadInfoDTO.class, responseContainer = "Map")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getThreads() {
        final Map<String, List<ThreadInfoDTO>> dtos = Stream
                .of(ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)) //
                .filter(dto -> dto.getThreadName().startsWith("OH-")).map(ThreadInfoDTO::build) //
                .collect(Collectors.groupingBy(this::threadGroup));

        return Response.ok(dtos).build();
    }

    private String threadGroup(final ThreadInfoDTO dto) {
        final String threadName = dto.getThreadName();
        final int lastIndexOf = threadName.lastIndexOf('-');
        final int firstIndexOf = threadName.indexOf('-');

        return lastIndexOf < 0 || firstIndexOf == lastIndexOf ? threadName : threadName.substring(0, lastIndexOf);
    }

    @GET
    @Path("/operatingSystem")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the Operating System info.", response = OperatingSystemDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getOperationSystem() {
        return Response.ok(OperatingSystemDTO.build(ManagementFactory.getOperatingSystemMXBean())).build();
    }


    @GET
    @Path("/systemLoadAverage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get system load average of last minute.", response = OperatingSystemDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getSystemSoadAverage() {
        return Response.ok(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()).build();
    }

}
