package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleAckRequest extends Message {
    private final String sourceFile;
    private final String intermediatePrefix;

    public MapleAckRequest(
        String sourceFile,
        String intermediatePrefix
    ) {
        super(MsgType.MAPLE_ACK_REQUEST);
        this.sourceFile = sourceFile;
        this.intermediatePrefix = intermediatePrefix;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.SOURCE_FILE,
            this.sourceFile
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            this.intermediatePrefix
        );
    }
}
