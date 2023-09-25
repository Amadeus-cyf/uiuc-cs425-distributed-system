package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

import java.util.List;

public class AgreeJoinHeartBeat extends HeartBeat {
    private final List<Member> membershipList;
    private final String mode;

    public AgreeJoinHeartBeat(
        String mode,
        List<Member> membershipList
    ) {
        super(MsgType.AGREE_JOIN);
        this.membershipList = membershipList;
        this.mode = mode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            "msgType",
            this.msgType
        ).put(
            "membership",
            this.membershipList
        ).put(
            "mode",
            this.mode
        );
    }
}
