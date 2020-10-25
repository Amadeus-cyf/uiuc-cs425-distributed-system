package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class ReplicateNotify extends Message {
    String sdfsFileName;

    public ReplicateNotify(String sdfsFileName) {
        super(MsgType.REPLICATE_NOTIFY);
        this.sdfsFileName = sdfsFileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, this.sdfsFileName);
        return jsonObject;
    }
}
