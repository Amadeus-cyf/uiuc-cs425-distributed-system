package mp1;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class UdpSocket {
    private static final Logger logger = Logger.getLogger(UdpSocket.class.getName());
    private final String ipAddress;
    private final int port;
    private DatagramSocket socket;

    public UdpSocket(
        String ipAddress,
        int port
    ) {
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
            this.socket = new DatagramSocket(
                port,
                address
            );
        } catch (SocketException exception) {
            logger.warning(exception.toString());
        } catch (UnknownHostException exception) {
            logger.warning(exception.toString());
        }
    }

    /*
     * send message to the target ip address and port
     */
    public void send(
        JSONObject msg,
        String targetIpAddress,
        int targetPort
    ) {
        if (msg == null) {
            return;
        }
        byte[] buffer = msg.toString().getBytes();
        try {
            InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
            DatagramPacket packet = new DatagramPacket(
                buffer,
                buffer.length,
                targetAddress,
                targetPort
            );
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
        } catch (IOException exception) {
            // TODO: log exception
        }
    }

    public void disconnect() {
        this.socket.disconnect();
    }
}
