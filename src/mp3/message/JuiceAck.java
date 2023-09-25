package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceAck extends Message {
    private final String destFileName;
    private final int isDelete;
    private final String ipAddress;
    private final int port;

    public JuiceAck(
        String destFileName,
        int isDelete,
        String ipAddress,
        int port
    ) {
        super(MsgType.JUICE_ACK);
        this.destFileName = destFileName;
        this.isDelete = isDelete;
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
            MsgKey.DEST_FILE,
            this.destFileName
        ).put(
            MsgKey.IS_DELETE,
            this.isDelete
        ).put(
            MsgKey.IP_ADDRESS,
            this.ipAddress
        ).put(
            MsgKey.PORT,
            this.port
        );
    }
}
