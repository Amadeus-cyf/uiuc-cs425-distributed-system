package mp2;

import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

public class UdpSocket {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;
    private static Logger logger = Logger.getLogger(UdpSocket.class.getName());

    public UdpSocket(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        bind();
    }

    /*
     * bind the socket to the ip address and port
     */
    public void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            this.socket = new DatagramSocket(port, address);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * send message to the target ip address and port
     */
    public void send(JSONObject msg, String targetIpAddress, int targetPort) {
        if (msg == null) {
            return;
        }
        byte[] buffer = msg.toString().getBytes();
        try {
            InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, targetPort);
            this.socket.send(packet);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void receive(DatagramPacket receivedPacket) {
        try {
            this.socket.receive(receivedPacket);
        } catch(Exception exception) {
            exception.printStackTrace();
        }
    }
}
