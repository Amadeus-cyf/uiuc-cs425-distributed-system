package mp1;

import mp1.model.AllToAllHeartBeat;
import mp1.model.GossipHeartBeat;
import mp1.model.Member;

import java.sql.Timestamp;
import java.util.List;

public class Sender {
    private UdpSocket socket;
    private String ipAddress;
    private int port;
    private List<Member> membershipList;
    private String id;
    private String mode;

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
                this.socket.send(all2all.toJSON(), idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    public void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(this.mode, this.membershipList);
        this.socket.send(gossipHeartBeat.toJSON(), targetIpAddress, targetPort);
    }

    public void disconnect() {
        socket.disconnect();
    }
}
