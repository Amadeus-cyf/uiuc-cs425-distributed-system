package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

/*
 * false positive or rejoin message
 */
public class FPRejoinMessage extends Message {
    private String ipAddress;
    private int port;

    public FPRejoinMessage(String ipAddress, int port) {
        super(MsgType.FP_REJOIN_MSG);
        this.ipAddress = ipAddress;
        this.port = port - 1;               // file server port is one less the fd port number
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, ipAddress);
        jsonObject.put(MsgKey.PORT, port);
        return jsonObject;
    }
}
