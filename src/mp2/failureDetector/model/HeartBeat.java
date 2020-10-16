package mp2.failureDetector.model;

import org.json.JSONObject;

public abstract class HeartBeat {
    protected String msgType;

    protected HeartBeat(String msgType) {
        this.msgType = msgType;
    }

    public abstract JSONObject toJSON();
}
