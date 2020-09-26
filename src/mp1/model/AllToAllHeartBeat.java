package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;


public class AllToAllHeartBeat extends HeartBeat {
    private String senderId;
    private String mode;
    private long heartbeatCounter;

    public AllToAllHeartBeat(String mode, String senderId, long heartbeatCounter) {
        super(MsgType.ALL_TO_ALL_MSG);
        this.senderId = senderId;
        this.mode = mode;
        this.heartbeatCounter = heartbeatCounter;
    }

    public String getSenderId() {
        return senderId;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", senderId);
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("mode", this.mode);
        jsonObject.put("heartbeatCounter", this.heartbeatCounter);
        return jsonObject;
    }
}
