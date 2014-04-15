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
import java.util.logging.Level;
import java.util.logging.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.Settings;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.link.*;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.link.medium.RawFrame;
import tuwien.auto.calimero.link.medium.RawFrameBase;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogStreamWriter;
import tuwien.auto.calimero.log.LogWriter;

/**
 * A tool for Calimero allowing monitoring of KNX network messages. <p>
 * NetworkMonitor is a {@link Runnable} tool implementation allowing a user to
 * track KNX network messages in a KNX network. It provides monitoring access
 * using a KNXnet/IP connection or FT1.2 connection. NetworkMonitor shows the
 * necessary interaction with the core library API for this particular task.<br>
 * Note that by default the network monitor will run with default settings, if
 * not specified otherwise using command line options. Since these settings
 * might be system dependent (for example the local host) and not always
 * predictable, a user may want to specify particular settings using the
 * available tool options. <p> The main part of this tool implementation
 * interacts with the type {@link KNXNetworkMonitor}, which offers monitoring
 * access to a KNX network. All monitoring output, as well as occurring problems
 * are written to either
 * <code>System.out
 * </code> (console mode), or the log writer supplied by the user. <p> To use
 * the network monitor, invoke {@link NetworkMonitor#main(String[])}, or create
 * a new instance with {@link NetworkMonitor#NetworkMonitor(String[], LogWriter)},
 * and invoke
 * {@link NetworkMonitor#start()} or {@link NetworkMonitor#run()} on that
 * instance. When running this tool from the console, the
 * <code>main</code> method of this class is executed, otherwise use it in the
 * context appropriate to a {@link Runnable}. <p> To quit a monitor running on a
 * console, use a user interrupt for termination (
 * <code>^C</code> for example).
 * 
* @author B. Malinowsky
 */
public class NetworkMonitor implements Runnable {

    private final Map options = new HashMap();
    private KNXNetworkMonitor m;
    private final LinkListener l = new LinkListener() {

        public void indication(final FrameEvent e) {
            NetworkMonitor.this.onIndication(e);
        }

        public void linkClosed(final CloseEvent e) {
            Knx4Fd.LOG.info("Network monitor closed (" + e.getReason() + ")");
            synchronized (NetworkMonitor.this) {
                NetworkMonitor.this.notify();
            }
        }
    };

