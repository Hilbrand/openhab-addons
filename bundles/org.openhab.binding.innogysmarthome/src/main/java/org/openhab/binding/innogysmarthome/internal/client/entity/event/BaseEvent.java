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
package org.openhab.binding.innogysmarthome.internal.client.entity.event;

import java.util.Set;

import com.google.api.client.util.Key;
import com.google.common.collect.ImmutableSet;

/**
 * @author Oliver Kuhl - Initial contribution
 *
 */
public class BaseEvent {

    public static final String TYPE_STATE_CHANGED = "StateChanged";// "device/SHC.RWE/1.0/event/StateChanged";
    public static final String TYPE_NEW_MESSAGE_RECEIVED = "NewMessageReceived"; // "device/SHC.RWE/1.0/event/NewMessageReceived";
    public static final String TYPE_MESSAGE_CREATED = "MessageCreated";
    public static final String TYPE_MESSAGE_DELETED = "MessageDeleted"; // "device/SHC.RWE/1.0/event/MessageDeleted";
    public static final String TYPE_DISCONNECT = "Disconnect"; // "/event/Disconnect";
    public static final String TYPE_CONFIGURATION_CHANGED = "ConfigurationChanged"; // "device/SHC.RWE/1.0/event/ConfigChanged";
    public static final String TYPE_CONTROLLER_CONNECTIVITY_CHANGED = "/event/ControllerConnectivityChanged"; // "device/SHC.RWE/1.0/event/ControllerConnectivityChanged";
    public static final String TYPE_BUTTON_PRESSED = "ButtonPressed";

    public static final Set<String> SUPPORTED_EVENT_TYPES = ImmutableSet.of(TYPE_STATE_CHANGED,
            TYPE_NEW_MESSAGE_RECEIVED, TYPE_MESSAGE_CREATED, TYPE_MESSAGE_DELETED, TYPE_DISCONNECT, TYPE_CONFIGURATION_CHANGED,
            TYPE_CONTROLLER_CONNECTIVITY_CHANGED, TYPE_BUTTON_PRESSED);

    /**
     * The event sequence number – the gateway keeps track and adds a sequence number to each event for the client to
     * identify order and missing events
     */
    @Key("sequenceNumber")
    private Integer sequenceNumber;

    /**
     * Specifies the type of the event. The type must be the full path to uniquely reference the event definition.
     * Always available.
     */
    @Key("type")
    private String type;

    /**
     * Date and time when the event occurred in the system. Always available.
     */
    @Key("timestamp")
    private String timestamp;

    /**
     * @return the sequenceNumber
     */
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * @return the timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param sequenceNumber the sequenceNumber to set
     */
    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns true, if the {@link Event} is a ConfigChanged event.
     *
     * @return
     */
    public boolean isConfigChangedEvent() {
        return getType().equals(TYPE_CONFIGURATION_CHANGED);
    }

    /**
     * Returns true, if the {@link Event} is a ControllerConnectivityChanged event.
     *
     * @return
     */
    public boolean isControllerConnectivityChangedEvent() {
        return getType().equals(TYPE_CONTROLLER_CONNECTIVITY_CHANGED);
    }

    /**
     * Returns true, if the {@link Event} is a Disconnect event.
     *
     * @return
     */
    public boolean isDisconnectedEvent() {
        return getType().equals(TYPE_DISCONNECT);
    }

    /**
     * Returns true, if the {@link Event} is a MessageDeletedEvent.
     *
     * @return
     */
    public boolean isMessageDeletedEvent() {
        return getType().equals(TYPE_MESSAGE_DELETED);
    }

    /**
     * Returns true, if the {@link Event} is a NewMessageReceivedEvent.
     *
     * @return
     */
    public boolean isNewMessageReceivedEvent() {
        return getType().equals(TYPE_NEW_MESSAGE_RECEIVED);
    }

    /**
     * Returns true, if the {@link Event} is a StateChangedEvent.
     *
     * @return
     */
    public boolean isStateChangedEvent() {
        return getType().equals(TYPE_STATE_CHANGED);
    }

}
