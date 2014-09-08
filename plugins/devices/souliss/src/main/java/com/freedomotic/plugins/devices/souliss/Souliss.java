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
package com.freedomotic.plugins.devices.souliss;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.events.ProtocolRead;
import com.freedomotic.app.Freedomotic;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;
import java.nio.charset.Charset;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * @author Mauro Cicolella
 *
 */
public class Souliss extends Protocol {

    private static final Logger LOG = Logger.getLogger(Souliss.class.getName());
    private static ArrayList<Board> boards = null;
    private static int BOARD_NUMBER = 1;
    private static int POLLING_TIME = 1000;
    private Socket socket = null;
    private DataOutputStream outputStream = null;
    private BufferedReader inputStream = null;
    private String[] address = null;
    private int SOCKET_TIMEOUT = configuration.getIntProperty("socket-timeout", 1000);

    /**
     * Initializations
     */
    public Souliss() {
        super("Souliss", "/souliss/souliss.xml");
        setPollingWait(POLLING_TIME);
    }

    private void loadBoards() {
        if (boards == null) {
            boards = new ArrayList<Board>();
        }
        setDescription("Reading status changes from"); //empty description
        for (int i = 0; i < BOARD_NUMBER; i++) {
             // filter the tuples with "object.class" property
            String result = configuration.getTuples().getProperty(i, "object.class");
            // if the tuple hasn't an "object.class" property it's a board configuration one 
            if (result == null) {
                String ipToQuery;
                int portToQuery;
                String statusToQuery;
                ipToQuery = configuration.getTuples().getStringProperty(i, "ip-to-query", "192.168.1.201");
                portToQuery = configuration.getTuples().getIntProperty(i, "port-to-query", 80);
                statusToQuery = configuration.getTuples().getStringProperty(i, "status-to-query", "http://192.168.1.201:80/status");
                Board board = new Board(ipToQuery, portToQuery, statusToQuery);
                boards.add(board);
                setDescription(getDescription() + " " + ipToQuery + ":" + portToQuery + ";");
            }
        }
    }

    /**
     * Connection to boards
     */
    private boolean connect(String address, int port) {

        LOG.info("Trying to connect to Souliss node on address " + address + ':' + port);
        try {
            //TimedSocket is a non-blocking socket with timeout on exception
            socket = TimedSocket.getSocket(address, port, SOCKET_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT); //SOCKET_TIMEOUT ms of waiting on socket read/write
            BufferedOutputStream buffOut = new BufferedOutputStream(socket.getOutputStream());
            outputStream = new DataOutputStream(buffOut);
            return true;
        } catch (IOException e) {
            LOG.severe("Unable to connect to host " + address + " on port " + port + " Exception reported: " + e.toString());
            return false;
        }
    }

    private void disconnect() {
        // close streams and socket
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (Exception ex) {
            //do nothing. Best effort
        }
    }

    /**
     * Sensor side
     */
    @Override
    public void onStart() {
        super.onStart();
        POLLING_TIME = configuration.getIntProperty("polling-time", 1000);
        BOARD_NUMBER = configuration.getTuples().size();
        setPollingWait(POLLING_TIME);
        loadBoards();
    }

    @Override
    public void onStop() {
        super.onStop();
        //release resources
        boards.clear();
        boards = null;
        setPollingWait(-1); //disable polling
        //display the default description
        setDescription(configuration.getStringProperty("description", "Souliss"));
    }

