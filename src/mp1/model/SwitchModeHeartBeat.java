package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

public class SwitchModeHeartBeat extends HeartBeat {
    private final String mode;

    public SwitchModeHeartBeat(String mode) {
        super(MsgType.SWITCH_MODE);
        this.mode = mode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            "msgType",
            this.msgType
        ).put(
            "mode",
            this.mode
        );
    }
}
