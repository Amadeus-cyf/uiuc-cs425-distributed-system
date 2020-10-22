package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class FailMessage extends Message {
    private String failIpAddress;
    private int failPort;

    public FailMessage(String failIpAddress, int failPort) {
        super(MsgType.SERVER_FAIL);
        this.failIpAddress = failIpAddress;
        this.failPort = failPort-1;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, failIpAddress);
        jsonObject.put(MsgKey.PORT, failPort);
        return jsonObject;
    }
}
