package mp2.failureDetector.model;

import mp2.failureDetector.MsgType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GossipHeartBeat extends HeartBeat {
    private List<Member> membershipList;
    private String mode;
    private String senderId;
    private long heartbeatCounter;

    public GossipHeartBeat(String mode, String senderId, List<Member> membershipList, long heartbeatCounter) {
        super(MsgType.GOSSIP_MSG);
        this.senderId = senderId;
        this.membershipList = new ArrayList<>(membershipList);                  // to avoid concurrent modification exception
        this.mode = mode;
        this.heartbeatCounter = heartbeatCounter;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", this.senderId);
        jsonObject.put("msgType", this.msgType);
        jsonObject.put("membership", this.membershipList);
        jsonObject.put("mode", this.mode);
        jsonObject.put("heartbeatCounter", this.heartbeatCounter);
        return jsonObject;
    }
}