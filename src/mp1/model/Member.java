package mp1.model;

import mp1.Status;

import java.sql.Timestamp;

public class Member {
    private String id;

    private String ipAddress;

    private int port;

    private String status;

    private Timestamp timestamp;

    public Member(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.status = Status.WORKING;
        this.id = createId();
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    public String getStatus() {
        return this.status;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public void updateTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    private String createId() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.ipAddress);
        sb.append("_");
        sb.append(port);
        sb.append("_");
        sb.append(this.timestamp.toString());
        return sb.toString();
    }
}
