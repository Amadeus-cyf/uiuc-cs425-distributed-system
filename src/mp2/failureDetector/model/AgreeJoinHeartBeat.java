package mp2.failureDetector.model;

import mp2.failureDetector.MsgType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AgreeJoinHeartBeat extends HeartBeat {
    private final List<Member> membershipList;
    private final String mode;

    public AgreeJoinHeartBeat(
        String mode,
        List<Member> membershipList
    ) {
        super(MsgType.AGREE_JOIN);
        this.membershipList = new ArrayList<>(membershipList);              // to avoid concurrent modification exception
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
