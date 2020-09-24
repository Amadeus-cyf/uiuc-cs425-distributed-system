package mp1;

import mp1.model.AllToAllHeartBeat;
import mp1.model.GossipHeartBeat;
import mp1.model.Member;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Sender {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;
    private List<Member> membershipList;
    private String id;
    private String mode;


    public Sender(String ipAddress, int port, List<Member> membershipList, String id, String mode) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = membershipList;
        this.id = id;
        this.mode = mode;
        bind();
    }

    public static void main(String[] args) {
        String sender_ipAddress = "localhost";
        Integer sender_port = 6000;
        String receiver_ipAddress = "localhost";
        Integer receiver_port = 5000;
        Member member = new Member("senderID", new Timestamp(System.currentTimeMillis()));
        List<Member> mem_lst = new ArrayList<>();
        mem_lst.add(member);
        mem_lst.add(member);
        mem_lst.add(member);
        mem_lst.add(member);
//        System.out.println("Test sendAlltoAll");
        Sender sender = new Sender(sender_ipAddress, sender_port, mem_lst, "senderID", Mode.ALL_TO_ALL);
//        sender.sendAllToAll(Mode.ALL_TO_ALL);
        System.out.println("Test sendGossip");
        sender.sendMembership(receiver_ipAddress, receiver_port);
    }

    /*
     * bind the socket to the ip address and port
     */
    private void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            socket = new DatagramSocket(port, address);
        } catch (SocketException exception) {
            // TODO: Log the exception


        } catch (UnknownHostException exception) {
            // TODO: Log the exception

        }
    }
    
    public void sendAllToAll() {
        for(int i=0; i < membershipList.size(); i++){
            Member member = membershipList.get(i);
            if(member.getId() == this.id){
                continue;
            }
            Timestamp cur_time = new Timestamp(System.currentTimeMillis()); 
            AllToAllHeartBeat all2all = new AllToAllHeartBeat(Mode.ALL_TO_ALL, this.id, cur_time);
            String[] id_info = member.getId().split("_"); // ipaddr_port_timestamp
            this.send(all2all.toJSON(), id_info[0], Integer.parseInt(id_info[1]));
        }
    }

    public void sendMembership(String targetIpAddress, int targetPort) {
        GossipHeartBeat gossipHeartBeat = new GossipHeartBeat(mode, membershipList);
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
            socket.send(packet);
            Logger logger = Logger.getLogger(Sender.class.getName());
            logger.warning("SENDER: " + ipAddress+":"+port + " sends to " +  targetIpAddress+":"+port + " message: " + msg);
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


