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

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.Settings;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkFT12;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogWriter;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;
import tuwien.auto.calimero.process.ProcessListener;

/**
 * A tool for Calimero 2 providing basic process communication. <p> ProcComm is
 * a {@link Runnable} tool implementation allowing a user to read or write
 * datapoint values in a KNX network. It supports KNX network access using a
 * KNXnet/IP connection or an FT1.2 connection. <p> The tool implementation
 * shows the necessary interaction with the Calimero library API for this
 * particular task. The main part of this tool implementation uses the library's {@link ProcessCommunicator},
 * which offers high level access for reading and writing process values. It
 * also shows creation of a {@link KNXNetworkLink}, which is supplied to the
 * process communicator, serving as the link to the KNX network. <p> When
 * running this tool from the console to read or write one value, the
 * <code>main</code> -method of this class is invoked, otherwise use this class
 * in the context appropriate to a {@link Runnable} or use start and {@link #quit()}.
 * <br> In console mode, the values read from datapoints, as well as occurring
 * problems are written to
 * <code>System.out</code>. <p> Note that by default the communication will use
 * common settings, if not specified otherwise using command line options. Since
 * these settings might be system dependent (for example the local host) and not
 * always predictable, a user may want to specify particular settings using the
 * available options.
 * 
* @author B. Malinowsky
 */
public class ProcComm implements Runnable {

    //   private static final String tool = "ProcComm";
    //   private static final String version = "1.1";
    //   private static final String sep = System.getProperty("line.separator");
    //private static LogService out = LogManager.getManager().getLogService("tools");
    /**
     * The used process communicator.
     */
    protected ProcessCommunicator pc;
// specifies parameters to use for the network link and process communication
    private final Map options = new HashMap();
//private final LogWriter w;

