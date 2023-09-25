package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceAckRequest extends Message {
    private final String destFileName;
    private final int isDelete;

    public JuiceAckRequest(
        String destFileName,
        int isDelete
    ) {
        super(MsgType.JUICE_ACK_REQUEST);
        this.destFileName = destFileName;
        this.isDelete = isDelete;
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
        );
    }
}
