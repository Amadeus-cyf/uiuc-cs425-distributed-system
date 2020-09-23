package mp1.model;

import mp1.Mode;

import java.sql.Timestamp;

public class AllToAllHeartBeat extends HeartBeat {
    private String senderId;
    private Timestamp timestamp;

    public AllToAllHeartBeat(Mode mode, String senderId) {
        super(mode);
        this.senderId = senderId;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public String getSenderId() {
        return senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
 }
