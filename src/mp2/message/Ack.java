package mp2.message;

import mp2.constant.MsgKey;
import org.json.JSONObject;

public class Ack extends Message {
    private final String ipAddress;
    private final int port;
    private final String sdfsFileName;

    public Ack(
        String sdfsFileName,
        String ipAddress,
        int port,
        String msgType
    ) {
        super(msgType);
        this.ipAddress = ipAddress;
        this.port = port;
        this.sdfsFileName = sdfsFileName;
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
