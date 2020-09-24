package mp1.model;

import mp1.Status;

import java.sql.Timestamp;

public class Member {
    private String id;

    private String status;

    private Timestamp timestamp;

    public Member(String id, Timestamp timestamp) {
        this.timestamp = timestamp;
        this.status = Status.WORKING;
        this.id = id;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatue(String status) {
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
}
