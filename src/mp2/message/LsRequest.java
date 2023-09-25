package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class LsRequest extends Message {
    private final String sdfsFileName;
    private final String ipAddress;
    private final int port;

    public LsRequest(
        String ipAddress,
        int port,
        String sdfsFileName
    ) {
        super(MsgType.LS_REQUEST);
        this.sdfsFileName = sdfsFileName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.IP_ADDRESS,
            ipAddress
        ).put(
            MsgKey.PORT,
            port
        ).put(
            MsgKey.SDFS_FILE_NAME,
            sdfsFileName
        );
    }
}
