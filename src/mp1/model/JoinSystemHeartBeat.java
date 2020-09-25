package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

public class JoinSystemHeartBeat extends HeartBeat {
    private String senderId;

    public JoinSystemHeartBeat(String senderId) {
        super(MsgType.JOIN_MSG);
        this.senderId = senderId;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("id", this.senderId);
        return jsonObject;
    }
}
