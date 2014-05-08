/*
 * Calimero 2 - A library for KNX network access Copyright (c) 2006, 2011 B.
 * Malinowsky
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.freedomotic.plugins.devices.knx;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.Settings;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkFT12;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogStreamWriter;
import tuwien.auto.calimero.log.LogWriter;

/**
 * A tool for Calimero allowing monitoring of KNX group communication.
 * 
* @author B. Malinowsky, B. Haumacher
 */
public class GroupMonitor implements Runnable {

    private static final short GROUP_READ = 0x00;
    private static final short GROUP_RESPONSE = 0x40;
    private static final short GROUP_WRITE = 0x80;
    private Knx4Fd pluginRef;
    private final Map<String, Object> options = new HashMap<String, Object>();
    private KNXNetworkLink link;
    private final NetworkLinkListener l = new NetworkLinkListener() {

        @Override
        public void linkClosed(CloseEvent e) {
            Knx4Fd.LOG.info("Network monitor closed (" + e.getReason() + ")");
            synchronized (GroupMonitor.this) {
                GroupMonitor.this.notify();
            }
        }

        @Override
        public void indication(FrameEvent e) {
            GroupMonitor.this.onIndication(e);
        }

        @Override
        public void confirmation(FrameEvent e) {
            GroupMonitor.this.onConfirmation(e);
        }
    };

