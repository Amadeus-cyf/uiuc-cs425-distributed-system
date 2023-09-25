package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleRequest extends Message {
    private final String mapleExe;
    private final int mapleNum;
    private final String intermediatePrefix;
    private final String sourceFile;

    public MapleRequest(
        String mapleExe,
        int mapleNum,
        String intermediatePrefix,
        String sourceFile
    ) {
        super(MsgType.MAPLE_REQUEST);
        this.mapleExe = mapleExe;
        this.mapleNum = mapleNum;
        this.intermediatePrefix = intermediatePrefix;
        this.sourceFile = sourceFile;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.MAPLE_EXE,
            this.mapleExe
        ).put(
            MsgKey.NUM_MAPLE,
            this.mapleNum
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            this.intermediatePrefix
        ).put(
            MsgKey.SOURCE_FILE,
            this.sourceFile
        );
    }
}
