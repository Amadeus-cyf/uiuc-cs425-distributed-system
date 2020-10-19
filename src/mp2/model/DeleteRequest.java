package mp2.model;

import mp2.constant.MsgType;
import mp2.constant.MsgKey;
import org.json.JSONObject;

public class DeleteRequest extends Message{
    private String fileName;
    private String ipAddress;
    private int port;

    public DeleteRequest(String fileName, String ipAddress, int port) {
        super(MsgType.DEL_REQUEST);
        this.fileName = fileName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, ipAddress);
        jsonObject.put(MsgKey.PORT, port);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, fileName);
        return jsonObject;
    }
}
