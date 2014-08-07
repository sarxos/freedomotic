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

/**
 * @autor Mauro Cicolella <mcicolella@libero.it>
*/

package com.freedomotic.plugins.devices.philipshue;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
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
    public String IP_ADDRESS = configuration.getStringProperty("ip-address", "127.0.0.1");
    private int PORT = configuration.getIntProperty("port", 80);
    private String PROTOCOL = configuration.getStringProperty("protocol.name", "philips-hue");
    private PHHueSDK phHueSDK;
    private PHBridge bridge;

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

        phHueSDK = PHHueSDK.create();
        PHAccessPoint lastAccessPoint = new PHAccessPoint();
        lastAccessPoint.setIpAddress(IP_ADDRESS);
        lastAccessPoint.setUsername("newdeveloper");
        if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
            phHueSDK.connect(lastAccessPoint);
            System.out.println("Bridges found: " + phHueSDK.getAllBridges().size());
        } else {  // First time use, so perform a bridge search.
            System.out.println("Impossible to connect!");
        }

        //HueProperties.loadProperties();  // Load in HueProperties, if first time use a properties file is created.
        //bridge = phHueSDK.getSelectedBridge();

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
        }
        setPollingWait(-1); //disable polling
        //display the default description
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
            Logger.getLogger(PhilipsHue.class.getName()).log(Level.SEVERE, null, ex);
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
    // Create a Listener to receive bridge notifications.
    public PHSDKListener listener = new PHSDKListener() {

        @Override
        public void onCacheUpdated(int flags, PHBridge bridge) {
        }

        @Override
        public void onBridgeConnected(PHBridge b) {
            phHueSDK.setSelectedBridge(b);
            phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
            phHueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
            //prefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
            //prefs.setUsername(prefs.getUsername());
            //PHWizardAlertDialog.getInstance().closeProgressDialog();
            //startMainActivity();
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            //Log.w(TAG, "Authentication Required.");

            phHueSDK.startPushlinkAuthentication(accessPoint);
            //startActivity(new Intent(PHHomeActivity.this, PHPushlinkActivity.class));

        }

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> list) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void onError(int code, final String message) {
            //  Log.e(TAG, "on Error Called : " + code + ":" + message);

            if (code == PHHueError.NO_CONNECTION) {
//            Log.w(TAG, "On No Connection");
            } else if (code == PHHueError.AUTHENTICATION_FAILED || code == 1158) {
                //          PHWizardAlertDialog.getInstance().closeProgressDialog();
            } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
                //        Log.w(TAG, "Bridge Not Responding . . . ");
                //      PHWizardAlertDialog.getInstance().closeProgressDialog();
                //      PHHomeActivity.this.runOnUiThread(new Runnable() {
                //        @Override
                //      public void run() {
                //          PHWizardAlertDialog.showErrorDialog(PHHomeActivity.this, message, R.string.btn_ok);
                //      }
                //   }); 
            } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
                // PHWizardAlertDialog.getInstance().closeProgressDialog();
                //  PHHomeActivity.this.runOnUiThread(new Runnable() {
                //    @Override
                //    public void run() {
                //      PHWizardAlertDialog.showErrorDialog(PHHomeActivity.this, message, R.string.btn_ok);
                // }
                // });                
            }
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoints) {
        }
    };
}
