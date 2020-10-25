package mp2.message;

import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class ErrorResponse extends Message {
    private String fileName;

    public ErrorResponse(String fileName) {
        super(MsgType.ERROR_RESPONSE);
        this.fileName = fileName;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        jsonObject.put(MsgKey.ERROR, MsgContent.FILE_NOT_FOUND);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, this.fileName);
        return jsonObject;
    }
}
