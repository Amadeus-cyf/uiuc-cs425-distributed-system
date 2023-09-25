package mp2.failureDetector.model;

import mp2.failureDetector.MsgType;
import org.json.JSONObject;

public class JoinSystemHeartBeat extends HeartBeat {
    private final String senderId;

    public JoinSystemHeartBeat(String senderId) {
        super(MsgType.JOIN_MSG);
        this.senderId = senderId;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            "msgType",
            this.msgType
        ).put(
            "id",
            this.senderId
        );
    }
}
