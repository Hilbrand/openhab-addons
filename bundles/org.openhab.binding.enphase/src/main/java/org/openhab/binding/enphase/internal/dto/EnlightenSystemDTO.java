/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

/**
 * Data class for Enphase Enlighten Portal.
 *
 * @author Joe Inkenbrandt - Initial contribution
 */
public class EnlightenSystemDTO {
    private long system_id;
    private String system_name;
    private String system_public_name;
    private String status;
    private String timezone;
    private String country;
    private String state;
    private String city;
    private String postal_code;
    private String connection_type;

    public class EnglightenSystemMetaDTO {
        private String status;
        private long last_report_at;
        private long last_energy_at;
        private long operational_at;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getLast_report_at() {
            return last_report_at;
        }

        public void setLast_report_at(long last_report_at) {
            this.last_report_at = last_report_at;
        }

        public long getLast_energy_at() {
            return last_energy_at;
        }

        public void setLast_energy_at(long last_energy_at) {
            this.last_energy_at = last_energy_at;
        }

        public long getOperational_at() {
            return operational_at;
        }

        public void setOperational_at(long operational_at) {
            this.operational_at = operational_at;
        }
    }

    private EnglightenSystemMetaDTO meta;

    public long getSystem_id() {
        return system_id;
    }

    public void setSystem_id(long system_id) {
        this.system_id = system_id;
    }

    public String getSystem_name() {
        return system_name;
    }

    public void setSystem_name(String system_name) {
        this.system_name = system_name;
    }

    public String getSystem_public_name() {
        return system_public_name;
    }

    public void setSystem_public_name(String system_public_name) {
        this.system_public_name = system_public_name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostal_code() {
        return postal_code;
    }

    public void setPostal_code(String postal_code) {
        this.postal_code = postal_code;
    }

    public String getConnection_type() {
        return connection_type;
    }

    public void setConnection_type(String connection_type) {
        this.connection_type = connection_type;
    }

    public EnglightenSystemMetaDTO getMeta() {
        return meta;
    }

    public void setMeta(EnglightenSystemMetaDTO meta) {
        this.meta = meta;
    }
}
