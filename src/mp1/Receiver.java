package mp1;

import mp1.model.Member;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class Receiver {
    private String id;
    private String ipAddress;
    private int port;
    private DatagramSocket socket;
    private byte[] buffer = new byte[2048];
    private volatile String mode;
    private final List<Member> membershipList;
    static Logger logger = Logger.getLogger(Receiver.class.getName());

    public Receiver(String id, String ipAddress, int port, List<Member> membershipList, String mode, DatagramSocket socket) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.mode = mode;
        this.socket = socket;
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
            //logger.warning("mp1.Receiver: " + senderAddress + ":" + senderPort + " sends " + msg);
            receiveAllToAll(msg);
        }
    }

    private void receiveAllToAll(String msg) {
        JSONObject jsonObject = new JSONObject(msg);
        String senderId = jsonObject.getString("id");
        String timestampStr = jsonObject.getString("timestamp");
        Timestamp timestamp = Timestamp.valueOf(timestampStr);
        String senderMode = jsonObject.getString("mode");
        if (this.mode.equals(senderMode)) {
            updateMembershipAllToAll(senderId, timestamp);
        }
    }

    /*
     * init the membership list received from the introducer
     * used only when the server joins the system
     */
    public void receiveAndInitMembership(String msg) {
        JSONArray jsonArray = new JSONArray(msg);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
            String id = jsonObject.getString("id");
            Timestamp timestamp = Timestamp.valueOf(jsonObject.getString("timestamp"));
            synchronized (this.membershipList) {
                this.membershipList.add(new Member(id, timestamp));
            }
        }
    }

    /*
    * In all to all mode, update membership  ist based on the heartbeat received
     */
    private void updateMembershipAllToAll(String id, Timestamp timestamp) {
        boolean isInMembershipList = false;
        for (int i = 0; i < membershipList.size(); i++) {
            Member member = membershipList.get(i);
            if (member.getId().equals(id) && member.getStatus().equals(Status.WORKING) && timestamp.after(member.getTimestamp())) {
                synchronized (membershipList.get(i)) {
                    member.updateTimestamp(timestamp);
                }
                isInMembershipList = true;
                break;
            }
        }
        // this is a new server joining the system
        if (!isInMembershipList) {
            membershipList.add(new Member(id, timestamp));
        }
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

    public void disconnect() {
        socket.close();
    }
}
