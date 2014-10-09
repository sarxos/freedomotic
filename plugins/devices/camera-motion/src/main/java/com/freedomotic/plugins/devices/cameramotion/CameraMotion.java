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
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import com.freedomotic.util.Info;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.ds.ipcam.*;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;

public class CameraMotion
        extends Protocol {

    private static final Logger LOG = Logger.getLogger(CameraMotion.class.getName());

    static {
        Webcam.setDriver(new IpCamDriver(new IpCamStorage(Info.PATHS.PATH_DEVICES_FOLDER + "/camera-motion/" + "cameras.xml")));
    }
    final int POLLING_WAIT;
    JFrame f = new JFrame("Camera Motion");

    public CameraMotion() {
        super("Camera Motion", "/camera-motion/camera-motion-manifest.xml");
        POLLING_WAIT = configuration.getIntProperty("time-between-reads", 2000);
        setPollingWait(-1);
    }

    @Override
    protected void onShowGui() {
        bindGuiToPlugin(f);
    }

    @Override
    protected void onHideGui() {
        //implement here what to do when the this plugin GUI is closed
        //for example you can change the plugin description
        setDescription("Camera Motion");
    }

    @Override
    protected void onRun() {
    }

    @Override
    protected void onStart() {
        try {
            loadCameras();
        } catch (MalformedURLException ex) {
            Logger.getLogger(CameraMotion.class.getName()).log(Level.SEVERE, null, ex);
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
            WebcamPanel panel = new WebcamPanel(webcam, new Dimension(256, 144), false);
            panel.setFillArea(true);
            panel.setFPSLimited(true);
            panel.setFPSLimit(0.2); // 0.1 FPS = 1 frame per 10 seconds
            panel.setBorder(BorderFactory.createEmptyBorder());
            f.add(panel);
            panels.add(panel);
        }

        f.pack();
        for (WebcamPanel panel : panels) {
            panel.start();
        }
    }
}
