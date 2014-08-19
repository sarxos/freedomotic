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
/**
 * @author Mauro Cicolella <mcicolella@libero.it>
 */
package com.freedomotic.plugins.devices.philipshue;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.app.Freedomotic;
import com.freedomotic.events.ProtocolRead;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import com.philips.lighting.hue.sdk.*;
import com.philips.lighting.model.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhilipsHue extends Protocol {

    private static final Logger LOG = Logger.getLogger(PhilipsHue.class.getName());
    private static final int MAX_HUE = 65535;
    private int SOCKET_TIMEOUT = configuration.getIntProperty("socket-timeout", 1000);
    private static int POLLING_TIME = 1000;
    private String PROTOCOL = configuration.getStringProperty("protocol.name", "philips-hue");
    private PHHueSDK phHueSDK;
    private PHBridge bridge;
    ArrayList<PHLight> allLights = new ArrayList<PHLight>();

    /**
     * Initializations
     */
    public PhilipsHue() {
        super("Philips Hue", "/philips-hue/philips-hue-manifest.xml");
        setPollingWait(POLLING_TIME);
    }

    @Override
    public void onStart() {
        super.onStart();

        phHueSDK = phHueSDK.create();
        HueProperties.loadProperties(); // Load in HueProperties, if first time use a properties file is created.
        //PHAccessPoint lastAccessPoint = new PHAccessPoint();
        if (connectToLastKnownAccessPoint()) {
            LOG.info("Philips Hue connected to " + HueProperties.getLastConnectedIP());
            setDescription("Connected to " + HueProperties.getLastConnectedIP());
        } else {
            findBridges();
        }

        //lastAccessPoint.setIpAddress(IP_ADDRESS + ":" + PORT);
        //lastAccessPoint.setUsername(USERNAME);
        //if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
        //    phHueSDK.connect(lastAccessPoint);
        //    LOG.info("Philips Hue connected to " + IP_ADDRESS + ":" + PORT);
        //    setDescription("Connected to " + IP_ADDRESS + ":" + PORT);
        //} else {  // First time use, so perform a bridge search.
        //    LOG.severe("Connection impossible! " + "Bridges found: " + phHueSDK.getAllBridges().size());
        //    onStop();
        // }
        // gets all lights 
        //PHBridge bridge = phHueSDK.getSelectedBridge();
        //allLights = (ArrayList<PHLight>) bridge.getResourceCache().getAllLights();
        findBridges();
    }

    @Override
    public void onStop() {
        super.onStop();
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {
            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }
            phHueSDK.disconnect(bridge);
            LOG.info("Philips Hue disconnected from " + HueProperties.getLastConnectedIP());
        }
        setPollingWait(-1); //disable polling
        LOG.info("Philips Hue plugin stopped");
        setDescription(configuration.getStringProperty("description", "Philips Hue"));
    }

    @Override
    protected void onRun() {
        /*
         * PHBridge bridge = phHueSDK.getSelectedBridge(); List<PHLight>
         * allLights = bridge.getResourceCache().getAllLights(); for (PHLight
         * light : allLights) { PHLightState lightState = new PHLightState();
         * lightState = light.getLastKnownLightState(); // To validate your
         * lightstate is valid (before sending to the bridge) you can use: //
         * String validState = lightState.validateState();
         * //bridge.updateLightState(light, lightState, listener);
         *
         * }
         */

        try {
            Thread.sleep(POLLING_TIME);
        } catch (InterruptedException ex) {
            LOG.severe(ex.getMessage());
        }
    }

    /**
     * Actuator side
     */
    @Override
    public void onCommand(Command c) throws UnableToExecuteException {
        String command = c.getProperty("command");
        String value = c.getProperty("value");
        // Integer address = IntegerOf(c.getProperty("address"));
        //  State newLightState = new State();

        //   if (command.)
        //  newLightState.setOn(false);


        /*
         * Note, the lights are 1-based, not 0.
         */
        // if (LIGHTS_FACTORY.setLightState(address, newLightState) == false) {
        //     logger.error("Hmm...didn't set the light");
        //   }

    }

    @Override
    protected boolean canExecute(Command c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // this method sends a freedomotic event
    private void sendEvent(String LightNumber) {
        String objectName = null;
        String objectAddress = null;
        String effect = null;
        Boolean powered = false;
        Integer brightness = 0;
        Integer hue = 0;
        Integer saturation = 0;



        ProtocolRead event = new ProtocolRead(this, PROTOCOL, objectAddress);
        event.addProperty("object.class", "HueLight");
        event.addProperty("isOn", powered.toString());
        event.addProperty("brightness", brightness.toString());
        event.addProperty("hue", hue.toString());
        event.addProperty("saturation", saturation.toString());
        event.addProperty("effect", effect);
        //publish the event on the messaging bus
        this.notifyEvent(event);
    }

// retrieve a key from value in the hashmap 
    public static Object getKeyFromValue(Map hm, Object value) {
        for (Object o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }

    public void findBridges() {
        phHueSDK = PHHueSDK.getInstance();
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        sm.search(true, true);
    }
    private PHSDKListener listener = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPointsList) {
            //  desktopView.getFindingBridgeProgressBar().setVisible(false);
            //     AccessPointList accessPointList = new AccessPointList(accessPointsList, instance);
            //     accessPointList.setVisible(true);
            //     accessPointList.setLocationRelativeTo(null); // Centre the AccessPointList Frame
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
// Start the Pushlink Authentication.
            //    desktopView.getFindingBridgeProgressBar().setVisible(false);
            phHueSDK.startPushlinkAuthentication(accessPoint);
            //     pushLinkDialog = new PushLinkFrame(instance);
            //     pushLinkDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            //     pushLinkDialog.setModal(true);
            //     pushLinkDialog.setLocationRelativeTo(null); // Center the dialog.
            //     pushLinkDialog.setVisible(true);
        }

        @Override
        public void onBridgeConnected(PHBridge bridge) {
            phHueSDK.setSelectedBridge(bridge);
            phHueSDK.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);
            String username = HueProperties.getUsername();
            String lastIpAddress = bridge.getResourceCache().getBridgeConfiguration().getIpAddress();
            LOG.info("On connected: IP " + lastIpAddress);
            HueProperties.storeUsername(username);
            HueProperties.storeLastIPAddress(lastIpAddress);
            HueProperties.saveProperties();
// Update the GUI.
            // desktopView.getLastConnectedIP().setText(lastIpAddress);
            // desktopView.getLastUserName().setText(username);
// Close the PushLink dialog (if it is showing).
            //  if (pushLinkDialog != null && pushLinkDialog.isShowing()) {
            //    pushLinkDialog.setVisible(false);
            //   }
// Enable the Buttons/Controls to change the hue bulbs.s
            //   desktopView.getRandomLightsButton().setEnabled(true);
            //   desktopView.getSetLightsButton().setEnabled(true);
        }

        @Override
        public void onCacheUpdated(int arg0, PHBridge arg1) {
        }

        @Override
        public void onConnectionLost(PHAccessPoint arg0) {
        }

        @Override
        public void onConnectionResumed(PHBridge arg0) {
        }

        @Override
        public void onError(int code, final String message) {
            //  if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
            //   desktopView.getFindingBridgeProgressBar().setVisible(false);
            //    desktopView.getFindBridgesButton().setEnabled(true);
            //    desktopView.getConnectToLastBridgeButton().setEnabled(true);
            //    desktopView.showDialog(message);
            // } else if (code == PHMessageType.PUSHLINK_BUTTON_NOT_PRESSED) {
            //   pushLinkDialog.incrementProgress();
            //  } else if (code == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {
            // if (pushLinkDialog.isShowing()) {
            //   pushLinkDialog.setVisible(false);
            //    desktopView.showDialog(message);
            //    } else {
            //   desktopView.showDialog(message);
        }
        // desktopView.getFindBridgesButton().setEnabled(true);
        //  } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
        //  desktopView.getFindingBridgeProgressBar().setVisible(false);
        //  desktopView.getFindBridgesButton().setEnabled(true);
        //  desktopView.showDialog(message);
        // }
        // }
    };

    public PHSDKListener getListener() {
        return listener;
    }

    public void setListener(PHSDKListener listener) {
        this.listener = listener;
    }

    public void randomLights() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        PHBridgeResourcesCache cache = bridge.getResourceCache();
        List<PHLight> allLights = cache.getAllLights();
        Random rand = new Random();
        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(rand.nextInt(MAX_HUE));
            bridge.updateLightState(light, lightState); // If no bridge response is required then use this simpler form.
        }
    }

    /**
     * Connect to the last known access point. This method can be used to automatically
     * connect to a bridge.
     *     
*/
    public boolean connectToLastKnownAccessPoint() {
        String username = HueProperties.getUsername();
        String lastIpAddress = HueProperties.getLastConnectedIP();
        if (username == null || lastIpAddress == null) {
            LOG.severe("Missing Last Username or Last IP. Last known connection not found.");
            return false;
        }
        PHAccessPoint accessPoint = new PHAccessPoint();
        accessPoint.setIpAddress(lastIpAddress);
        accessPoint.setUsername(username);
        phHueSDK.connect(accessPoint);
        return true;
    }
}
