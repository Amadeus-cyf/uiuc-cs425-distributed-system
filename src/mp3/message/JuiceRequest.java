package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceRequest extends Message {
    private String juiceExe;
    private int juiceNum;
    private String intermediatePrefix;
    private String destFileName;
    private int isDelete;

    public JuiceRequest(String juiceExe, int juiceNum, String intermediatePrefix, String destFileName, int isDelete) {
        super(MsgType.JUICE_REQUEST);
        this.juiceExe = juiceExe;
        this.juiceNum = juiceNum;
        this.intermediatePrefix = intermediatePrefix;
        this.destFileName = destFileName;
        this.isDelete = isDelete;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.JUICE_EXE, this.juiceExe);
        jsonObject.put(MsgKey.NUM_JUICE, this.juiceNum);
        jsonObject.put(MsgKey.INTERMEDIATE_PREFIX, this.intermediatePrefix);
        jsonObject.put(MsgKey.DEST_FILE, this.destFileName);
        jsonObject.put(MsgKey.IS_DELETE, this.isDelete);
        return jsonObject;
    }
}
