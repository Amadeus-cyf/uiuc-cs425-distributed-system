package mp1.model;

import org.json.JSONObject;

public abstract class HeartBeat {
    String mode;

    protected HeartBeat(String mode) {
        this.mode = mode;
    }

    public abstract JSONObject toJSON();
}
