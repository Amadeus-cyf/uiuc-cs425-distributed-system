package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleAckRequest extends Message {
    private String sourceFile;
    private String intermediatePrefix;

    public MapleAckRequest(String sourceFile, String intermediatePrefix) {
        super(MsgType.MAPLE_ACK_REQUEST);
        this.sourceFile = sourceFile;
        this.intermediatePrefix = intermediatePrefix;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SOURCE_FILE, this.sourceFile);
        jsonObject.put(MsgKey.INTERMEDIATE_PREFIX, this.intermediatePrefix);
        return jsonObject;
    }
}
