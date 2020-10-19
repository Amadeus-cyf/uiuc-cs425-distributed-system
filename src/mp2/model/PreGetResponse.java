package mp2.model;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PreGetResponse extends Message {
    private String ipAddress;
    private int port;

    public PreGetResponse(String ipAddress, int port) {
        super(MsgType.PRE_GET_RESPONSE);
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, ipAddress);
        jsonObject.put(MsgKey.PORT, port);
        return jsonObject;
    }
}
