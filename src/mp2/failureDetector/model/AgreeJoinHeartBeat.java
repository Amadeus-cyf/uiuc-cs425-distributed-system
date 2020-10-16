package mp2.failureDetector.model;

import mp2.failureDetector.MsgType;
import org.json.JSONObject;

import java.util.List;

public class AgreeJoinHeartBeat extends HeartBeat {
    private List<Member> membershipList;
    private String mode;

    public AgreeJoinHeartBeat(String mode, List<Member> membershipList) {
        super(MsgType.AGREE_JOIN);
        this.membershipList = membershipList;
        this.mode = mode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("membership", this.membershipList);
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }
}