    @Override
    protected void onRun() {
        for (Board node : boards) {
            evaluateDiffs(getJsonStatusFile(node), node); //parses the xml and crosscheck the data with the previous read
        }
        try {
            Thread.sleep(POLLING_TIME);
        } catch (InterruptedException ex) {
            LOG.severe("Thread interrupted Exception reported: " + ex.toString());
            Logger.getLogger(Souliss.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JsonNode getJsonStatusFile(Board board) {
        //get the json stream from the socket connection
        String statusFileURL = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        //statusFileURL = "http://" + board.getIpAddress() + ":"
        //      + Integer.toString(board.getPort()) + "/status";
        statusFileURL = board.getStatusToQuery();
        LOG.info("Souliss Sensor gets nodes status from file " + statusFileURL);
        try {
            // add json server http
            rootNode = mapper.readValue(readJsonFromUrl(statusFileURL), JsonNode.class);
        } catch (IOException ex) {
            LOG.severe("JSON server IOException reported: " + ex.toString());
            Logger.getLogger(Souliss.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            LOG.severe("JSONException reported: " + ex.toString());
            Logger.getLogger(Souliss.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rootNode;
    }

    public static String readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            System.out.println("Json string from server " + jsonText);
            // find the json start point 
            int startJson = jsonText.indexOf('(');
            jsonText = jsonText.substring(startJson + 1, jsonText.length());
            System.out.println("Json string filtered " + jsonText);
            JSONObject json = new JSONObject(jsonText);
            return jsonText;
        } finally {
            is.close();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private void evaluateDiffs(JsonNode rootNode, Board board) {
        int id = 0;
        int slot = 0;
        String typical = null;
        String val = null;
        //parses json
        if (rootNode != null && board != null) {
            id = 0;
            for (JsonNode node : rootNode.path("id")) {
                String hlt = node.path("hlt").getTextValue();
                System.out.println("Hlt: " + hlt + "\n");
                slot = 0;
                for (JsonNode node2 : node.path("slot")) {
                    typical = node2.path("typ").getTextValue();
                    val = node2.path("val").toString();   //.getTextValue();
                    System.out.println("id:" + id + " slot" + slot + " Typ: " + typical + " Val: " + val + "\n");
                    LOG.info("Souliss monitorize id: " + id + " slot: " + slot + " typ: " + typical + " val: " + val);
                    // call for notify event
                    sendChanges(board, id, slot, val, typical);
                    if (typical.equals("19") || typical.equals("51") || typical.equals("52") || typical.equals("53") || typical.equals("54") || typical.equals("55") || typical.equals("56") || typical.equals("57") || typical.equals("58") || typical.equals("59") ) {
			slot = slot + 2; }
                    else if (typical.equals("16")) {
			slot = slot + 4; }
                    else slot++;
                }
                id++;
            }
        }
    }

    private void sendChanges(Board board, int id, int slot, String val, String typical) { //
        //reconstruct freedomotic object address
        String address = board.getIpAddress() + ":" + board.getPort() + ":" + id + ":" + slot;
        LOG.info("Sending Souliss protocol read event for object address '" + address + "'");
        //building the event ProtocolRead
        ProtocolRead event = new ProtocolRead(this, "souliss", address);
        event.addProperty("souliss.typical", typical);
        event.addProperty("souliss.val", val);
        //event.addProperty("object.name", "Light "+id+":"+slot);
        event.addProperty("object.protocol", "souliss");
        switch (Integer.parseInt(typical)) {
            case 11:       //   ON/OFF Light-Relay
                if (val.equals("0")) {
                    event.addProperty("isOn", "false");
                } else {
                    event.addProperty("isOn", "true");
                }
                event.addProperty("object.name", "Light "+id+":"+slot);
                event.addProperty("object.class", "Light");
                break;
            case 16:       //   Dimmable Light
                if (val.equals("0")) {
                    event.addProperty("isOn", "false");
                } else {
                    event.addProperty("isOn", "true");
                }
                event.addProperty("object.name", "Light "+id+":"+slot);
                event.addProperty("object.class", "Light");
                break;
            case 19:        //  RGB Light
                if (val.equals("0")) {
                    event.addProperty("isOn", "false");
                } else {
                    event.addProperty("isOn", "true");
                }
                event.addProperty("object.name", "Light "+id+":"+slot);
                event.addProperty("object.class", "Light");
                break;
            case 51:
                //Temporally used for Lux until a Souliss Bug 77 Solved
                event.addProperty("object.name", "Analog "+id+":"+slot);
                event.addProperty("object.class", "Light Sensor");
                event.addProperty("sensor.analog", val);
                break;
            case 52:
                //event.addProperty("sensor.temperature", val);
                event.addProperty("object.name", "Temperature "+id+":"+slot);
                event.addProperty("object.class", "Thermostat");
                event.addProperty("sensor.analog", val);
                break;
            case 53:
                event.addProperty("object.name", "Humidity "+id+":"+slot);
                event.addProperty("object.class", "Hygrometer");
                event.addProperty("sensor.analog", val);
                break;                
            case 54:
                event.addProperty("object.name", "Lux "+id+":"+slot);
                event.addProperty("object.class", "Light Sensor");
                event.addProperty("sensor.analog", val);
                break;                
            case 55:
                //event.addProperty("object.name", "Voltage "+id+":"+slot);
                //event.addProperty("object.class", "Powermeter");;
                event.addProperty("sensor.analog", val);
                break;                
            case 56:
                //event.addProperty("object.name", "Current "+id+":"+slot);
                //event.addProperty("object.class", "Powermeter");;
                event.addProperty("sensor.analog", val);
                break;                
            case 57:
                event.addProperty("object.name", "Watt "+id+":"+slot);
                event.addProperty("object.class", "PowerMeter");;
                event.addProperty("sensor.analog", val);
                break;                
            case 58:
                event.addProperty("object.name", "Barometer "+id+":"+slot);
                event.addProperty("object.class", "Barometer");;
                event.addProperty("sensor.analog", val);
                break; 
            case 59:
                event.addProperty("sensor.analog", val);
                break;                
                
        }
        //publish the event on the messaging bus
        this.notifyEvent(event);
    }

    /**
     * Actuator side
     */
    @Override
    public void onCommand(Command c) throws UnableToExecuteException {
        String delimiter = configuration.getProperty("address-delimiter");

        try {
            URL url = null;
            URLConnection urlConnection;
            address = c.getProperty("address").split(delimiter);
            String ipAddress = address[0];
            String portNumber = address[1];
            String message = createCommandUrl(c);

            //Create a URL for the desired page
            url = new URL("http://" + ipAddress + ":" + portNumber + "/" + message);
            urlConnection = url.openConnection();
            LOG.info("Freedomotic sends the command " + url);
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuffer sb = new StringBuffer();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            String result = sb.toString();
        } catch (MalformedURLException e) {
            LOG.severe("Malformed URL " + e.toString());
        } catch (IOException e) {
            LOG.severe("IOexception" + e.toString());
        }



    }

    // create message to send to the board
    public String createCommandUrl(Command c) {
        String message = null;
        String id = null;
        String slot = null;
        String behavior = null;
        String url = null;
        Integer val = 0;

        id = address[2];
        slot = address[3];
        val = Integer.parseInt(c.getProperty("val"));

        //compose requested url
        url = "force?id=" + id + "&slot=" + slot + "&val=" + val;
        return (url);
    }

    @Override
    protected boolean canExecute(Command c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
