package mp2.message;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

public class ReplicateRequest extends Message {
    private String fileName;
    private String targetIpAddress;
    private int targetPort;

    public ReplicateRequest(String fileName, String targetIpAddress, int targetPort) {
        super(MsgType.REPLICATE_REQUEST);
        this.fileName = fileName;
        this.targetIpAddress = targetIpAddress;
        this.targetPort = targetPort;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, fileName);
        jsonObject.put(MsgKey.IP_ADDRESS, targetIpAddress);
        jsonObject.put(MsgKey.PORT, targetPort);
        return jsonObject;
    }
}
