package mp2.message;

import mp2.ServerInfo;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

import java.util.Set;

public class ReplicateRequest extends Message {
    private String fileName;
    private Set<ServerInfo> newReplicaServers;

    public ReplicateRequest(String fileName, Set<ServerInfo> newReplicaServers) {
        super(MsgType.REPLICATE_REQUEST);
        this.fileName = fileName;
        this.newReplicaServers = newReplicaServers;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, fileName);
        jsonObject.put(MsgKey.TARGET_SERVERS, newReplicaServers);
        return jsonObject;
    }
}
