package mp1.model;

import org.json.JSONObject;

import java.sql.Timestamp;

public class AllToAllHeartBeat extends HeartBeat {
    private String senderId;
    private Timestamp timestamp;

    public AllToAllHeartBeat(String mode, String senderId, Timestamp timestamp) {
        super(mode);
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", senderId);
        jsonObject.put("timestamp", timestamp.toString());
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }
 }
