package mp1;

import mp1.model.AllToAllHeartBeat;
import mp1.model.GossipHeartBeat;
import mp1.model.JoinSystemHeartBeat;
import mp1.model.Member;

import java.util.ArrayList;
import java.util.Random;
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
    private Long incarnation;
    static Logger logger = Logger.getLogger(Sender.class.getName());
    private static final int K = 3;

    public Sender(String id, String ipAddress, int port, List<Member> membershipList, String mode, UdpSocket socket, Long incarnation) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.id = id;
        this.socket = socket;
        this.mode = mode;
        this.incarnation = incarnation;
    }

    public void send() {
//        logger.warning("current send mode:" + mode);
        switch(mode) {
            case(Mode.ALL_TO_ALL):
                sendAllToAll();
                break;
            case(Mode.GOSSIP):
                sendGossip();
                break;
            default:
                break;
        }
    }

    private void sendAllToAll() {
        for (Member member : membershipList){
            Timestamp curTime = new Timestamp(System.currentTimeMillis());
            if(member.getId().equals(this.id)) {
                member.updateTimestamp(curTime);
                continue;
            }
            if (member.getStatus().equals(Status.FAIL)) {
                continue;
            }
            AllToAllHeartBeat all2all = new AllToAllHeartBeat(Mode.ALL_TO_ALL, this.id);
            String[] idInfo = member.getId().split("_"); // ipaddr_port_timestamp
            if (idInfo.length == 3) {
                logger.warning("sendAlltoAll: sends" + all2all.toJSON() + "to" + idInfo[0] + ":" + idInfo[1]);
                this.socket.send(all2all.toJSON(), idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    private void sendGossip() {
        int length = membershipList.size();
        // empty membership list, do nothing
        if(length == 0){
            return;
        }

        // update the timestamp for the current id
        for (Member member : membershipList) {
            Timestamp curTime = new Timestamp(System.currentTimeMillis());
            if (member.getId().equals(this.id)) {
                member.updateTimestamp(curTime);
                break;
            }
        }

        // if we have less number of servers then K working now, we send all membershiplists
        if(length <= K){
            // update the timestamp for the current id
            for (Member member : membershipList) {
                Timestamp curTime = new Timestamp(System.currentTimeMillis());
                if (member.getId().equals(this.id)) {
                    member.updateTimestamp(curTime);
                    continue;
                }
                if(member.getStatus().equals(Status.FAIL)) {
                    continue;
                }
                String[] idInfo = member.getId().split("_");
                if(idInfo.length == 3){
                    sendMembership(idInfo[0], Integer.parseInt(idInfo[1]));
                }
            }
            return;
        }
        int[] randomIndices;
        randomIndices = new int[K];
        for(int i = 0; i < K; i++) {
            int random = new Random().nextInt(length-1);
            if(membershipList.get(random).getId().equals(id)) {
                continue;
            }
            randomIndices[i] = random;
            // see if the newly generated random number has already exsited in randomIndices
            for(int j = 0; j < i; j++) {
                if(randomIndices[i] == randomIndices[j]){
                    i--;
                    break;
                }
            }
        }
        logger.warning("sendGossip: generate K random indices of");

        // send MembershipList for each random indices
        for(int indx: randomIndices){
            Member curMember = membershipList.get(indx);
            // we stop sending the membershipList to the server which already fails
            if(curMember.getStatus().equals(Status.FAIL)) {
                continue;
            }
            String[] idInfo = curMember.getId().split("_");
            if(idInfo.length == 3){
                sendMembership(idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    // helper function for sendGossip() of sending each membership
    private void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(this.mode, this.id, this.membershipList);
        logger.warning("sendMembership: sends " + gossipHeartBeat.toJSON() + "to" + targetIpAddress + ":" + targetPort);
        this.socket.send(gossipHeartBeat.toJSON(), targetIpAddress, targetPort);
    }

    public void sendJoinRequest() {
        JoinSystemHeartBeat joinSystemHeartBeat = new JoinSystemHeartBeat(this.id);
        logger.warning("sendJoinRequest: sends " + joinSystemHeartBeat.toJSON() + "to Introducer");
        this.socket.send(joinSystemHeartBeat.toJSON(), Introducer.IP_ADDRESS, Introducer.PORT);
    }

    public void disconnect() {
        socket.disconnect();
    }
}