    /**
     * Creates a new ProcComm instance using the supplied options. <p> Mandatory
     * arguments are an IP host or a FT1.2 port identifier, depending on the
     * type of connection to the KNX network. See {@link #main(String[])} for
     * the list of options.
     *
     * @param args list with options
     * @throws KNXIllegalArgumentException
     */
    /**
     * Creates a new ProcComm instance using the supplied options. <p> Mandatory
     * arguments are an IP host or a FT1.2 port identifier, depending on the
     * type of connection to the KNX network. See {@link #main(String[])} for
     * the list of options.
     *     
* @param args list with options
     * @param w a log writer, might be
     * <code>null</code>: this parameter is ignored for now!
     * @throws KNXIllegalArgumentException
     */
    protected ProcComm(final String[] args) throws KNXIllegalArgumentException {
//this.w = w;
        try {
            parseOptions(args);
        } catch (final KNXIllegalArgumentException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new KNXIllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Entry point for running ProcComm. <p> An IP host or port identifier has
     * to be supplied, specifying the endpoint for the KNX network access.<br>
     * To show the usage message of this tool on the console, supply the command
     * line option -help (or -h).<br> Command line options are treated case
     * sensitive. Available options for the communication connection: <ul> <li><code>-help -h</code>
     * show help message</li> <li><code>-version</code> show tool/library
     * version and exit</li> <li><code>-verbose -v</code> enable verbose status
     * output</li> <li><code>-localhost</code> <i>id</i> &nbsp;local IP/host
     * name</li> <li><code>-localport</code> <i>number</i> &nbsp;local UDP port
     * (default system assigned)</li> <li><code>-port -p</code> <i>number</i>
     * &nbsp;UDP port on host (default 3671)</li> <li><code>-nat -n</code>
     * enable Network Address Translation</li> <li><code>-routing</code> use
     * KNXnet/IP routing</li> <li><code>-serial -s</code> use FT1.2 serial
     * communication</li> <li><code>-medium -m</code> <i>id</i> &nbsp;KNX medium
     * [tp0|tp1|p110|p132|rf] (defaults to tp1)</li> </ul> Available commands
     * for process communication: <ul> <li><code>read</code> <i>DPT
     * &nbsp;KNX-address</i> &nbsp;read from group address, using DPT value
     * format</li> <li><code>write</code> <i>DPT &nbsp;value
     * &nbsp;KNX-address</i> &nbsp;write to group address, using DPT value
     * format</li> </ul> For the more common datapoint types (DPTs) the
     * following name aliases can be used instead of the general DPT number
     * string: <ul> <li><code>switch</code> for DPT 1.001</li> <li><code>bool</code>
     * for DPT 1.002</li> <li><code>string</code> for DPT 16.001</li> <li><code>float</code>
     * for DPT 9.002</li> <li><code>ucount</code> for DPT 5.010</li> <li><code>angle</code>
     * for DPT 5.003</li> </ul>
     *     
* @param args command line options for process communication
     */
    /*
     * (non-Javadoc) @see java.lang.Runnable#run()
     */
    public void run() {
        Exception thrown = null;
        boolean canceled = false;
        try {
            start(null);
            readWrite();
        } catch (final KNXException e) {
            thrown = e;
        } catch (final InterruptedException e) {
            canceled = true;
            Thread.currentThread().interrupt();
        } catch (final RuntimeException e) {
            thrown = e;
        } finally {
            quit();
            onCompletion(thrown, canceled);
        }
    }

    /**
     * Runs the process communicator. <p> This method immediately returns when
     * the process communicator is running. Call
     * {@link #quit()} to quit process communication.
     *     
* @param l a process event listener, can be
     * <code>null</code>
     * @throws KNXException on problems creating network link or communication
     * @throws InterruptedException on interrupted thread
     */
    public void start(final ProcessListener l) throws KNXException, InterruptedException {
        if (options.isEmpty()) {
        }

// create the network link to the KNX network
        final KNXNetworkLink lnk = createLink();
// ??? if this is giving useful output, re-enable lnk logging
//if (w != null)
// LogManager.getManager().addWriter(lnk.getName(), w);

// create process communicator with the established link
        pc = new ProcessCommunicatorImpl(lnk);
        if (l != null) {
            pc.addProcessListener(l);
        }
// user might specify a response timeout for KNX message
// answers from the KNX network
        if (options.containsKey("timeout")) {
            pc.setResponseTimeout(((Integer) options.get("timeout")).intValue());
        }
    }

    /**
     * Quits process communication. <p> Detaches the network link from the
     * process communicator and closes the link.
     */
    public void quit() {
        if (pc != null) {
            final KNXNetworkLink lnk = pc.detach();
            lnk.close();
        }
    }

    /**
     * Called by this tool on completion.
     *
     * @param thrown the thrown exception if operation completed due to a raised
     * exception,
     * <code>null</code> otherwise
     * @param canceled whether the operation got canceled before its planned end
     */
    protected void onCompletion(final Exception thrown, final boolean canceled) {
        if (canceled) {
            Knx4Fd.LOG.info("Process communicator was stopped");
        }
        if (thrown != null) {
            Knx4Fd.LOG.severe("Read/Write completed with error " + thrown.getMessage());
        }
    }

    /**
     * Creates the KNX network link to access the network specified in
     * <code>options</code>. <p>
     *     
* @return the KNX network link
     * @throws KNXException on problems on link creation
     * @throws InterruptedException on interrupted thread
     */
    private KNXNetworkLink createLink() throws KNXException, InterruptedException {
        final KNXMediumSettings medium = (KNXMediumSettings) options.get("medium");
        if (options.containsKey("serial")) {
// create FT1.2 network link
            final String p = (String) options.get("serial");
            try {
                return new KNXNetworkLinkFT12(Integer.parseInt(p), medium);
            } catch (final NumberFormatException e) {
                return new KNXNetworkLinkFT12(p, medium);
            }
        }
// create local and remote socket address for network link
        final InetSocketAddress local = Utilities.createLocalSocket((InetAddress) options.get("localhost"),
                (Integer) options.get("localport"));
        final InetSocketAddress host = new InetSocketAddress((InetAddress) options.get("host"),
                ((Integer) options.get("port")).intValue());
        final int mode = options.containsKey("routing") ? KNXNetworkLinkIP.ROUTING
                : KNXNetworkLinkIP.TUNNELING;
        return new KNXNetworkLinkIP(mode, local, host, options.containsKey("nat"), medium);
    }

    private void readWrite() throws KNXException, InterruptedException {
        // check if we are doing a read or write operation
        final boolean read = options.containsKey("read");
        final GroupAddress main = (GroupAddress) options.get(read ? "read" : "write");
        // encapsulate information into a datapoint
        // this is a convenient way to let the process communicator
        // handle the DPT stuff, so an already formatted string will be returned
        final Datapoint dp = new StateDP(main, "", 0, Utilities.getDPT(options));
        System.out.println("Read=" + read + " main=" + main + " dtp=" + Utilities.getDPT(options));
        final String s;
        if (read) {
            s = "Read value: " + pc.read(dp);
        } else {
// note, a write to a non existing datapoint might finish successfully,
// too.. no check for existence or read back of a written value is done
            pc.write(dp, (String) options.get("value"));
            s = "Write successful"; // FOR DEBUG
        }
        Knx4Fd.LOG.info(s);
    }

    /**
     * Reads all options in the specified array, and puts relevant options into
     * the supplied options map. <p> On options not relevant for doing process
     * communication (like
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
            if (Utilities.isOption(arg, "read", null)) {
                if (i + 2 >= args.length) {
                    break;
                }
                options.put("dpt", args[++i]);
                try {
                    options.put("read", new GroupAddress(args[++i]));
                } catch (final KNXFormatException e) {
                    throw new KNXIllegalArgumentException("read DPT: " + e.getMessage(), e);
                }
            } else if (Utilities.isOption(arg, "write", null)) {
                if (i + 3 >= args.length) {
                    break;
                }
                options.put("dpt", args[++i]);
                options.put("value", args[++i]);
                try {
                    options.put("write", new GroupAddress(args[++i]));
                } catch (final KNXFormatException e) {
                    throw new KNXIllegalArgumentException("write DPT: " + e.getMessage(), e);
                }
            } else if (Utilities.isOption(arg, "-localhost", null)) {
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
            } else if (Utilities.isOption(arg, "-timeout", "-t")) {
                options.put("timeout", Integer.decode(args[++i]));
            } else if (Utilities.isOption(arg, "-routing", null)) {
                options.put("routing", null);
            } else if (options.containsKey("serial")) // add port number/identifier to serial option
            {
                options.put("serial", arg);
            } else if (!options.containsKey("host")) {
                Utilities.parseHost(arg, false, options);
            } else {
                throw new KNXIllegalArgumentException("unknown option " + arg);
            }
        }
        if (options.containsKey("host") == options.containsKey("serial")) {
            throw new KNXIllegalArgumentException("no host or serial port specified");
        }
        if (options.containsKey("read") == options.containsKey("write")) {
            throw new KNXIllegalArgumentException("do either read or write");
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
