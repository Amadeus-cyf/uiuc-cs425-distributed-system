package mp2.message;

import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class ErrorResponse extends Message {
    public ErrorResponse() {
        super(MsgType.ERROR_RESPONSE);
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        jsonObject.put(MsgKey.ERROR, MsgContent.FILE_NOT_FOUND);
        return jsonObject;
    }
}