    /**
     * Creates a new NetworkMonitor instance using the supplied options. <p> See {@link #main(String[])}
     * for a list of options.
     *
     * @param args list with options
     * @throws KNXIllegalArgumentException on unknown/invalid options
     */
    public NetworkMonitor(final String[] args) {
        try {
// read the command line options
            parseOptions(args);
        } catch (final KNXIllegalArgumentException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new KNXIllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Entry point for running the NetworkMonitor. <p> An IP host or port
     * identifier has to be supplied, specifying the endpoint for the KNX
     * network access.<br> To show the usage message of this tool on the
     * console, supply the command line option -help (or -h).<br> Command line
     * options are treated case sensitive. Available options for network
     * monitoring: <ul> <li><code>-help -h</code> show help message</li> <li><code>-version</code>
     * show tool/library version and exit</li> <li><code>-verbose -v</code>
     * enable verbose status output</li> <li><code>-localhost</code> <i>id</i>
     * &nbsp;local IP/host name</li> <li><code>-localport</code> <i>number</i>
     * &nbsp;local UDP port (default system assigned)</li> <li><code>-port -p</code>
     * <i>number</i> &nbsp;UDP port on host (default 3671)</li> <li><code>-nat -n</code>
     * enable Network Address Translation</li> <li><code>-serial -s</code> use
     * FT1.2 serial communication</li> <li><code>-medium -m</code> <i>id</i>
     * &nbsp;KNX medium [tp0|tp1|p110|p132|rf] (defaults to tp1)</li> </ul>
     *
     * @param args command line options for network monitoring
     */
    /*
     * (non-Javadoc) @see java.lang.Runnable#run()
     */
    public void run() {
        Exception thrown = null;
        boolean canceled = false;
        try {
            start();

// If we have a user supplied link listener, we would not wake up on
// closed link events caused by the underlying network monitor instance or
// the server side endpoint. Therefore, we could use our own listener to
// receive that notification, or simply wake up from time to time in the
// wait loop and check monitor status. We do the latter.

// just wait for the network monitor to quit
            synchronized (this) {
                while (m != null && m.isOpen()) {
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
        if (options.isEmpty()) {
        }
        try {
            m = createMonitor();
        } catch (UnknownHostException ex) {
            Logger.getLogger(NetworkMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
        // on console we want to have all possible information, so enable
        // decoding of a received raw frame by the monitor link
        m.setDecodeRawFrames(true);
        // listen to monitor link events
        m.addMonitorListener(l);
    }

    /**
     * Quits a running network monitor, otherwise returns immediately. <p>
     */
    public void quit() {
        if (m != null && m.isOpen()) {
            m.close();
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
        final StringBuffer sb = new StringBuffer();
        sb.append(e.getFrame().toString());
// since we specified decoding of raw frames during monitor initialization,
// we can get the decoded raw frame here
// but note, that on decoding error null is returned
        final RawFrame raw = ((MonitorFrameEvent) e).getRawFrame();
        if (raw != null) {
            sb.append(": ").append(raw.toString());
            if (raw instanceof RawFrameBase) {
                final RawFrameBase f = (RawFrameBase) raw;
                sb.append(": ").append(DataUnitBuilder.decode(f.getTPDU(), f.getDestination()));
            }
        }
        Knx4Fd.LOG.info("Read frame: " + sb.toString());
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
            Knx4Fd.LOG.info("Stopped");
        }
        if (thrown != null) {
            Knx4Fd.LOG.severe("onCompletion " + thrown.getMessage() != null ? thrown.getMessage() : thrown.getClass().getName());
        }
    }

    /**
     * Creates the KNX network monitor link to access the network specified in
     * <code>options</code>. <p>
     *
     * @return the KNX network monitor link
     * @throws KNXException on problems on link creation
     * @throws InterruptedException on interrupted thread
     */
    private KNXNetworkMonitor createMonitor() throws KNXException, InterruptedException, UnknownHostException {
        final KNXMediumSettings medium = (KNXMediumSettings) options.get("medium");
        if (options.containsKey("serial")) {
            // create FT1.2 monitor link
            final String p = (String) options.get("serial");
            try {
                return new KNXNetworkMonitorFT12(Integer.parseInt(p), medium);
            } catch (final NumberFormatException e) {
                return new KNXNetworkMonitorFT12(p, medium);
            }
        }
        // create local and remote socket address for monitor link
        final InetSocketAddress local = Utilities.createLocalSocket((InetAddress) options.get("localhost"),
               (Integer) options.get("localport"));
          final InetSocketAddress host = new InetSocketAddress((InetAddress) options.get("host"),
                ((Integer) options.get("port")).intValue());
       // InetSocketAddress local = new InetSocketAddress(InetAddress.getLocalHost(), 0);
       // InetSocketAddress host = new InetSocketAddress(Knx.EIBD_SERVER_ADDRESS, Integer.valueOf(Knx.EIBD_SERVER_PORT));
        // create the monitor link, based on the KNXnet/IP protocol
        // specify whether network address translation shall be used,
        // and tell the physical medium of the KNX network
        //return new KNXNetworkMonitorIP(local, host, options.containsKey("nat"), medium);
        return new KNXNetworkMonitorIP(local, host, false, TPSettings.TP1);
        
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

    public final class ShutdownHandler extends Thread {

        ShutdownHandler register() {
            Runtime.getRuntime().addShutdownHook(this);
            return this;
        }

        void unregister() {
            Runtime.getRuntime().removeShutdownHook(this);
        }

        public void run() {
            quit();
        }
    }
}
