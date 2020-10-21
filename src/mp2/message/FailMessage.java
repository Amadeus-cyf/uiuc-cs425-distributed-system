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
        this.failPort = failPort;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.IP_ADDRESS, failIpAddress);
        jsonObject.put(MsgKey.PORT, failPort);
        return jsonObject;
    }
}
