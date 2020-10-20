package mp2.model;

import mp2.failureDetector.model.Member;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class JoinRequest extends Message{
    private List<ServerInfo> servers;

    public JoinRequest(List<Member> membershipList) {
        super(MsgType.JOIN_REQUEST);
        for (Member member: membershipList) {
            String[] idInfo = member.getId().split("_");
            this.servers.add(new ServerInfo(idInfo[0], Integer.parseInt(idInfo[1])));
        }

    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.MEMBERSHIP_LIST, servers);
        return jsonObject;
    }
}
