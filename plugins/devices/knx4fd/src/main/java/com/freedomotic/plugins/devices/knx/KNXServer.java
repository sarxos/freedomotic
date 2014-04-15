package com.freedomotic.plugins.devices.knx;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;

import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.event.NetworkLinkListener;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;

public class KNXServer {

    Knx4Fd knx;
    /**
     * The object used to interact with the KNX network
     */
    private KNXNetworkLink knxLink = null;
    /**
     * The object used to read and write from the KNX network
     */
    private ProcessCommunicator pc = null;
    /**
     * A listener class used to capture KNX events
     */
    private KNXListener listener = null;
    private KNXNetworkLinkListener networkLinkListener = null;

    public KNXServer(Knx4Fd knx) {
        this.knx = knx;
    }

    /**
     * Connects to the KNX network through the EIBD server, thus the EIBD server
     * must be up&running first
     *
     * @param serverIP The IP where the EIBD server should be listening
     * @param serverPort The Port where the EIBD server is listening
     * @return TRUE if connection successful
     */
    public boolean connectToEIBD(String serverIP, int serverPort) throws InterruptedException {

        try {
            listener = new KNXListener(this);
            networkLinkListener = new KNXNetworkLinkListener(this);
            InetSocketAddress local = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            InetSocketAddress remote = new InetSocketAddress(serverIP, serverPort);
            knxLink = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, local, remote, false, TPSettings.TP1);


            setPc(new ProcessCommunicatorImpl(knxLink));
            getPc().addProcessListener(listener);
            knxLink.addLinkListener(networkLinkListener);
            return true;



        } catch (KNXException e) {
            knx.LOG.severe("There was a problem trying to create a KNXNetworkLink object or creating a ProcessCommunicator object. More info " + e.getMessage());
            return false;
        } catch (UnknownHostException e) {
            knx.LOG.severe("There was a problem trying to obtain the local host address, aborting.");
            return false;
        }

    }

    public ProcessCommunicator getPc() {
        return pc;
    }

    public void setPc(ProcessCommunicator pc) {
        this.pc = pc;
    }

    public KNXListener getListener() {
        return listener;
    }
}
