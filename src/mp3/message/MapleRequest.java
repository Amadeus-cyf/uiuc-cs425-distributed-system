package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleRequest extends Message {
    private String mapleExe;
    private int mapleNum;
    private String intermediatePrefix;
    private String sourceFile;

    public MapleRequest(String mapleExe, int mapleNum, String intermediatePrefix, String sourceFile) {
        super(MsgType.MAPLE_REQUEST);
        this.mapleExe = mapleExe;
        this.mapleNum = mapleNum;
        this.intermediatePrefix = intermediatePrefix;
        this.sourceFile = sourceFile;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.MAPLE_EXE, this.mapleExe);
        jsonObject.put(MsgKey.NUM_MAPLE, this.mapleNum);
        jsonObject.put(MsgKey.INTERMEDIATE_PREFIX, this.intermediatePrefix);
        jsonObject.put(MsgKey.SOURCE_FILE, this.sourceFile);
        return jsonObject;
    }
}
