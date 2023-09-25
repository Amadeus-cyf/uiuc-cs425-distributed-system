package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleCompleteMsg extends Message {
    private final String ipAddress;
    private final int port;
    private final String sourceFile;
    private final String mapleIntermediateFile;
    private final String intermediatePrefix;

    public MapleCompleteMsg(
        String ipAddress,
        int port,
        String sourceFile,
        String mapleIntermediateFile,
        String intermediatePrefix
    ) {
        super(MsgType.MAPLE_COMPLETE_MSG);
        this.ipAddress = ipAddress;
        this.port = port;
        this.sourceFile = sourceFile;
        this.mapleIntermediateFile = mapleIntermediateFile;
        this.intermediatePrefix = intermediatePrefix;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.IP_ADDRESS,
            this.ipAddress
        ).put(
            MsgKey.PORT,
            this.port
        ).put(
            MsgKey.SOURCE_FILE,
            this.sourceFile
        ).put(
            MsgKey.MAPLE_INTERMEDIATE_FILE,
            this.mapleIntermediateFile
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            this.intermediatePrefix
        );
    }
}
