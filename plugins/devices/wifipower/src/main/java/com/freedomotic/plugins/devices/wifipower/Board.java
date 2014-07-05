/**
 *
 * Copyright (c) 2009-2014 Freedomotic team http://freedomotic.com
 *
 * This file is part of Freedomotic
 *
 * This Program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This Program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Freedomotic; see the file COPYING. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.freedomotic.plugins.devices.wifipower;

public class Board {

    private String ipAddress = null;
    private String relayTag;
    private String autoConfiguration;
    private String objectClass;
    private String alias = null;
    private String monitorRelay = null;
    private String authentication = null;
    private String username = null;
    private String password = null;
    private int port;
    private int relayNumber;
    private int startingRelay;
    private int[] relayStatus;

    public Board(String ipAddress, int port, String alias, int relayNumber,
            int startingRelay, String relayTag, String autoConfiguration, String objectClass,
            String monitorRelay, String authentication, String username, String password) {
        setIpAddress(ipAddress);
        setPort(port);
        setAlias(alias);
        setRelayNumber(relayNumber);
        setStartingRelay(startingRelay);
        setAuthentication(authentication);
        setUsername(username);
        setPassword(password);
        setRelayTag(relayTag);
        setAutoConfiguration(autoConfiguration);
        setObjectClass(objectClass);
        setMonitorRelay(monitorRelay);
        initializeRelayStatus(relayNumber);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRelayTag() {
        return relayTag;
    }

    public void setRelayTag(String relayTag) {
        this.relayTag = relayTag;
    }

    public void setMonitorRelay(String monitorRelay) {
        this.monitorRelay = monitorRelay;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getRelayNumber() {
        return relayNumber;
    }

    public void setRelayNumber(int relayNumber) {
        this.relayNumber = relayNumber;
    }

    public int getStartingRelay() {
        return startingRelay;
    }

    public void setStartingRelay(int startingRelay) {
        this.startingRelay = startingRelay;
    }

    public String getAutoConfiguration() {
        return autoConfiguration;
    }

    public String getMonitorRelay() {
        return monitorRelay;
    }

    public void setAutoConfiguration(String autoConfiguration) {
        this.autoConfiguration = autoConfiguration;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public int getRelayStatus(int relayNumber) {
        return relayStatus[relayNumber];
    }

    public void setRelayStatus(int relayNumber, int value) {
        relayStatus[relayNumber] = value;
    }

    private void initializeRelayStatus(int relayNumber) {
        relayStatus = new int[relayNumber];
        for (int i = 0; i < relayNumber; i++) {
            relayStatus[i] = -1;
        }
    }
}
