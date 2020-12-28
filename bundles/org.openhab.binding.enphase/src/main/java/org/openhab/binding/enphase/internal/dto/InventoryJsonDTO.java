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
package org.openhab.binding.enphase.internal.dto;

import com.google.gson.annotations.SerializedName;

/**
 * @author Hilbrand Bouwkamp - Initial contribution
 */
public class InventoryJsonDTO {

    public class DeviceDTO {
        private String type;

        @SerializedName("part_num")
        private String partNumber;
        @SerializedName("serial_num")
        private String serialNumber;

        @SerializedName("device_status")
        private String[] deviceStatus;
        @SerializedName("last_rpt_date")
        private String lastReportDate;
        private boolean producing;
        private boolean communicating;
        private boolean provisioned;
        private boolean operating;
        // NSRB data
        private String relay;
        @SerializedName("line1-connected")
        private boolean line1Connected;
        @SerializedName("line2-connected")
        private boolean line2Connected;
        @SerializedName("line3-connected")
        private boolean line3Connected;

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(final String type) {
            this.type = type;
        }

        /**
         * @return the part_num
         */
        public String getPartNum() {
            return partNumber;
        }

        /**
         * @return the serial_num
         */
        public String getSerialNumber() {
            return serialNumber;
        }

        /**
         * @return the device_status
         */
        public String getDeviceStatus() {
            return deviceStatus == null || deviceStatus.length == 0 ? "" : deviceStatus[0];
        }

        /**
         * @return the producing
         */
        public boolean isProducing() {
            return producing;
        }

        /**
         * @return the communicating
         */
        public boolean isCommunicating() {
            return communicating;
        }

        /**
         * @return the provisioned
         */
        public boolean isProvisioned() {
            return provisioned;
        }

        /**
         * @return the operating
         */
        public boolean isOperating() {
            return operating;
        }

        /**
         * @return the relay
         */
        public String getRelay() {
            return relay;
        }

        /**
         * @return the line1Connected
         */
        public boolean isLine1Connected() {
            return line1Connected;
        }

        /**
         * @return the line2Connected
         */
        public boolean isLine2Connected() {
            return line2Connected;
        }

        /**
         * @return the line3Connected
         */
        public boolean isLine3Connected() {
            return line3Connected;
        }
    }

    public String type;
    public DeviceDTO[] devices;
}
