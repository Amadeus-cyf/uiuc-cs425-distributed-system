package mp2.message;

import mp2.ServerInfo;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.failureDetector.model.Member;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JoinRequest extends Message {
    private final List<ServerInfo> servers;

    public JoinRequest(List<Member> membershipList) {
        super(MsgType.JOIN_REQUEST);
        this.servers = new ArrayList<>();
        for (Member member : membershipList) {
            String[] idInfo = member.getId().split("_");
            // we need to subtract 1 for port number sending to the master
            this.servers.add(new ServerInfo(
                idInfo[0],
                Integer.parseInt(idInfo[1]) - 1
            ));
        }

    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.MEMBERSHIP_LIST,
            servers
        );
    }
}
