package mp2.message;

import mp2.ServerInfo;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

import java.util.Set;

public class ReplicateRequest extends Message {
    private final String fileName;
    private final Set<ServerInfo> newReplicaServers;
    private final String ipAddress;
    private final int port;

    public ReplicateRequest(
        String fileName,
        Set<ServerInfo> newReplicaServers,
        String ipAddress,
        int port
    ) {
        super(MsgType.REPLICATE_REQUEST);
        this.fileName = fileName;
        this.newReplicaServers = newReplicaServers;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.SDFS_FILE_NAME,
            fileName
        ).put(
            MsgKey.TARGET_SERVERS,
            newReplicaServers
        ).put(
            MsgKey.IP_ADDRESS,
            ipAddress
        ).put(
            MsgKey.PORT,
            port
        );
    }
}
