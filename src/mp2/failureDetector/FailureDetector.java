package mp2.failureDetector;

import mp2.DataTransfer;
import mp2.failureDetector.model.Member;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public abstract class FailureDetector {
    protected String ipAddress;
    protected int port;
    protected DataTransfer socket;
    protected List<Member> membershipList;
    protected Timestamp startingTime;
    protected String id;
    protected volatile StringBuilder modeBuilder;
    protected volatile StringBuilder statusBuilder;
    protected String status = Status.RUNNING;

    protected FailureDetector(
        String ipAddress,
        int port
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = new DataTransfer(
            this.ipAddress,
            this.port
        );
        this.membershipList = new ArrayList<>();
        this.modeBuilder = new StringBuilder();
        this.modeBuilder.append(Mode.GOSSIP);
        this.statusBuilder = new StringBuilder();
        this.statusBuilder.append(Status.RUNNING);
    }

    public List<Member> getMembershipList() {
        return this.membershipList;
    }

    protected String createId() {
        String sb = this.ipAddress +
            "_" +
            this.port +
            "_" +
            this.startingTime.toString();
        return sb;
    }

    public StringBuilder getModeBuilder() {
        return this.modeBuilder;
    }

    abstract public Sender getSender();

    public void leave() {
        this.statusBuilder.setLength(0);
        this.statusBuilder.append(Status.STOP);
        this.status = Status.STOP;
        this.membershipList.clear();
    }

    public abstract void run();
}
