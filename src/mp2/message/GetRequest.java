package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class GetRequest extends Message {
    private String sdfsFileName;
    private String localFileName;
    private String ipAddress;
    private int port;

    public GetRequest(String sdfsFileName, String localFileName, String ipAddress, int port) {
        super(MsgType.GET_REQUEST);
        this.sdfsFileName = sdfsFileName;
        this.localFileName = localFileName;
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
        jsonObject.put(MsgKey.LOCAL_FILE_NAME, localFileName);
        return jsonObject;
    }
}
