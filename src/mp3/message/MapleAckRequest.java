package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleAckRequest extends Message {
    private String sourceFile;

    public MapleAckRequest(String sourceFile) {
        super(MsgType.REQUEST_ACK);
        this.sourceFile = sourceFile;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SOURCE_FILE, this.sourceFile);
        return jsonObject;
    }
}
