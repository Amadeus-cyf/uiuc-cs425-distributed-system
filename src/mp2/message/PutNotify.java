package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class PutNotify extends Message {
    private String sdfsFileName;

    public PutNotify(String sdfsFileName) {
        super(MsgType.PUT_NOTIFY);
        this.sdfsFileName = sdfsFileName;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, MsgType.PUT_NOTIFY);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, sdfsFileName);
        return jsonObject;
    }
}
