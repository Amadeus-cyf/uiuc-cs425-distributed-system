package mp1;

import mp1.model.Member;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class Receiver {
    private String ipAddress;
    private int port;
    private DatagramSocket socket;
    private byte[] buffer = new byte[2048];
    private Mode mode;
    private List<Member> membershipList;
    static Logger logger = Logger.getLogger(Receiver.class.getName());

    public static void main(String[] args) {
        Receiver receiver = new Receiver("localhost", 5000);
        receiver.start();
    }

    public Receiver(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        bind();
    }

    /*
     * Bind to the socket to the ip address and the port
     */
    private void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            socket = new DatagramSocket(port, address);
        } catch (SocketException exception) {
            // TODO: log exception
            System.out.println(exception);
        } catch (UnknownHostException exception) {
            // TODO: log exception
            System.out.println(exception);
        }
    }

    public void start() {
        while (true) {
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivedPacket);
            } catch(IOException exception) {
                // TODO: log exception
            }
            InetAddress senderAddress = receivedPacket.getAddress();
            int senderPort = receivedPacket.getPort();
            String msg = readBytes(buffer, receivedPacket.getLength());
            logger.warning("mp1.Receiver: " + senderAddress + ":" + senderPort + " sends " + msg);
            JSONObject obj = new JSONObject(msg);
//            JSONArray list = obj.getJSONArray("key1");
        }
    }

    public void disconnect() {
        socket.close();
    }

    /*
     * turn bytes into string
     */
    private String readBytes(byte[] packet, int length) {
        if (packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char)(packet[i]));
        }
        return sb.toString();
    }

    /*
    * update membership  list based on the heartbeat received
     */
    private void updateMembershipAllToAll(String ipAddress, int port, Timestamp timestamp) {
        boolean isInMembershipList = false;
        for (int i = 0; i < membershipList.size(); i++) {
            Member member = membershipList.get(i);
            String[] mem_info = member.getId().split("_");
            if (Integer.parseInt(mem_info[1]) == port && mem_info[0].equals(ipAddress)) {
                if (member.getStatus().equals(Status.WORKING)) {
                    if (timestamp.after(member.getTimestamp())) {
                        synchronized (membershipList.get(i)) {
                            member.updateTimestamp(timestamp);
                        }
                    }
                    isInMembershipList = true;
                    break;
                }
            }
        }
        // this is a new server joining the system
        if (!isInMembershipList) {
            Timestamp d = new Timestamp(System.currentTimeMillis());
            membershipList.add(new Member(ipAddress, d));
        }
    }
}
