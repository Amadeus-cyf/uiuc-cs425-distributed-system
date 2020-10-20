package mp2.failureDetector;

import mp2.UdpSocket;
import mp2.failureDetector.model.AgreeJoinHeartBeat;
import mp2.failureDetector.model.HeartBeat;
import mp2.failureDetector.model.Member;
import mp2.model.JoinRequest;
import mp2.model.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

import static mp2.constant.MasterInfo.masterIpAddress;
import static mp2.constant.MasterInfo.masterPort;

public class Receiver {
    private String id;
    private String ipAddress;
    private int port;
    private UdpSocket socket;
    private byte[] buffer = new byte[2048];
    private volatile StringBuilder modeBuilder;
    private volatile StringBuilder statusBuilder;
    private volatile String status;
    private final List<Member> membershipList;
    private Long heartbeatCounter;
    static Logger logger = Logger.getLogger(Receiver.class.getName());

    public Receiver(String id, String ipAddress, int port, List<Member> membershipList, StringBuilder modeBuilder,
                    StringBuilder statusBuilder, UdpSocket socket, Long heartbeatCounter
    ) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.modeBuilder = modeBuilder;
        this.statusBuilder = statusBuilder;
        this.status = statusBuilder.toString();
        this.socket = socket;
        this.heartbeatCounter = heartbeatCounter;
    }

    public void start() {
        while (true) {
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.socket.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            receive(msg);
        }
    }

    /*
     * handle received message in different modes
     */
    private void receive(String msg) {
        if (!this.statusBuilder.toString().equals(Status.RUNNING)) {
            return;
        }
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
                receiveAndInitMembership(msgJson);
                break;
            case(MsgType.SWITCH_MODE):
                receiveSwitchMode(msgJson);
                break;
            default:
                break;
        }
    }

    /*
     * handle all to all heartbeats
     */
    private void receiveAllToAll(JSONObject msg) {
        //logger.warning("receiveAllToAllt" + msg);
        String senderId = msg.getString("id");
        String senderMode = msg.getString("mode");
        long heartbeatCounter = msg.getLong("heartbeatCounter");
        if (senderId == null) {
            return;
        }
        // thread race condition, mode could be null if a heartbeat before a introducer response reaches
        if (this.modeBuilder == null || this.modeBuilder.toString().equals(senderMode)) {
            boolean isInMembershipList = false;
            for (Member member : this.membershipList) {
                if ((member.getId().equals(senderId))) {
                    member.updateTimestamp(new Timestamp(System.currentTimeMillis()));
                    member.setHeartbeatCounter(heartbeatCounter);
                    isInMembershipList = true;
                    break;
                }
            }
            // this is a new working server joining the system
            if (!isInMembershipList) {
                membershipList.add(new Member(senderId, new Timestamp(System.currentTimeMillis()), heartbeatCounter));
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
        String senderMode = msg.getString("mode");
        if (this.modeBuilder != null && (!this.modeBuilder.toString().equals(senderMode))) {
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
                    if (!status.equals(Status.RUNNING)) {
                        if (member.getHeartbeatCounter() < heartbeatCounter) {
                            member.setHeartbeatCounter(heartbeatCounter);
                            member.setStatus(status);
                        }
                    } else {
                        if (member.getHeartbeatCounter() < heartbeatCounter) {
                            member.updateTimestamp(new Timestamp(System.currentTimeMillis()));
                            member.setHeartbeatCounter(heartbeatCounter);
                        }
                    }
                    // sender has fail status on the local membership list, we let it rejoin the system
                    if (member.getId().equals(senderId) && (!member.getStatus().equals(Status.RUNNING))) {
                        member.setStatus(Status.RUNNING);
                       member.setHeartbeatCounter(heartbeatCounter);
                    }
                }
            }
            // this is a new working server joining the system
            if (!isMemberExist && status.equals(Status.RUNNING)) {
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
        logger.warning("SERVER " + senderId + " requests joining the system.");
        if (senderId == null || isMemberExists(senderId)) {
            return;
        }
        if (this.modeBuilder.toString().equals("")) {
            this.modeBuilder.setLength(0);
            this.modeBuilder.append(request.getString("mode"));
        }
        String[] senderInfo = senderId.split("_");
        if (senderInfo.length == 3) {
           // logger.warning("receiveJoinRequest" + request);
            String targetIpAddress = senderInfo[0];
            int targetPort = Integer.parseInt(senderInfo[1]);
            this.membershipList.add(new Member(senderId, new Timestamp(System.currentTimeMillis()), 0));
            HeartBeat heartBeat = new AgreeJoinHeartBeat(this.modeBuilder.toString(), this.membershipList);

            //logger.warning("SendBackMembership" + heartBeat.toJSON());
            this.socket.send(heartBeat.toJSON(), targetIpAddress, targetPort);
        }
        JSONArray jsonArray = new JSONArray(membershipList);
        System.out.println(jsonArray.toString());
        Message joinRequest = new JoinRequest(membershipList);
        // send the new membership list to the master for the sdfs file system
        this.socket.send(joinRequest.toJSON(), masterIpAddress, masterPort);
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
        String senderMode = msg.getString("mode");
        if (senderMode == null) {
            return;
        }
        this.modeBuilder.setLength(0);
        this.modeBuilder.append(senderMode);
        //logger.warning("receiveAndInitMembership" + msg);
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
     * change the mode
     */
    private void receiveSwitchMode(JSONObject msg) {
        String newMode = msg.getString("mode");
        this.modeBuilder.setLength(0);
        this.modeBuilder.append(newMode);
        logger.warning("MODE CHANGED FROM " + this.modeBuilder.toString() + " to " + newMode);
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
}
