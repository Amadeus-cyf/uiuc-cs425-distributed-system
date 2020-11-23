package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleFileMsg extends Message {
    private String sourcefile;
    private String splitFile;
    private String intermediatePrefix;
    private String mapleExe;

    public MapleFileMsg(String sourcefile, String splitFile, String intermediatePrefix, String mapleExe) {
        super(MsgType.MAPLE_FILE_MSG);
        this.sourcefile = sourcefile;
        this.splitFile = splitFile;
        this.intermediatePrefix = intermediatePrefix;
        this.mapleExe = mapleExe;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SOURCE_FILE, this.sourcefile);
        jsonObject.put(MsgKey.SPLIT_FILE, this.splitFile);
        jsonObject.put(MsgKey.INTERMEDIATE_PREFIX, this.intermediatePrefix);
        jsonObject.put(MsgKey.MAPLE_EXE, this.mapleExe);
        return jsonObject;
    }
}
