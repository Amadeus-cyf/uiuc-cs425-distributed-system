package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

public class JuiceAckRequest extends Message {
    private String destFileName;
    private int isDelete;

    public JuiceAckRequest(String destFileName, int isDelete) {
        super(MsgType.JUICE_ACK_REQUEST);
        this.destFileName = destFileName;
        this.isDelete = isDelete;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.DEST_FILE, this.destFileName);
        jsonObject.put(MsgKey.IS_DELETE, this.isDelete);
        return jsonObject;
    }
}
