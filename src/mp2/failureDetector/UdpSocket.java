package mp2.failureDetector;

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
        } catch (SocketException exception) {
            logger.warning(exception.toString());
        } catch (UnknownHostException exception) {
            logger.warning(exception.toString());
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
        } catch (UnknownHostException exception) {
            logger.warning(exception.toString());
        } catch (IOException exception) {
            logger.warning(exception.toString());
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
        this.socket.disconnect();
    }
}