    /**
     * Creates a new {@link GroupMonitor} instance using the supplied options.
     * <p> See {@link #main(String[])} for a list of options.
     *
     * @param args list with options
     * @throws KNXIllegalArgumentException on unknown/invalid options
     */
    public GroupMonitor(final String[] args, Knx4Fd pluginRef) {
        this.pluginRef = pluginRef;

        try {
            // read the command line options
            parseOptions(args);
        } catch (final KNXIllegalArgumentException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new KNXIllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        Exception thrown = null;
        boolean canceled = false;
        try {
            start();

            // just wait for the network monitor to quit
            synchronized (this) {
                while (link != null && link.isOpen()) {
                    wait(500);
                }
            }
        } catch (final InterruptedException e) {
            canceled = true;
            Thread.currentThread().interrupt();
        } catch (final KNXException e) {
            thrown = e;
        } catch (final RuntimeException e) {
            thrown = e;
        } finally {
            quit();
            onCompletion(thrown, canceled);
        }
    }

    /**
     * Starts the network monitor. <p> This method returns after the network
     * monitor was started.
     *
     * @throws KNXException on problems creating or connecting the monitor
     * @throws InterruptedException on interrupted thread
     */
    public void start() throws KNXException, InterruptedException {
        link = createLink();
        // listen to monitor link events
        link.addLinkListener(l);
    }

    /**
     * Quits a running network monitor, otherwise returns immediately. <p>
     */
    public void quit() {
        if (link != null && link.isOpen()) {
            link.close();
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Called by this tool on receiving a monitor indication frame. <p>
     *
     * @param e the frame event
     */
    protected void onIndication(final FrameEvent e) {
        frameReceived(e);
    }

    /**
     * Called by this tool on receiving a monitor confirmation frame. <p>
     *
     * @param e the frame event
     */
    protected void onConfirmation(final FrameEvent e) {
        frameReceived(e);
    }

    public void frameReceived(FrameEvent e) {
        try {
            CEMILData frame = (CEMILData) e.getFrame();
            final byte[] apdu = frame.getPayload();
            byte[] asdu;

            int service = DataUnitBuilder.getAPDUService(apdu);

            String svc;

            if (service == GROUP_READ) {
                asdu = new byte[0];
                svc = "READ";
            } else if (service == GROUP_WRITE) {
                asdu = DataUnitBuilder.extractASDU(apdu);
                svc = "WRITE";
            } else if (service == GROUP_RESPONSE) {
                asdu = DataUnitBuilder.extractASDU(apdu);
                svc = "RESPONSE";
            } else {
                asdu = new byte[0];
                svc = "UNSUPPORTED";
                Knx4Fd.LOG.info("Unsupported APDU service - ignored, service code = 0x" + Integer.toHexString(service));
            }
            String msg =
                    Utilities.getDate() + "" + " Frame received from "
                    + frame.getSource() 
                    + " to: " + frame.getDestination() 
                    + " ASDU (hex): " + Utilities.getHexString(asdu) 
                    + " SVC: " + svc;

            msg = msg + " Found object " + pluginRef.getObjectAddress(frame.getDestination().toString());
            // starts value translation based on DTP type
            String DPT = pluginRef.getObjectAddress(frame.getDestination().toString());
            Integer mainNumber = Integer.valueOf(DPT.substring(0,1));
            DPTXlator translator = TranslatorTypes.createTranslator(mainNumber, Utilities.extractDTP(pluginRef.getObjectAddress(frame.getDestination().toString())));
            translator.setData(asdu);
            String value = translator.getValue();
            msg = msg + " value translated " + value;
            pluginRef.notifyChanges(pluginRef.getObjectAddress(frame.getDestination().toString()), DPT, value, null, null);
            Knx4Fd.LOG.info(msg);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

  
    /**
     * Called by this tool on completion. <p>
     *
     * @param thrown the thrown exception if operation completed due to an
     * raised exception,
     * <code>null</code> otherwise
     * @param canceled whether the operation got canceled before its planned end
     */
    protected void onCompletion(final Exception thrown, final boolean canceled) {
        if (canceled) {
            Knx4Fd.LOG.info("GroupMonitor stopped");
        }
        if (thrown != null) {
            Knx4Fd.LOG.severe(thrown.getMessage() != null ? thrown.getMessage() : thrown.getClass().getName());
        }
    }

    /**
     * Creates the KNX network link to access the network specified in
     * <code>options</code>. <p>
     *
     * @return the KNX network monitor link
     * @throws KNXException on problems on link creation
     * @throws InterruptedException on interrupted thread
     */
    private KNXNetworkLink createLink() throws KNXException, InterruptedException {
        final KNXMediumSettings medium = (KNXMediumSettings) options.get("medium");
        if (options.containsKey("serial")) {
            // create FT1.2 monitor link
            final String p = (String) options.get("serial");
            try {
                return new KNXNetworkLinkFT12(Integer.parseInt(p), medium);
            } catch (final NumberFormatException e) {
                return new KNXNetworkLinkFT12(p, medium);
            }
        }
        // create local and remote socket address for monitor link
        InetAddress localHost = (InetAddress) options.get("localhost");
        Integer localPort = (Integer) options.get("localport");
        final InetSocketAddress local = Utilities.createLocalSocket(localHost, localPort);
        final InetSocketAddress host = new InetSocketAddress((InetAddress) options.get("host"),
                ((Integer) options.get("port")).intValue());
        // create the monitor link, based on the KNXnet/IP protocol
        // specify whether network address translation shall be used,
        // and tell the physical medium of the KNX network
        return new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, local, host, options.containsKey("nat"), medium);
    }

    /**
     * Reads all options in the specified array, and puts relevant options into
     * the supplied options map. <p> On options not relevant for doing network
     * monitoring (like
     * <code>help</code>), this method will take appropriate action (like
     * showing usage information). On occurrence of such an option, other
     * options will be ignored. On unknown options, a
     * KNXIllegalArgumentException is thrown.
     *
     * @param args array with command line options
     */
    private void parseOptions(final String[] args) {
        if (args.length == 0) {
            return;
        }

        // add defaults
        options.put("port", new Integer(KNXnetIPConnection.DEFAULT_PORT));
        options.put("medium", TPSettings.TP1);

        int i = 0;
        for (; i < args.length; i++) {
            final String arg = args[i];
            if (Utilities.isOption(arg, "-localhost", null)) {
                Utilities.parseHost(args[++i], true, options);
            } else if (Utilities.isOption(arg, "-localport", null)) {
                options.put("localport", Integer.decode(args[++i]));
            } else if (Utilities.isOption(arg, "-port", "-p")) {
                options.put("port", Integer.decode(args[++i]));
            } else if (Utilities.isOption(arg, "-nat", "-n")) {
                options.put("nat", null);
            } else if (Utilities.isOption(arg, "-serial", "-s")) {
                options.put("serial", null);
            } else if (Utilities.isOption(arg, "-medium", "-m")) {
                options.put("medium", Utilities.getMedium(args[++i]));
            } else if (options.containsKey("serial")) // add port number/identifier to serial option
            {
                options.put("serial", arg);
            } else if (!options.containsKey("host")) {
                Utilities.parseHost(arg, false, options);
            } else {
                throw new KNXIllegalArgumentException("unknown option " + arg);
            }
        }
        if (!options.containsKey("host") && !options.containsKey("serial")) {
            throw new KNXIllegalArgumentException("no host or serial port specified");
        }
    }

    final class ShutdownHandler extends Thread {

        public ShutdownHandler register() {
            Runtime.getRuntime().addShutdownHook(this);
            return this;
        }

        public void unregister() {
            Runtime.getRuntime().removeShutdownHook(this);
        }

        @Override
        public void run() {
            quit();
        }
    }
}
