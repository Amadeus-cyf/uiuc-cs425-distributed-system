package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PreDelRequest extends Message {
    private final String ipAddress;
    private final int port;
    private final String fileName;

    public PreDelRequest(
        String ipAddress,
        int port,
        String fileName
    ) {
        super(MsgType.PRE_DEL_REQUEST);
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileName = fileName;
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
            fileName
        );
    }
}
