package mp1.model;

import mp1.Status;

import java.sql.Timestamp;

public class Member {
    private String id;

    private String status;

    private Timestamp timestamp;

    private long incarnation;

    public Member(String id, Timestamp timestamp, long incarnation) {
        this.timestamp = timestamp;
        this.status = Status.WORKING;
        this.id = id;
        this.incarnation = incarnation;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public void updateTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return this.id;
    }

    public long getIncarnation() {
        return this.incarnation;
    }

    public void setIncarnation(long incarnation) {
        this.incarnation = incarnation;
    }
}