package mp1.model;

import org.json.JSONObject;

import java.util.List;

public class GossipHeartBeat extends HeartBeat {
    List<Member> membershipList;

    public GossipHeartBeat(String mode, List<Member> membershipList) {
        super(mode);
        this.membershipList = membershipList;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("membership", this.membershipList);
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }
}