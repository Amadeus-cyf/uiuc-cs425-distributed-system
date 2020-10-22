package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class StoreRequest extends Message{
    public StoreRequest() {
        super(MsgType.STORE_REQUEST);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, msgType);
        return jsonObject;
    }
}
