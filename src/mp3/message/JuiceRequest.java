package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceRequest extends Message {
    private final String juiceExe;
    private final int juiceNum;
    private final String intermediatePrefix;
    private final String destFileName;
    private final int isDelete;

    public JuiceRequest(
        String juiceExe,
        int juiceNum,
        String intermediatePrefix,
        String destFileName,
        int isDelete
    ) {
        super(MsgType.JUICE_REQUEST);
        this.juiceExe = juiceExe;
        this.juiceNum = juiceNum;
        this.intermediatePrefix = intermediatePrefix;
        this.destFileName = destFileName;
        this.isDelete = isDelete > 0 ? 1 : 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.JUICE_EXE,
            this.juiceExe
        ).put(
            MsgKey.NUM_JUICE,
            this.juiceNum
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            this.intermediatePrefix
        ).put(
            MsgKey.DEST_FILE,
            this.destFileName
        ).put(
            MsgKey.IS_DELETE,
            this.isDelete
        );
    }
}
