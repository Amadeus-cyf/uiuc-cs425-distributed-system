package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceCompleteMsg extends Message {
    private final String ipAddress;
    private final int port;
    private final String destFile;
    private final String juiceIntermediateFile;
    private final int isDelete;

    public JuiceCompleteMsg(
        String ipAddress,
        int port,
        String juiceIntermediateFile,
        String destFile,
        int isDelete
    ) {
        super(MsgType.JUICE_COMPLETE_MSG);
        this.ipAddress = ipAddress;
        this.port = port;
        this.juiceIntermediateFile = juiceIntermediateFile;
        this.destFile = destFile;
        this.isDelete = isDelete;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.IP_ADDRESS,
            this.ipAddress
        ).put(
            MsgKey.PORT,
            this.port
        ).put(
            MsgKey.JUICE_INTERMEDIATE_FILE,
            this.juiceIntermediateFile
        ).put(
            MsgKey.DEST_FILE,
            this.destFile
        ).put(
            MsgKey.IS_DELETE,
            this.isDelete
        );
    }
}
