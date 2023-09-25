package mp2.failureDetector.model;

import mp2.failureDetector.MsgType;
import org.json.JSONObject;

public class AllToAllHeartBeat extends HeartBeat {
    private final String senderId;
    private final String mode;
    private final long heartbeatCounter;

    public AllToAllHeartBeat(
        String mode,
        String senderId,
        long heartbeatCounter
    ) {
        super(MsgType.ALL_TO_ALL_MSG);
        this.senderId = senderId;
        this.mode = mode;
        this.heartbeatCounter = heartbeatCounter;
    }

    public String getSenderId() {
        return senderId;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            "id",
            senderId
        ).put(
            "msgType",
            this.msgType
        ).put(
            "mode",
            this.mode
        ).put(
            "heartbeatCounter",
            this.heartbeatCounter
        );
    }
}
