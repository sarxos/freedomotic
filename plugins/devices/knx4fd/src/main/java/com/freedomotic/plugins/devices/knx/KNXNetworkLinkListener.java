package com.freedomotic.plugins.devices.knx;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.link.LinkListener;
import tuwien.auto.calimero.link.NetworkLinkListener;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
/**
 *
 * @author mauro
 */
public class KNXNetworkLinkListener implements NetworkLinkListener {

    /**
     * Reference to the KNXServer class
     */
    KNXServer serverRef;

    public KNXNetworkLinkListener(KNXServer ref) {
        serverRef = ref;
    }

    @Override
    public void indication(FrameEvent e) {
        System.out.println("srcadress " + e.getSource());
        System.out.println(e.getSource().getClass());
        System.out.println("targetadress " + ((tuwien.auto.calimero.cemi.CEMILData) e.getFrame()).getDestination());
    }

    @Override
    public void linkClosed(CloseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void confirmation(FrameEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
