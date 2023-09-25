package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PreGetRequest extends Message {
    private final String ipAddress;
    private final int port;
    private final String sdfsFileName;
    private final String localFileName;

    public PreGetRequest(
        String ipAddress,
        int port,
        String sdfsFileName,
        String localFileName
    ) {
        super(MsgType.PRE_GET_REQUEST);
        this.ipAddress = ipAddress;
        this.port = port;
        this.sdfsFileName = sdfsFileName;
        this.localFileName = localFileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            msgType
        ).put(
            MsgKey.IP_ADDRESS,
            ipAddress
        ).put(
            MsgKey.PORT,
            port
        ).put(
            MsgKey.SDFS_FILE_NAME,
            sdfsFileName
        ).put(
            MsgKey.LOCAL_FILE_NAME,
            localFileName
        );
    }
}
