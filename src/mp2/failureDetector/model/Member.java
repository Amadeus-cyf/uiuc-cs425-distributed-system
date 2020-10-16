package mp2.failureDetector.model;

import mp2.failureDetector.Status;

import java.sql.Timestamp;

public class Member {
    private String id;

    private String status;

    private Timestamp timestamp;

    private long heartbeatCounter;

    public Member(String id, Timestamp timestamp, long heartbeatCounter) {
        this.timestamp = timestamp;
        this.status = Status.RUNNING;
        this.id = id;
        this.heartbeatCounter = heartbeatCounter;
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

    public long getHeartbeatCounter() {
        return this.heartbeatCounter;
    }

    public void incHeartbeatCounter() {
        this.heartbeatCounter++;
    }

    public void setHeartbeatCounter(long heartbeatCounter) {
        this.heartbeatCounter = heartbeatCounter;
    }
}