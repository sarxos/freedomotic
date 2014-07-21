package com.freedomotic.plugins.devices.enocean;

import org.opencean.core.ESP3Host;
import org.opencean.core.EnoceanReceiver;
import org.opencean.core.EnoceanSerialConnector;
import org.opencean.core.common.ParameterAddress;
import org.opencean.core.common.ParameterValueChangeListener;
import org.opencean.core.common.ProtocolConnector;
import org.opencean.core.common.values.Value;
import org.opencean.core.packets.BasicPacket;
import org.opencean.core.packets.Header;
import org.opencean.core.packets.QueryIdCommand;

/**
 *
 * @author mauro
 */
public class EnoceanClient implements EnoceanReceiver, ParameterValueChangeListener {

    private static final byte[] testPayload = new byte[] { (byte) 0xD2, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD,
            (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, 0x00, (byte) 0x80, 0x35, (byte) 0xC4, 0x00, (byte) 0x03, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x4D, (byte) 0x00, (byte) 0x36 };
    
    public void connect(String connectorPort) {
        //ProtocolConnector connector = new EnoceanSerialConnector();
        //connector.connect(connectorPort);
        // simulates a fake connection for testing
        EnoceanBufferedDummieConnector connector = new EnoceanBufferedDummieConnector(2048);
        Header testHeader = new Header((byte) 1, (short) 15, (byte) 7);
        connector.write(testPayload);
        ESP3Host esp3Host = new ESP3Host(connector);
        esp3Host.addParameterChangeListener(this);
        esp3Host.addListener(this);
        //BasicPacket packet = new QueryIdCommand();
        //esp3Host.sendRadio(packet);
        esp3Host.start();
        for (int i=1; i<=10; i++)
            
        connector.write(testPayload);
    }

    @Override
    public void receivePacket(BasicPacket packet) {
        System.out.println("Packet received");
    }

    @Override
    public void valueChanged(ParameterAddress parameterId, Value value) {
        System.out.println("Value received");
    }
}
