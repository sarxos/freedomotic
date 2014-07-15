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
/*
 * @author Mauro Cicolella <mcicolella@libero.it>
 */
package com.freedomotic.plugins.devices.coapclient;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.app.Freedomotic;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import java.io.IOException;
import java.util.logging.Logger;

public class CoapClient4FD
        extends Protocol {

    public static final Logger LOG = Logger.getLogger(CoapClient4FD.class.getName());
    private String BROKER_URL = configuration.getStringProperty("broker-url", "tcp://test.mosquitto.org:1883");
    private String AUTHENTICATION_ENABLED = configuration.getStringProperty("authentication-enabled", "false");
    private String USERNAME = configuration.getStringProperty("username", "admin");
    private String PASSWORD = configuration.getStringProperty("password", "admin");
    private Coap coapClient = null;

    public CoapClient4FD() {
        //every plugin needs a name and a manifest XML file
        super("CoAP Client", "/coap-client/coap-client-manifest.xml");
        setPollingWait(-1); //onRun() disabled
    }

    @Override
    protected void onShowGui() {
        //bindGuiToPlugin(new HelloWorldGui(this));
    }

    @Override
    protected void onHideGui() {
    }

    @Override
    protected void onRun() {
    }

    @Override
    protected void onStart() {
        coapClient = new Coap(BROKER_URL, this);
        coapClient.startClient();
    }

    @Override
    protected void onStop() {

        LOG.info("CoAP Client stopped");
    }

    @Override
    protected void onCommand(Command c)
            throws IOException, UnableToExecuteException {
        String topic = c.getProperty("topic");
        String message = c.getProperty("message");
        Integer subQoS = Integer.parseInt(c.getProperty("sub-qos"));
        Integer pubQoS = Integer.parseInt(c.getProperty("pub-qos"));

    }

    @Override
    protected boolean canExecute(Command c) {
        //don't mind this method for now
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        //don't mind this method for now
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
