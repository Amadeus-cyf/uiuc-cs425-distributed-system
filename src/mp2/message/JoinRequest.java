package mp2.message;

import mp2.ServerInfo;
import mp2.failureDetector.model.Member;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JoinRequest extends Message{
    private List<ServerInfo> servers;

    public JoinRequest(List<Member> membershipList) {
        super(MsgType.JOIN_REQUEST);
        this.servers = new ArrayList<>();
        for (Member member: membershipList) {
            String[] idInfo = member.getId().split("_");
            // we need to substract 1 for port number sending to the master
            this.servers.add(new ServerInfo(idInfo[0], Integer.parseInt(idInfo[1])-1));
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
