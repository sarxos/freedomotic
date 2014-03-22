package com.freedomotic.knx;

import com.freedomotic.knx.KNXServer;
import java.util.EventListener;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.process.ProcessEvent;
import tuwien.auto.calimero.process.ProcessListener;

/**
 * Class used to capture KNX events from the KNX server
 *
 */
class KNXListener implements ProcessListener {

    /**
     * Reference to the KNXServer class
     */
    KNXServer serverRef;

    public KNXListener(KNXServer ref) {
        serverRef = ref;
    }

    @Override
    /**
     * Callback method whenever something is written in the KNX network
     *
     * @param Contains the information about the event occured
     */
    public void groupWrite(ProcessEvent event) {

        String destAddr = event.getDestination().toString();

       //Message value (state)
        byte[] asdu = event.getASDU();
        //Check if state was correct
        int state = ((asdu != null) && (asdu.length > 0)) ? asdu[0] : -1;

       System.out.println("Group address is " + destAddr + "and state is " + state);

    }

   // @Override
    public void detached(DetachEvent arg0) {

        Knx.LOG.info("The KNXNetworkLink has been disconnected from the Process Monitor");
    }

    /**
     * Method used to send a command to a KNX device
     *
     * @param deviceAddress	The group address of the device
     * @param value	The value to send to the device TRUE or FALSE
     * @return	TRUE if everything went fine
     */
    public boolean sendDataToDevice(String deviceAddress, String value) throws KNXException {
        //try to send data to a device
        try {
            Knx.LOG.info("Trying to send message to device with address " + deviceAddress + "...");
            GroupAddress gp = new GroupAddress(deviceAddress);
            serverRef.getPc().write(gp, value);
            Knx.LOG.info("Sent message to device with address " + deviceAddress + " successfully");
            return true;
        } catch (KNXFormatException e) {
            Knx.LOG.severe("Could not obtain GroupAddress from string provided. Aborting send");
            return false;
        } catch (KNXTimeoutException e) {
            Knx.LOG.severe("Timeout occurred while trying to send a write to the KNX network. Aborting send.");
            return false;
        } catch (KNXLinkClosedException e) {
            Knx.LOG.severe("The link was closed while trying to send a command. Aborting send");
            return false;
        }

    }
}
