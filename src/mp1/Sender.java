package mp1;

import mp1.model.Member;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

public class Sender {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;
    private String targetIpAddress;
    private int targetPort;

    public static void main(String[] args) {
        Sender sender = new Sender("localhost", 6000, "localhost", 5000);
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String line = scanner.nextLine();
            Map<String, List<Member>> map = new HashMap<>();
            List<Member> members = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                members.add(new Member("localhost", 3000 + i));
            }
            map.put("key1", members);
            sender.send(new JSONObject(map));
        }
    }

    public Sender(String ipAddress, int port, String targetIpAddress, int targetPort) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.targetIpAddress = targetIpAddress;
        this.targetPort = targetPort;
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

    /*
     * send message to the target ip address and port
     */
    public void send(JSONObject msg) {
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
