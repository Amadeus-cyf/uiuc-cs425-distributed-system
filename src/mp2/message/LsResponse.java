package mp2.message;

import mp2.ServerInfo;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

public class LsResponse extends Message {
    private final Set<ServerInfo> servers;
    private final String fileName;

    public LsResponse(
        Set<ServerInfo> servers,
        String fileName
    ) {
        super(MsgType.LS_RESPONSE);
        this.servers = servers;
        this.fileName = fileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            msgType
        ).put(
            MsgKey.TARGET_SERVERS,
            new JSONArray(servers)
        ).put(
            MsgKey.SDFS_FILE_NAME,
            fileName
        );
    }
}
