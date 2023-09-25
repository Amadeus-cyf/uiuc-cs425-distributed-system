package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

import java.util.List;

public class GossipHeartBeat extends HeartBeat {
    private final List<Member> membershipList;
    private final String mode;
    private final String senderId;
    private final long heartbeatCounter;

    public GossipHeartBeat(
        String mode,
        String senderId,
        List<Member> membershipList,
        long heartbeatCounter
    ) {
        super(MsgType.GOSSIP_MSG);
        this.senderId = senderId;
        this.membershipList = membershipList;
        this.mode = mode;
        this.heartbeatCounter = heartbeatCounter;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            "id",
            this.senderId
        ).put(
            "msgType",
            this.msgType
        ).put(
            "membership",
            this.membershipList
        ).put(
            "mode",
            this.mode
        ).put(
            "heartbeatCounter",
            this.heartbeatCounter
        );
    }
}
