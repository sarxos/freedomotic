/**
 *
 * Copyright (c) 2009-2013 Freedomotic team http://freedomotic.com
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
package com.freedomotic.plugins.devices.knx;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.app.Freedomotic;
import com.freedomotic.events.ProtocolRead;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import com.freedomotic.util.Info;
import java.io.IOException;
import java.net.UnknownHostException;
import com.freedomotic.plugins.devices.knx.ProcComm.ShutdownHandler;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.DatapointMap;
import tuwien.auto.calimero.datapoint.DatapointModel;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLFactory;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

public class Knx4Fd extends Protocol {

    public static final Logger LOG = Logger.getLogger(Knx4Fd.class.getName());
    public final String EIBD_SERVER_ADDRESS = configuration.getStringProperty("eibd-server-address", "127.0.0.1");
    public final String EIBD_SERVER_PORT = configuration.getStringProperty("eibd-server-port", "3671");
    public final String EIBD_SERVER_DATACONTROL = configuration.getStringProperty("eibd-server-datacontrol", "127.0.1.1");
    KNXServer serverObj;
    public NetworkMonitor networkMonitor = null;
    public static DatapointModel m;
    private static final String dataPointsFile = Info.getDevicesPath() + "/knx4fd/datapointMap.xml";
    KnxFrame KnxGui = new KnxFrame(this);

    public Knx4Fd() {
        super("Knx", "/knx4fd/knx4fd-manifest.xml");
        setPollingWait(2000); //waits 2000ms in onRun method before call onRun() again
    }

    protected void onShowGui() {
        bindGuiToPlugin(KnxGui);
    }

    @Override
    public void onStart() {
        //serverObj = new KNXServer(this);
        //try {
        //  if (serverObj.connectToEIBD(EIBD_SERVER_ADDRESS, EIBD_SERVER_PORT)) {
        //    LOG.info("Connected to EIBD server!");
        // }
        //} catch (InterruptedException ex) {
        //  LOG.severe(ex.toString());
        // }
        //NetworkMon();
    }

    @Override
    public void onStop() {
        if (networkMonitor != null) {
            networkMonitor.quit();
        }
    }

    @Override
    protected void onRun() {
        //called in a loop while this plugin is running
        //loops waittime is specified using setPollingWait()
    }

    @Override
    protected void onCommand(Command c) throws IOException, UnableToExecuteException {
        //this method receives freedomotic commands send on channel app.actuators.protocol.arduinousb.in
        //ProcessCommunication("127.0.0.1", "127.0.1.1", "3671", "write", "switch", "off", "0/0/1");
        // try {
        // serverObj.getListener().sendDataToDevice("0/0/1", "on");
        //   serverObj.getListener().sendDataToDevice(c.getProperty("address"), c.getProperty("value"));
        // } catch (KNXException ex) {
        //   LOG.severe(ex.toString());
        // }
        String address = c.getProperty("address");
        String dpt = c.getProperty("dpt");
        String operation = c.getProperty("operation");
        String value = c.getProperty("value");




        String[] args = {"-p", EIBD_SERVER_PORT, "-localhost", EIBD_SERVER_ADDRESS, EIBD_SERVER_DATACONTROL, operation, dpt, value, address};

        try {
            final ProcComm pc = new ProcComm(args);
            final ShutdownHandler sh = pc.new ShutdownHandler().register();
            pc.run();
            sh.unregister();
        } catch (final Throwable t) {
            LOG.severe("ProcessCommunication parsing options error");
        }
    }

    @Override
    protected boolean canExecute(Command c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void NetworkMon() {
        String[] args = {"-p", EIBD_SERVER_PORT, "-localhost", EIBD_SERVER_ADDRESS, EIBD_SERVER_DATACONTROL};

        try {
            // if listener is null, we create our default one
            //final NetworkMonitor m = new NetworkMonitor(args);
            networkMonitor = new NetworkMonitor(args);
            final NetworkMonitor.ShutdownHandler sh = networkMonitor.new ShutdownHandler().register();
            networkMonitor.run();
            sh.unregister();
        } catch (final KNXIllegalArgumentException e) {
            LOG.severe("NetworkMon() error parsing options");
        }
    }

    public static DatapointMap LoadDatapoints(String dataPointsFile) {
        DatapointMap m = new DatapointMap();
        final XMLReader r;
        try {
            r = XMLFactory.getInstance().createXMLReader(dataPointsFile);
            m.load(r);
            r.close();
            return (m);
        } catch (KNXMLException ex) {
            LOG.severe(ex.toString());
            return (m);
        }
    }

    public void autoDiscoveringDevices(DatapointMap m) {
        Collection c = ((DatapointMap) m).getDatapoints();
        Iterator<Datapoint> iterator = c.iterator();
        GroupAddress dpAddress = null;
        String dpName = null;
        String dpt = null;
        ProtocolRead event = null;

        // while loop
        while (iterator.hasNext()) {
            dpAddress = iterator.next().getMainAddress();
            dpName = iterator.next().getName();
            dpt = iterator.next().getDPT();
            System.out.println(dpName + " " + dpAddress + " " + dpt);
            event = new ProtocolRead(this, "knx", dpAddress.toString());
            //event.addProperty("object.class", configuration.getStringProperty(dpt));
            //event.addProperty("object.name", dpName);
            notifyEvent(event);
        }
    }
}