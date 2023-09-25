package mp2.failureDetector.model;

import mp2.failureDetector.MsgType;
import org.json.JSONObject;

import java.util.ArrayList;
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
        this.membershipList = new ArrayList<>(membershipList);                  // to avoid concurrent modification exception
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
