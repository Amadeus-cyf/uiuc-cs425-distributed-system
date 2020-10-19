package mp2.model;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

public class PrePutResponse extends Message {
    private Set<ServerInfo> servers;

    public PrePutResponse(Set<ServerInfo> servers) {
        super(MsgType.PRE_PUT_RESPONSE);
        this.servers = servers;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        JSONArray jsonArray = new JSONArray(servers);
        jsonObject.put(MsgKey.TARGET_SERVERS, jsonArray);
        return jsonObject;
    }
}
