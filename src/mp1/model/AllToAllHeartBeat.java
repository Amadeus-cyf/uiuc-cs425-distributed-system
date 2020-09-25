package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

import java.sql.Timestamp;

public class AllToAllHeartBeat extends HeartBeat {
    private String senderId;
    private Timestamp timestamp;
    private String mode;

    public AllToAllHeartBeat(String mode, String senderId, Timestamp timestamp) {
        super(MsgType.ALL_TO_ALL_MSG);
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.mode = mode;
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
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }

}
