package mp1.model;

import mp1.Mode;

import java.sql.Timestamp;

public class AllToAllHeartBeat extends HeartBeat {
    private String message;
    private Timestamp timestamp;

    public AllToAllHeartBeat(Mode mode, String message) {
        super(mode);
        this.message = message;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
 }
