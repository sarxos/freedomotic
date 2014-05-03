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
package com.freedomotic.plugins.devices.knx;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.link.medium.TPSettings;

public class Utilities {

    public static void parseHost(final String host, final boolean local, final Map options) {
        try {
            options.put(local ? "localhost" : "host", InetAddress.getByName(host));
        } catch (final UnknownHostException e) {
            throw new KNXIllegalArgumentException("failed to read host " + host, e);
        }
    }

    public static boolean isOption(final String arg, final String longOpt, final String shortOpt) {
        return arg.equals(longOpt) || shortOpt != null && arg.equals(shortOpt);
    }

    /**
     * Gets the datapoint type identifier from the
     * <code>options</code>, and maps alias names of common datapoint types to
     * its datapoint type ID. <p> The option map must contain a "dpt" key with
     * value.
     *
     * @return datapoint type identifier
     */
    public static String getDPT(Map options) {
        final String dpt = (String) options.get("dpt");
        if (dpt.equals("switch")) {
            return "1.001";
        }
        if (dpt.equals("bool")) {
            return "1.002";
        }
        if (dpt.equals("string")) {
            return "16.001";
        }
        if (dpt.equals("float")) {
            return "9.002";
        }
        if (dpt.equals("ucount")) {
            return "5.010";
        }
        if (dpt.equals("int")) {
            return "13.001";
        }
        if (dpt.equals("float32")) {
            return "14.005";
        }
        if (dpt.equals("angle")) {
            return "5.003";
        }
        return dpt;
    }
    

    public static InetSocketAddress createLocalSocket(final InetAddress host, final Integer port) {
        final int p = port != null ? port.intValue() : 0;
        try {
            return host != null ? new InetSocketAddress(host, p) : p != 0 ? new InetSocketAddress(
                    InetAddress.getLocalHost(), p) : null;
        } catch (final UnknownHostException e) {
            throw new KNXIllegalArgumentException("failed to get local host " + e.getMessage(), e);
        }
    }

    public static KNXMediumSettings getMedium(final String id) {
// for now, the local device address is always left 0 in the
// created medium setting, since there is no user cmd line option for this
// so KNXnet/IP server will supply address
        if (id.equals("tp0")) {
            return TPSettings.TP0;
        } else if (id.equals("tp1")) {
            return TPSettings.TP1;
        } else if (id.equals("p110")) {
            return new PLSettings(false);
        } else if (id.equals("p132")) {
            return new PLSettings(true);
        } else if (id.equals("rf")) {
            return new RFSettings(null);
        } else {
            throw new KNXIllegalArgumentException("unknown medium");
        }
    }

    public static String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static long getTimestamp() {
        Date date = new Date();
        return date.getTime();
    }

     /**
     * Converts a byte array to a hex string
     *
     * @param  byte array to convert
     * @return string of converted value to hex
     */
    public static String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result = result + Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static String extractMainAddress(String objectAddress) {
        String[] result = objectAddress.split("\\[");
        return result[0];
    }

    public static String extractDTP(String objectAddress) {
        String[] result = objectAddress.split("\\]");
        return result[1];
    }
}
