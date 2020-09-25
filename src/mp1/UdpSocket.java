package mp1;

import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

public class UdpSocket {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;

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
        } catch (SocketException exception) {
            // TODO: Log the exception
        } catch (UnknownHostException exception) {
            // TODO: Log the exception
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
            Logger logger = Logger.getLogger(Sender.class.getName());
            logger.warning("SENDER: " + this.ipAddress+":"+ this.port + " sends to " +  targetIpAddress+":"+ targetPort + " message: " + msg);
        } catch (UnknownHostException exception) {
            // TODO: Log the exception
        } catch (IOException exception) {
            // TODO: Log the exception
        }
    }

    public void receive(DatagramPacket receivedPacket) {
        try {
            this.socket.receive(receivedPacket);
        } catch(IOException exception) {
            // TODO: log exception
        }
    }

    public void disconnect() {
        this.socket.close();
    }
}
