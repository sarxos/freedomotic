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
package com.freedomotic.plugins.devices.cameramotion;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.app.Freedomotic;
import com.freedomotic.exceptions.PluginStartupException;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import com.freedomotic.util.Info;
import com.github.sarxos.webcam.*;
import com.github.sarxos.webcam.ds.ipcam.*;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.border.Border;

public class CameraMotion
        extends Protocol {

    private static final Logger LOG = Logger.getLogger(CameraMotion.class.getName());

    static {
        Webcam.setDriver(new IpCamDriver(new IpCamStorage(Info.PATHS.PATH_DEVICES_FOLDER + "/camera-motion/" + "cameras.xml")));
    }
    JFrame f = new JFrame("Camera Motion");

    public CameraMotion() {
        super("Camera Motion", "/camera-motion/camera-motion-manifest.xml");
        setPollingWait(-1);
    }

    @Override
    protected void onShowGui() {
        bindGuiToPlugin(f);
    }

    @Override
    protected void onHideGui() {
        setDescription("Camera Motion");
    }

    @Override
    protected void onRun() {
        new DetectMotion();
    }

    @Override
    protected void onStart() throws PluginStartupException {
        try {
            loadCameras();
        } catch (MalformedURLException ex) {
            // TODO if there are no cameras available stop plugin
            throw new PluginStartupException("Error loading cameras " + ex.getMessage(), ex);
        }
        LOG.info("Camera Motion plugin started");

    }

    @Override
    protected void onStop() {
        LOG.info("Camera Motion plugin stopped");
    }

    @Override
    protected void onCommand(Command c)
            throws IOException, UnableToExecuteException {
        LOG.info("Camera Motion plugin receives a command called " + c.getName() + " with parameters "
                + c.getProperties().toString());
    }

    @Override
    protected boolean canExecute(Command c) {
        //don't mind this method for now
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        //don't mind this method for now
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void loadCameras() throws MalformedURLException {

        f.setLayout(new GridLayout(0, 3, 1, 1));
        List<WebcamPanel> panels = new ArrayList<WebcamPanel>();
        for (Webcam webcam : Webcam.getWebcams()) {
            // TODO set panel dimensions as config parameters
            WebcamPanel panel = new WebcamPanel(webcam, new Dimension(256, 144), false);
            panel.setFillArea(true);
            panel.setFPSLimited(true);
            panel.setFPSLimit(0.2); // 0.1 FPS = 1 frame per 10 seconds
            //panel.setBorder(BorderFactory.createEmptyBorder());
            Border title = BorderFactory.createTitledBorder(webcam.getName());
            panel.setBorder(title);
            f.add(panel);
            panels.add(panel);
        }

        f.pack();
        for (WebcamPanel panel : panels) {
            panel.start();
        }
    }

    public class DetectMotion implements WebcamMotionListener {

        List<WebcamMotionDetector> detectors = new ArrayList<WebcamMotionDetector>();

        public DetectMotion() {
            for (Webcam webcam : Webcam.getWebcams()) {
                WebcamMotionDetector detector = new WebcamMotionDetector(webcam);
                detector.setInterval(100); // one check per 100 ms (10 FPS)
                detector.addMotionListener(this);
                detectors.add(detector);
            }
            for (WebcamMotionDetector detector : detectors) {
                detector.start();
            }
        }

        @Override
        public void motionDetected(WebcamMotionEvent wme) {
            Webcam webcam = ((WebcamMotionDetector) wme.getSource()).getWebcam();
            IpCamDevice device = (IpCamDevice) webcam.getDevice(); // in case whe IP camera driver is used
            System.out.println("Camera " + webcam.getName() + " detected motion!");
            // capture an image
            captureImage(webcam.getName());
            //System.out.println("Webcam name: " + webcam.getName());
            //System.out.println("Webcam URL: " + device.getURL());
        }
    }

    public void captureImage(String cameraName) {
        Webcam webcam = getCameraByName(cameraName);
        if (webcam != null) {
            webcam.open();
            BufferedImage image = webcam.getImage();
            Date now = new Date();
            SimpleDateFormat dateformat =
                    new SimpleDateFormat("dd MMMM yyyy - HH:mm.ss");
            try {
                ImageIO.write(image, "PNG", new File(Info.PATHS.PATH_DEVICES_FOLDER + "/camera-motion/data/" + webcam.getName() + "_" + now + ".png"));
            } catch (IOException ex) {
                LOG.severe(ex.getMessage());
            }
        }
    }

    public Webcam getCameraByName(String name) {
        for (Webcam webcam : Webcam.getWebcams()) {
            if (webcam.getName().equalsIgnoreCase(name)) {
                return webcam;
            }
        }
        return null;
    }
}
