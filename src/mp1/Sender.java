package mp1;

import mp1.model.Member;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;

public class Sender {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;
    private final List<Member> membershipList;

    public Sender(String ipAddress, int port, List<Member> membershipList) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        bind();
    }

    /*
     * bind the socket to the ip address and port
     */
    private void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            socket = new DatagramSocket(port, address);
        } catch (SocketException exception) {
            // TODO: Log the exception
        } catch (UnknownHostException exception) {
            // TODO: Log the exception
        }
    }

    public void sendAllToAll() {

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
            socket.send(packet);
            Logger logger = Logger.getLogger(Sender.class.getName());
            logger.warning("SENDER: " + ipAddress+":"+port + " sends to " +  targetIpAddress+":"+port + " message: " + msg);
        } catch (UnknownHostException exception) {
            // TODO: Log the exception
        } catch (IOException exception) {
            // TODO: Log the exception
        }
    }

    public void disconnect() {
        socket.close();
    }
}
