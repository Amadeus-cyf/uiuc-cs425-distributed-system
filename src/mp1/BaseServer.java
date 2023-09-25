package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseServer {
    protected String ipAddress;
    protected int port;
    protected UdpSocket socket;
    protected List<Member> membershipList;
    protected Timestamp startingTime;
    protected String id;
    protected volatile StringBuilder modeBuilder;
    protected volatile StringBuilder statusBuilder;
    protected String status = Status.RUNNING;

    protected BaseServer(
        String ipAddress,
        int port
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = new UdpSocket(
            ipAddress,
            port
        );
        this.membershipList = new ArrayList<>();
        this.modeBuilder = new StringBuilder();
        this.modeBuilder.append(Mode.GOSSIP);
        this.statusBuilder = new StringBuilder();
        this.statusBuilder.append(Status.RUNNING);
    }

    protected String createId() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.ipAddress);
        sb.append("_");
        sb.append(this.port);
        sb.append("_");
        sb.append(this.startingTime.toString());
        return sb.toString();
    }

    protected StringBuilder getModeBuilder() {
        return this.modeBuilder;
    }

    abstract public Sender getSender();

    public void leave() {
        this.statusBuilder.setLength(0);
        this.statusBuilder.append(Status.STOP);
        this.status = Status.STOP;
        this.membershipList.clear();
    }
}
