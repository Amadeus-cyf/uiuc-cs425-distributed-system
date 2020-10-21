package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PreDelRequest extends Message {
    private String ipAddress;
    private int port;
    private String fileName;

    public PreDelRequest(String ipAddress, int port, String fileName) {
        super(MsgType.PRE_DEL_REQUEST);
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileName = fileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, ipAddress);
        jsonObject.put(MsgKey.PORT, port);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, fileName);
        return jsonObject;
    }
}
