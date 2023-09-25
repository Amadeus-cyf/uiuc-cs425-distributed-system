package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleAck extends Message {
    private final String sourceFile;
    private final String intermediatePrefix;
    private final String ipAddress;
    private final int port;

    public MapleAck(
        String sourceFile,
        String intermediatePrefix,
        String ipAddress,
        int port
    ) {
        super(MsgType.MAPLE_ACK);
        this.sourceFile = sourceFile;
        this.intermediatePrefix = intermediatePrefix;
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
            MsgKey.SOURCE_FILE,
            sourceFile
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            intermediatePrefix
        ).put(
            MsgKey.IP_ADDRESS,
            this.ipAddress
        ).put(
            MsgKey.PORT,
            this.port
        );
    }
}
