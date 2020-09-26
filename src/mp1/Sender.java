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
    private Long heartbeatCounter;
    static Logger logger = Logger.getLogger(Sender.class.getName());
    private static final int K = 3;

    public Sender(String id, String ipAddress, int port, List<Member> membershipList, String mode, UdpSocket socket, Long heartbeatCounter) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.id = id;
        this.socket = socket;
        this.mode = mode;
        this.heartbeatCounter = heartbeatCounter;
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
//                member.updateTimestamp(curTime);
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
//        logger.warning("length of the membership list:" + length);
        // empty membership list, do nothing
        if(length == 0){
            return;
        }
//        logger.warning("length not equal to zero");

        // count the number of working members

       int numAlive = 0;
       for (Member member : membershipList) {
           if (member.getStatus().equals(Status.WORKING)) {
               numAlive++;
           }
       }

        // if we have less number of servers then K working now, we send all membershiplists
        if(numAlive <= K){
//            logger.warning("length smaller than K");
            // update the timestamp for the current id
            for (Member member : membershipList) {
//                Timestamp curTime = new Timestamp(System.currentTimeMillis());
                if (member.getId().equals(this.id)) {
                    continue;
                }
                if(member.getStatus().equals(Status.FAIL)) {
                    continue;
                }
                String[] idInfo = member.getId().split("_");
//                logger.warning("the splitting result:" + idInfo.length);
                if(idInfo.length == 3){
                    sendMembership(idInfo[0], Integer.parseInt(idInfo[1]));
                }
            }
            return;
        }
//        logger.warning("start generating random numbers");
        int[] randomIndices;
        randomIndices = new int[K];
        Random random = new Random();
        for(int i = 0; i < K; i++) {
//            logger.warning("value i " + i);
            int randIdx = random.nextInt(length);
            if(membershipList.get(randIdx).getId().equals(id) || membershipList.get(randIdx).getStatus().equals(Status.FAIL)){
                i--;
                continue;
            }
            randomIndices[i] = randIdx;
            // see if the newly generated random number has already exsited in randomIndices
            for(int j = 0; j < i; j++) {
                if(randomIndices[i] == randomIndices[j]){
                    i--;
                    break;
                }
            }
        }
//        logger.warning("sendGossip: generate K random indices");

        // send MembershipList for each random indices
        for(int indx: randomIndices){
            Member curMember = membershipList.get(indx);
            // we stop sending the membershipList to the server which already fails
//            if(curMember.getStatus().equals(Status.FAIL)) {
//                continue;
//            }
            String[] idInfo = curMember.getId().split("_");
//            logger.warning("the splitting result:" + idInfo.length);
            if(idInfo.length == 3){
                sendMembership(idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    // helper function for sendGossip() of sending each membership
    private void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(this.mode, this.id, this.membershipList, this.heartbeatCounter);
        logger.warning("sendMembership: sends " + gossipHeartBeat.toJSON() + "to" + targetIpAddress + ":" + targetPort);
        this.socket.send(gossipHeartBeat.toJSON(), targetIpAddress, targetPort);
        this.updateMember();

    }

    // helper function to inc by 1 of the sender'id member
    private void updateMember() {
        this.heartbeatCounter++;
        for(int i= 0; i < this.membershipList.size(); i++) {
            if(this.membershipList.get(i).getId().equals(this.id)) {
                this.membershipList.get(i).incHeartbeatCounter();
            }
        }
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
