package mp1;

import mp1.model.AllToAllHeartBeat;
import mp1.model.GossipHeartBeat;
import mp1.model.Member;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class Sender {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;
    private List<Member> membershipList;
    private String id;
    private String mode;

    public Sender(String id, String ipAddress, int port, List<Member> membershipList, String mode, DatagramSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.id = id;
        this.socket = socket;
    }
    
    public void sendAllToAll() {
        for (Member member : membershipList){
            Timestamp curTime = new Timestamp(System.currentTimeMillis());
            if(member.getId().equals(this.id) && member.getStatus().equals(Status.WORKING)) {
                member.updateTimestamp(curTime);
                continue;
            }
            if (member.getStatus().equals(Status.FAIL)) {
                continue;
            }
            AllToAllHeartBeat all2all = new AllToAllHeartBeat(Mode.ALL_TO_ALL, this.id, curTime);
            String[] idInfo = member.getId().split("_"); // ipaddr_port_timestamp
            if (idInfo.length == 3) {
                this.send(all2all.toJSON(), idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    public void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(this.mode, this.membershipList);
        this.send(gossipHeartBeat.toJSON(), targetIpAddress, targetPort);
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
            Logger logger = Logger.getLogger(Sender.class.getName());
            logger.warning("SENDER: " + ipAddress+":"+port + " sends to " +  targetIpAddress+":"+ targetPort + " message: " + msg);
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
