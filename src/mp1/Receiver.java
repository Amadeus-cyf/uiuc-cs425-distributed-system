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
    private Long heartbeatCounter;
    static Logger logger = Logger.getLogger(Receiver.class.getName());

    public Receiver(String id, String ipAddress, int port, List<Member> membershipList, String mode, UdpSocket socket, Long heartbeatCounter
    ) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.mode = mode;
        this.socket = socket;
        this.heartbeatCounter = heartbeatCounter;
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
            case(MsgType.GOSSIP_MSG):
                receiveGossip(msgJson);
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
        long heartbeatCounter = msg.getLong("heartbeatCounter");
        // thread race condition, mode could be null if a heartbeat before a introducer response reaches
        if (this.mode == null || this.mode.equals(senderMode)) {
            boolean isInMembershipList = false;
            for (Member member : this.membershipList) {
                if ((member.getId().equals(senderId))) {
                    member.updateTimestamp(new Timestamp(System.currentTimeMillis()));
                    member.setHeartbeatCounter(heartbeatCounter);
                    isInMembershipList = true;
                    break;
                }
            }
            // this is a new server joining the system
            if (!isInMembershipList) {
                synchronized (this.membershipList) {
                    membershipList.add(new Member(senderId, new Timestamp(System.currentTimeMillis()), heartbeatCounter));
                }
            }
        }
    }

    /*
     * handle gossip heartbeats
     */
    private void receiveGossip(JSONObject msg) {
        String senderId = msg.getString("id");
        JSONArray senderMembershipList = msg.getJSONArray("membership");
        if (senderMembershipList == null || senderId == null) {
            return;
        }
        for (int i = 0; i < senderMembershipList.length(); i++) {
            JSONObject memberJson = new JSONObject(senderMembershipList.get(i).toString());
            long heartbeatCounter = memberJson.getLong("heartbeatCounter");
            String status = memberJson.getString("status");
            String id = memberJson.getString("id");
            boolean isMemberExist = false;
            for (Member member : this.membershipList) {
                if (member.getId().equals(id)) {
                    isMemberExist = true;
                    if (status.equals(Status.FAIL)) {
                        if (member.getHeartbeatCounter() < heartbeatCounter) {
                            member.setHeartbeatCounter(heartbeatCounter);
                            member.setStatus(Status.FAIL);
                        }
                    } else {
                        if (member.getHeartbeatCounter() < heartbeatCounter) {
                            member.updateTimestamp(new Timestamp(System.currentTimeMillis()));
                            member.setHeartbeatCounter(heartbeatCounter);
                        }
                    }
                    // sender has fail status on the local membership list, we let it rejoin the system
                    if (member.getId().equals(senderId) && member.getStatus().equals(Status.FAIL)) {
                        member.setStatus(Status.WORKING);
                        member.setHeartbeatCounter(heartbeatCounter);
                    }
                }
            }
            if (!isMemberExist) {
                this.membershipList.add(new Member(id, new Timestamp(System.currentTimeMillis()), heartbeatCounter));
            }
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
            this.membershipList.add(new Member(senderId, new Timestamp(System.currentTimeMillis()), 0));
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
            long  heartbeatCounter = memberJson.getLong("heartbeatCounter");
            if (id != null && !isMemberExists(id)) {
                synchronized (this.membershipList) {
                    this.membershipList.add(new Member(id, new Timestamp(System.currentTimeMillis()), heartbeatCounter));
                }
            }
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
