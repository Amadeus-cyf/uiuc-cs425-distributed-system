package mp1;

import mp1.model.*;
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
        String senderMsgType = msgJson.getString("msgType");
        switch(senderMsgType) {
            case(MsgType.ALL_TO_ALL_MSG):
                receiveAllToAll(msgJson);
                break;
            case(MsgType.JOIN_MSG):
                receiveJoinRequest(msgJson);
                break;
            case(MsgType.AGREE_JOIN):
                logger.warning("agree join!");
                receiveAndInitMembership(msgJson);
                break;
            default:
                break;
        }
    }

    /*
     * handle all to all heartbeats
     */
    private void receiveAllToAll(JSONObject msg) {
        logger.warning("receiveAllToAllt" + msg);
        String senderId = msg.getString("id");
        String senderMode = msg.getString("mode");
        // thread race condition, mode could be null if a heartbeat before a introducer response reaches
        if (this.mode == null || this.mode.equals(senderMode)) {
            updateMembershipAllToAll(senderId);
        }
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
        if (this.mode == null) {
            this.mode = request.getString("mode");
        }
        String[] senderInfo = senderId.split("_");
        if (senderInfo.length == 3) {
            logger.warning("receiveJoinRequest" + request);
            String targetIpAddress = senderInfo[0];
            int targetPort = Integer.parseInt(senderInfo[1]);
            this.membershipList.add(new Member(senderId, new Timestamp(System.currentTimeMillis())));
            HeartBeat heartBeat = new AgreeJoinHeartBeat(this.mode, this.membershipList);
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
        if (jsonArray == null) {
            return;
        }
        logger.warning("receiveAndInitMembership" + msg);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject memberJson = new JSONObject(jsonArray.get(i).toString());
            String id = memberJson.getString("id");
            if (id != null && !isMemberExists(id)) {
                synchronized (this.membershipList) {
                    this.membershipList.add(new Member(id, new Timestamp(System.currentTimeMillis())));
                }
            }
        }
    }

    /*
    * In all to all mode, update membership  ist based on the heartbeat received
     */
    private void updateMembershipAllToAll(String id) {
        boolean isInMembershipList = false;
        for (int i = 0; i < membershipList.size(); i++) {
            Member member = membershipList.get(i);
            if (member.getId().equals(id)) {
                synchronized (membershipList.get(i)) {
                    member.updateTimestamp(new Timestamp(System.currentTimeMillis()));
                }
                isInMembershipList = true;
                break;
            }
        }
        // this is a new server joining the system
        if (!isInMembershipList) {
//            synchronized (this.membershipList) {
            membershipList.add(new Member(id, new Timestamp(System.currentTimeMillis())));
//            }
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
