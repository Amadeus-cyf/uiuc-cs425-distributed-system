package mp2.message;

import mp2.ServerInfo;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

public class LsResponse extends Message{
    private Set<ServerInfo> servers;
    private String fileName;

    public LsResponse(Set<ServerInfo> servers, String fileName) {
        super(MsgType.LS_RESPONSE);
        this.servers = servers;
        this.fileName = fileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        JSONArray jsonArray = new JSONArray(servers);
        jsonObject.put(MsgKey.TARGET_SERVERS, jsonArray);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, fileName);
        return jsonObject;
    }
}
