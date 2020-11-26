package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleAck extends Message {
    private String sourceFile;
    private String intermediatePrefix;
    private String ipAddress;
    private int port;

    public MapleAck(String sourceFile, String intermediatePrefix, String ipAddress, int port) {
        super(MsgType.MAPLE_ACK);
        this.sourceFile = sourceFile;
        this.intermediatePrefix = intermediatePrefix;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SOURCE_FILE, sourceFile);
        jsonObject.put(MsgKey.INTERMEDIATE_PREFIX, intermediatePrefix);
        jsonObject.put(MsgKey.IP_ADDRESS, this.ipAddress);
        jsonObject.put(MsgKey.PORT, this.port);
        return jsonObject;
    }
}
