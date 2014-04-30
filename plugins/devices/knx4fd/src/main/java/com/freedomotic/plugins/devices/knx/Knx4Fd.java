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
import com.freedomotic.objects.EnvObjectLogic;
import com.freedomotic.objects.EnvObjectPersistence;
import com.freedomotic.reactions.Command;
import com.freedomotic.util.Info;
import java.io.IOException;
import java.net.UnknownHostException;
import com.freedomotic.plugins.devices.knx.ProcComm.ShutdownHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.datapoint.*;
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
    public final String DATAPOINTS_FILE = configuration.getStringProperty("datapoints-file", "/knx4fd/datapointMap.xml");
    public final String PROTOCOL_NAME = configuration.getStringProperty("protocol.name", "knx");
    public GroupMonitor groupMonitor = null;
    public DatapointModel datapointModel;
    public DatapointMap datapointMap;
    private final String dataPointsFile = Info.getDevicesPath() + DATAPOINTS_FILE;
    HashMap<String, String> knxGroupAddressMap = new HashMap<String, String>();
    KnxFrame KnxGui = new KnxFrame(this);

    public Knx4Fd() {
        super("Knx", "/knx4fd/knx4fd-manifest.xml");
        setPollingWait(-1);
    }

    protected void onShowGui() {
        bindGuiToPlugin(KnxGui);
    }

    @Override
    public void onStart() {
        //datapointMap = LoadDatapoints(dataPointsFile);
        //initialization(datapointMap);
        //
    }

    @Override
    public void onStop() {
        if (groupMonitor != null) {
            groupMonitor.quit();
        }
    }

    @Override
    protected void onRun() {
        knxGroupAddressMap();
        GroupMon();

    }

    @Override
    protected void onCommand(Command c) throws IOException, UnableToExecuteException {

        String address = Utilities.extractMainAddress(c.getProperty("address"));
        String dpt = Utilities.extractDTP(c.getProperty("address"));
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

    public void GroupMon() {
        String[] args = {"-p", EIBD_SERVER_PORT, "-localhost", EIBD_SERVER_ADDRESS, EIBD_SERVER_DATACONTROL};

        try {
            // if listener is null, we create our default one
            groupMonitor = new GroupMonitor(args, this);
            final GroupMonitor.ShutdownHandler sh = groupMonitor.new ShutdownHandler().register();
            groupMonitor.run();
            sh.unregister();
        } catch (final KNXIllegalArgumentException e) {
            LOG.severe("groupMon() error parsing options");
        }
    }

    public DatapointMap LoadDatapoints(String dataPointsFile) {
        DatapointMap datapointMap = new DatapointMap();
        final XMLReader r;
        try {
            r = XMLFactory.getInstance().createXMLReader(dataPointsFile);
            datapointMap.load(r);
            r.close();
            return (datapointMap);
        } catch (KNXMLException ex) {
            LOG.severe(ex.toString());
            return (datapointMap);
        }
    }

    public String getObjectAddress(String groupAddress) {
        return (knxGroupAddressMap.get(groupAddress));

    }

    public void knxGroupAddressMap() {
        ArrayList<EnvObjectLogic> objectsList = EnvObjectPersistence.getObjectByProtocol(configuration.getStringProperty("protocol.name", "knx"));
        Iterator iterator = objectsList.iterator();
        if (!objectsList.isEmpty()) {
            while (iterator.hasNext()) {
                EnvObjectLogic e = (EnvObjectLogic) iterator.next();
                // maps main address
                knxGroupAddressMap.put(Utilities.extractMainAddress(e.getPojo().getPhisicalAddress()), e.getPojo().getPhisicalAddress());
                // extracts substring [] containing the list of updating addresses
                String updatingAddresses = StringUtils.substringBetween(e.getPojo().getPhisicalAddress(), "[", "]");;
                if (updatingAddresses.length() > 0) {
                    String updAddress[] = updatingAddresses.split(",");
                    for (Integer i = 0; i < updAddress.length; i++) {
                        // adds any updating address to the map linking it to the Freedomotic object physical address
                        knxGroupAddressMap.put(updAddress[i], e.getPojo().getPhisicalAddress());
                    }
                }

            }
        }
    }

    public void initialization(DatapointMap datapointMap) {
        Collection c = datapointMap.getDatapoints();
        Iterator iterator = c.iterator();
        GroupAddress dptAddress = null;
        String dptName = null;
        String dptDPT = null;
        String address = null;
        ProtocolRead event = null;

        // while loop
        while (iterator.hasNext()) {
            if (iterator.next() instanceof StateDP) {
                StateDP dpt = (StateDP) iterator.next();
                dptName = dpt.getName();

                address = dpt.getMainAddress() + "" + dpt.getAddresses(true) + dpt.getDPT();
                System.out.println("DPT " + dptName + " " + address + " main address: " + Utilities.extractMainAddress(address) + " DTP:" + Utilities.extractDTP(address));

            } else {
                CommandDP dpt = (CommandDP) iterator.next();
                dptName = dpt.getName();
                dptAddress = dpt.getMainAddress();
                dptDPT = dpt.getDPT();

                System.out.println("DPT " + dptName + " " + dptAddress + " " + dptDPT);


            }


        }
    }

    public void notifyChanges(String objectAddress, String value) {
        ProtocolRead event = new ProtocolRead(this, PROTOCOL_NAME, objectAddress);
        //event.addProperty("object.class", configuration.getStringProperty(dpt));
        //event.addProperty("object.name", dpName);
        event.addProperty("value", value);
        notifyEvent(event);
    }
}