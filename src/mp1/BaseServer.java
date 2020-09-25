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

    protected BaseServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = new UdpSocket(ipAddress, port);
        this.membershipList = new ArrayList<>();
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
}
