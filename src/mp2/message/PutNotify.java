package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PutNotify extends Message {
    private final String sdfsFileName;

    public PutNotify(String sdfsFileName) {
        super(MsgType.PUT_NOTIFY);
        this.sdfsFileName = sdfsFileName;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            MsgType.PUT_NOTIFY
        ).put(
            MsgKey.SDFS_FILE_NAME,
            sdfsFileName
        );
    }
}
