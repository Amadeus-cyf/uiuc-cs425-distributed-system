package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleFileMsg extends Message {
    private final String sourcefile;
    private final String splitFile;
    private final String intermediatePrefix;
    private final String mapleExe;

    public MapleFileMsg(
        String sourcefile,
        String splitFile,
        String intermediatePrefix,
        String mapleExe
    ) {
        super(MsgType.MAPLE_FILE_MSG);
        this.sourcefile = sourcefile;
        this.splitFile = splitFile;
        this.intermediatePrefix = intermediatePrefix;
        this.mapleExe = mapleExe;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.SOURCE_FILE,
            this.sourcefile
        ).put(
            MsgKey.FILE_TO_MAPLE,
            this.splitFile
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            this.intermediatePrefix
        ).put(
            MsgKey.MAPLE_EXE,
            this.mapleExe
        );
    }
}
