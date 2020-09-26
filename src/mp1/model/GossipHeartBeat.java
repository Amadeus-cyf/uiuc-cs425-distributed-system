package mp1.model;

import mp1.MsgType;
import org.json.JSONObject;

import java.util.List;

public class GossipHeartBeat extends HeartBeat {
    private List<Member> membershipList;
    private String mode;
    private String senderId;

    public GossipHeartBeat(String mode, String senderId, List<Member> membershipList) {
        super(MsgType.GOSSIP_MSG);
        this.senderId = senderId;
        this.membershipList = membershipList;
        this.mode = mode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", this.senderId);
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("membership", this.membershipList);
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }
}