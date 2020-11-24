package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceCompleteMsg extends Message {
    private String ipAddress;
    private int port;
    private String destFile;
    private String juiceIntermediateFile;
    private int isDelete;

    public JuiceCompleteMsg(String ipAddress, int port, String juiceIntermediateFile, String destFile, int isDelete) {
        super(MsgType.JUICE_COMPLETE_MSG);
        this.ipAddress = ipAddress;
        this.port = port;
        this.juiceIntermediateFile = juiceIntermediateFile;
        this.destFile = destFile;
        this.isDelete = isDelete;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, this.ipAddress);
        jsonObject.put(MsgKey.PORT, this.port);
        jsonObject.put(MsgKey.JUICE_INTERMEDIATE_FILE, this.juiceIntermediateFile);
        jsonObject.put(MsgKey.DEST_FILE, this.destFile);
        jsonObject.put(MsgKey.IS_DELETE, this.isDelete);
        return jsonObject;
    }
}
