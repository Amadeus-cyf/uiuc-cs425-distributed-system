package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class MapleCompleteMsg extends Message {
    private String ipAddress;
    private int port;
    private String sourceFile;
    private String destFile;

    public MapleCompleteMsg(String ipAddress, int port, String sourceFile, String destFile) {
        super(MsgType.MAPLE_COMPLETE_MSG);
        this.ipAddress = ipAddress;
        this.port = port;
        this.sourceFile = sourceFile;
        this.destFile = destFile;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, this.ipAddress);
        jsonObject.put(MsgKey.PORT, this.port);
        jsonObject.put(MsgKey.SOURCE_FILE, this.sourceFile);
        jsonObject.put(MsgKey.DEST_FILE, this.destFile);
        return jsonObject;
    }
}
