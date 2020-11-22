package mp3.message;

import org.json.JSONObject;

public abstract class Message {
    protected String msgType;

    protected Message(String msgType) {
        this.msgType = msgType;
    }

    public abstract JSONObject toJSON();
}
