/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;

public class ThreadInfoDTO {
    private String threadName;
    private long threadId;
    private long blockedTime;
    private long blockedCount;
    private long waitedTime;
    private long waitedCount;
    private LockInfo lock;
    private String lockName;
    private long lockOwnerId;
    private String lockOwnerName;
    private boolean inNative;
    private boolean suspended;
    private Thread.State threadState;
    private StackTraceElement[] stackTraceElements;

    public static final ThreadInfoDTO build(final ThreadInfo threadInfo) {
        final ThreadInfoDTO dto = new ThreadInfoDTO();
        dto.threadName = threadInfo.getThreadName();
        dto.threadId = threadInfo.getThreadId();
        dto.blockedTime = threadInfo.getBlockedTime();
        dto.blockedCount = threadInfo.getBlockedCount();
        dto.waitedTime = threadInfo.getWaitedTime();
        dto.waitedCount = threadInfo.getWaitedCount();
        // private LockInfo lock;
        dto.lockName = threadInfo.getLockName();
        dto.lockOwnerId = threadInfo.getLockOwnerId();
        dto.lockOwnerName = threadInfo.getLockOwnerName();
        dto.inNative = threadInfo.isInNative();
        dto.suspended = threadInfo.isSuspended();
        dto.threadState = threadInfo.getThreadState();
        dto.stackTraceElements = threadInfo.getStackTrace();

        return dto;
    }

    public String getThreadName() {
        return threadName;
    }

    public StackTraceElement[] getStackTraceElements() {
        return stackTraceElements;
    }
}
