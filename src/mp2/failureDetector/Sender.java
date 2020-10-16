package mp2.failureDetector;

import mp2.failureDetector.model.*;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class Sender {
    private UdpSocket socket;
    private String ipAddress;
    private int port;
    private List<Member> membershipList;
    private String id;
    private volatile StringBuilder modeBuilder;
    private volatile StringBuilder statusBuilder;
    private Long heartbeatCounter;
    static Logger logger = Logger.getLogger(Sender.class.getName());
    private static final int K = 3;

    public Sender(String id, String ipAddress, int port, List<Member> membershipList, StringBuilder modeBuilder, StringBuilder statusBuilder,
                  UdpSocket socket, Long heartbeatCounter) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.id = id;
        this.socket = socket;
        this.modeBuilder = modeBuilder;
        this.statusBuilder = statusBuilder;
        this.heartbeatCounter = heartbeatCounter;
    }

    public void send() {
        if (!this.statusBuilder.toString().equals(Status.RUNNING)) {
            return;
        }
        switch(this.modeBuilder.toString()) {
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
        //logger.warning("SEND ALL TO ALL");
        for (Member member : membershipList){
            if(member.getId().equals(this.id) || (!member.getStatus().equals(Status.RUNNING))) {
                continue;
            }
            AllToAllHeartBeat all2all = new AllToAllHeartBeat(Mode.ALL_TO_ALL, this.id, this.heartbeatCounter);
            String[] idInfo = member.getId().split("_"); // ipaddr_port_timestamp
            if (idInfo.length == 3) {
                //logger.warning("sendAlltoAll: sends" + all2all.toJSON() + "to" + idInfo[0] + ":" + idInfo[1]);
                this.socket.send(all2all.toJSON(), idInfo[0], Integer.parseInt(idInfo[1]));
                updateMember();
            }
        }
    }

    private void sendGossip() {
        //logger.warning("SEND GOSSIP");
        int length = membershipList.size();
        // empty membership list, do nothing
        if(length == 0){
            return;
        }
        // count the number of working members
       int numAlive = 0;
       for (Member member : membershipList) {
           if (member.getStatus().equals(Status.RUNNING)) {
               numAlive++;
           }
       }
        // if we have less number of servers then K working now, we send all membershiplists
        if(numAlive <= K){
            // update the timestamp for the current id
            for (Member member : membershipList) {
                if (member.getId().equals(this.id)) {
                    continue;
                }
                if(!member.getStatus().equals(Status.RUNNING)) {
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
        Random random = new Random();
        for(int i = 0; i < K; i++) {
            int randIdx = random.nextInt(length);
            if(membershipList.get(randIdx).getId().equals(id) || (!membershipList.get(randIdx).getStatus().equals(Status.RUNNING))){
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
        // send MembershipList for each random indices
        for(int indx: randomIndices){
            Member curMember = membershipList.get(indx);
            // we stop sending the membershipList to the server which already fails
            String[] idInfo = curMember.getId().split("_");
            if(idInfo.length == 3){
                sendMembership(idInfo[0], Integer.parseInt(idInfo[1]));
            }
        }
    }

    /*
     * helper function for sendGossip() of sending each membership
     */
    private void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(this.modeBuilder.toString(), this.id, this.membershipList, this.heartbeatCounter);
        //logger.warning("sendMembership: sends " + gossipHeartBeat.toJSON() + "to" + targetIpAddress + ":" + targetPort);
        this.socket.send(gossipHeartBeat.toJSON(), targetIpAddress, targetPort);
        this.updateMember();

    }

    /*
     * helper function to inc by 1 of the sender'id member
     */
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
        //logger.warning("sendJoinRequest: sends " + joinSystemHeartBeat.toJSON() + "to Introducer");
        this.socket.send(joinSystemHeartBeat.toJSON(), Introducer.IP_ADDRESS, Introducer.PORT);
    }

    public void switchMode(String mode) {
        if (this.modeBuilder.toString().equals(mode)) {
            return;
        }
        this.modeBuilder.setLength(0);
        this.modeBuilder.append(mode);
        SwitchModeHeartBeat switchModeHeartBeat = new SwitchModeHeartBeat(mode);
        for (Member member : membershipList) {
            String[] idInfo = member.getId().split("_");
            if (idInfo.length == 3) {
                this.socket.send(switchModeHeartBeat.toJSON(),idInfo[0], Integer.parseInt(idInfo[1]));
                logger.warning("Mode changed sends to " + idInfo[0] + ":" + idInfo[1] + switchModeHeartBeat.toJSON().toString());
            }
        }
    }
}
