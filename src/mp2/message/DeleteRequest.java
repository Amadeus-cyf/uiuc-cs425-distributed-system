package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class DeleteRequest extends Message {
    private final String fileName;
    private final String ipAddress;
    private final int port;

    public DeleteRequest(
        String fileName,
        String ipAddress,
        int port
    ) {
        super(MsgType.DEL_REQUEST);
        this.fileName = fileName;
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
            fileName
        );
    }
}
