package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PreGetResponse extends Message {
    private final String ipAddress;
    private final int port;
    private final String sdfsFileName;
    private final String localFileName;

    public PreGetResponse(
        String sdfsFileName,
        String localFileName,
        String ipAddress,
        int port
    ) {
        super(MsgType.PRE_GET_RESPONSE);
        this.sdfsFileName = sdfsFileName;
        this.localFileName = localFileName;
        this.ipAddress = ipAddress;
        this.port = port;
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
