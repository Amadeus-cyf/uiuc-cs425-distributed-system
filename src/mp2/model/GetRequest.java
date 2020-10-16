package mp2.model;

import mp2.MsgKey;
import mp2.MsgType;
import org.json.JSONObject;

public class GetRequest extends Message {
    private String fileName;
    private String ipAddress;
    private int port;

    public GetRequest(String fileName, String ipAddress, int port) {
        super(MsgType.GET_REQUEST);
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
        jsonObject.put(MsgKey.FILE_NAME, fileName);
        return jsonObject;
    }
}
