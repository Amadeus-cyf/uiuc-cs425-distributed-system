package mp1;

import mp1.model.AllToAllHeartBeat;
import mp1.model.GossipHeartBeat;
import mp1.model.JoinSystemHeartBeat;
import mp1.model.Member;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class Sender {
    private UdpSocket socket;
    private String ipAddress;
    private int port;
    private List<Member> membershipList;
    private String id;
    private String mode;
    static Logger logger = Logger.getLogger(Sender.class.getName());

    public Sender(String id, String ipAddress, int port, List<Member> membershipList, String mode, UdpSocket socket) {
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
                logger.warning("sendAlltoAll: sends" + all2all.toJSON() + "to" + idInfo[0] + ":" + idInfo[1]);
                this.socket.send(all2all.toJSON(), idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    public void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(this.mode, this.membershipList);
        logger.warning("sendMembership: sends " + gossipHeartBeat.toJSON() + "to" + targetIpAddress + ":" + targetPort);
        this.socket.send(gossipHeartBeat.toJSON(), targetIpAddress, targetPort);
    }

    public void sendJoinRequest() {
        JoinSystemHeartBeat joinSystemHeartBeat = new JoinSystemHeartBeat(this.mode, this.id);
        logger.warning("sendJoinRequest: sends " + joinSystemHeartBeat.toJSON() + "to Introducer");
        this.socket.send(joinSystemHeartBeat.toJSON(), Introducer.IP_ADDRESS, Introducer.PORT);
    }

    public void disconnect() {
        socket.disconnect();
    }
}
