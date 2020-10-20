package mp2.model;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class Ack extends Message{
    private String ipAddress;
    private int port;
    private String sdfsFileName;

    public Ack(String sdfsFileName, String ipAddress, int port, String msgType) {
        super(msgType);
        this.ipAddress = ipAddress;
        this.port = port;
        this.sdfsFileName = sdfsFileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, ipAddress);
        jsonObject.put(MsgKey.PORT, port);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, sdfsFileName);
        return jsonObject;
    }
}
