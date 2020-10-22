package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class LsRequest extends Message{
    private String sdfsFileName;
    private String ipAddress;
    private int port;

    public LsRequest(String ipAddress, int port, String sdfsFileName){
        super(MsgType.LS_REQUEST);
        this.sdfsFileName = sdfsFileName;
        this.ipAddress = ipAddress;
        this.port = port;
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
