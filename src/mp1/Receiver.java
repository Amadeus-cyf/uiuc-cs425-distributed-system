package mp1;

import mp1.model.GossipHeartBeat;
import mp1.model.HeartBeat;
import mp1.model.Member;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class Receiver {
    private String id;
    private String ipAddress;
    private int port;
    private UdpSocket socket;
    private byte[] buffer = new byte[2048];
    private volatile String mode;
    private final List<Member> membershipList;
    static Logger logger = Logger.getLogger(Receiver.class.getName());

    public Receiver(String id, String ipAddress, int port, List<Member> membershipList, String mode, UdpSocket socket) {
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
            this.socket.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            InetAddress senderAddress =  receivedPacket.getAddress();
            int senderPort = receivedPacket.getPort();
            logger.warning("mp1.Receiver: Re" + senderAddress + ":" + senderPort + " sends " + msg);
            receive(msg);
        }
    }

    /*
     * handle received message in different modes
     */
    private void receive(String msg) {
        JSONObject msgJson = new JSONObject(msg);
        String senderMode = msgJson.getString("mode");
        if (senderMode.equals(Mode.JOIN) || senderMode.equals(Mode.AGREE_JOIN) || this.mode.equals(senderMode)) {
            switch(senderMode) {
                case(Mode.ALL_TO_ALL):
                    receiveAllToAll(msgJson);
                    break;
                case(Mode.JOIN):
                    receiveJoinRequest(msgJson);
                    break;
                case(Mode.AGREE_JOIN):
                    receiveAndInitMembership(msgJson);
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * handle all to all heartbeats
     */
    private void receiveAllToAll(JSONObject msg) {
        logger.warning("receiveAllToAllt" + msg);
        String senderId = msg.getString("id");
        String timestampStr = msg.getString("timestamp");
        Timestamp timestamp = Timestamp.valueOf(timestampStr);
        updateMembershipAllToAll(senderId, timestamp);
    }

    /*
     * handle message request for joining the system
     */
    private void receiveJoinRequest(JSONObject request) {
        if (!this.ipAddress.equals(Introducer.IP_ADDRESS) || this.port != Introducer.PORT) {
            return;
        }
        String senderId = request.getString("id");
        if (senderId == null || isMemberExists(senderId)) {
            return;
        }
        String[] senderInfo = senderId.split("_");
        if (senderInfo.length == 3) {
            logger.warning("receiveJoinRequest" + request);
            String targetIpAddress = senderInfo[0];
            int targetPort = Integer.parseInt(senderInfo[1]);
            Timestamp joinTimeStamp = Timestamp.valueOf(senderInfo[2]);
            this.membershipList.add(new Member(senderId, joinTimeStamp));
            HeartBeat heartBeat = new GossipHeartBeat(Mode.AGREE_JOIN,this.membershipList);
            logger.warning("SendBackMembership" + heartBeat.toJSON());
            this.socket.send(heartBeat.toJSON(), targetIpAddress, targetPort);
        }
    }

    /*
     * init the membership list received from the introducer
     * used only when the server joins the system
     */
    private void receiveAndInitMembership(JSONObject msg) {
        if (msg.getJSONArray("membership") == null) {
            return;
        }
        JSONArray jsonArray = msg.getJSONArray("membership");
        logger.warning("receiveAndInitMembership" + msg);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject memberJson = new JSONObject(jsonArray.get(i).toString());
            String id = memberJson.getString("id");
            String timestampStr = memberJson.getString("timestamp");
            if (id != null && timestampStr != null) {
                Timestamp timestamp = Timestamp.valueOf(memberJson.getString("timestamp"));
                synchronized (this.membershipList) {
                    this.membershipList.add(new Member(id, timestamp));
                }
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
            if (member.getId().equals(id) && timestamp.after(member.getTimestamp())) {
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

    private boolean isMemberExists(String senderId) {
        boolean isMemberExists = false;
        for (Member member : this.membershipList) {
            if (member.getId().equals(senderId)) {
                isMemberExists = true;
                break;
            }
        }
        return isMemberExists;
    }

    public void disconnect() {
        this.socket.disconnect();
    }
}
