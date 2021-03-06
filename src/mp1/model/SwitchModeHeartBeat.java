package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

public class SwitchModeHeartBeat extends HeartBeat {
    private String mode;

    public SwitchModeHeartBeat(String mode) {
        super(MsgType.SWITCH_MODE);
        this.mode = mode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }
}
