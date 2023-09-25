package mp2.message;

import mp2.ServerInfo;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

public class PrePutResponse extends Message {
    private final Set<ServerInfo> servers;
    private final String sdfsFileName;
    private final String localFileName;

    public PrePutResponse(
        String sdfsFileName,
        String localFileName,
        Set<ServerInfo> servers
    ) {
        super(MsgType.PRE_PUT_RESPONSE);
        this.servers = servers;
        this.sdfsFileName = sdfsFileName;
        this.localFileName = localFileName;
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
            sdfsFileName
        ).put(
            MsgKey.LOCAL_FILE_NAME,
            localFileName
        );
    }
}
