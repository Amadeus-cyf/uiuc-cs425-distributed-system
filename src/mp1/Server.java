package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Server {
    String id;
    List<Member> membershipList;
    private boolean isIntroducer;
    private String ipAddress;
    private int port;
    private Timestamp startingTime;
    private volatile String mode;
    private String status;

    private Sender sender;
    private Receiver receiver;

    public Server(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = new ArrayList<>();
        this.isIntroducer = false;
    }

    private void join() {
        if (this.isIntroducer || (this.status != null && this.status.equals(Status.WORKING))) {
            return;
        }
        this.id = createId();
        this.startingTime = new Timestamp(System.currentTimeMillis());
        Member member = new Member(this.id, this.startingTime);
        // create sender and receiver
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList);

        // sender send a message to the ip address and port of the introducer
        // sender.send();
        // receiver.receiveAndInitMembership();
    }

    private String createId() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.ipAddress);
        sb.append("_");
        sb.append(port);
        sb.append("_");
        sb.append(this.startingTime.toString());
        return sb.toString();
    }
}
