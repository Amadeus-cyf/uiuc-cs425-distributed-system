package mp1.model;

import mp1.Mode;
import org.json.JSONObject;

public class JoinSystemHeartBeat extends HeartBeat {
    private String senderId;

    public JoinSystemHeartBeat(String mode, String senderId) {
        super(mode);
        this.senderId = senderId;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mode", Mode.JOIN);
        jsonObject.put("id", this.senderId);
        return jsonObject;
    }
}
