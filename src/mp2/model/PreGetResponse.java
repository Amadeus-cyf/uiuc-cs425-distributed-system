package mp2.model;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PreGetResponse extends Message {
    private String ipAddress;
    private int port;
    private String sdfsFileName;
    private String localFileName;

    public PreGetResponse(String sdfsFileName, String localFileName, String ipAddress, int port) {
        super(MsgType.PRE_GET_RESPONSE);
        this.sdfsFileName = sdfsFileName;
        this.localFileName = localFileName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        jsonObject.put(MsgKey.IP_ADDRESS, ipAddress);
        jsonObject.put(MsgKey.PORT, port);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, sdfsFileName);
        jsonObject.put(MsgKey.LOCAL_FILE_NAME, localFileName);

        return jsonObject;
    }
}
