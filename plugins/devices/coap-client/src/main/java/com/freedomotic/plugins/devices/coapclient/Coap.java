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
package com.freedomotic.plugins.devices.coapclient;

/*
 * @author Mauro Cicolella <mcicolella@libero.it>
 */
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

public class Coap {

    String brokerUrl = null;
    CoapClient4FD pluginRef = null;
    CoapClient client = null;

    Coap(String brokerUrl, CoapClient4FD pluginRef) {
        this.brokerUrl = brokerUrl;
        this.pluginRef = pluginRef;
    }

    public void startClient() {
        client = new CoapClient(brokerUrl);
        CoapResponse response = client.get();
        if (response!= null) {
            System.out.println(response.getCode());
            System.out.println(response.getOptions());
            System.out.println(response.getResponseText());
        } else {
            System.out.println("Request failed");

        }
    }
}
