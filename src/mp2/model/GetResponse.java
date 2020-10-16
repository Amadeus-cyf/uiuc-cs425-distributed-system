package mp2.model;

import mp2.MsgContent;
import mp2.MsgKey;
import mp2.MsgType;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class GetResponse extends Message {
    private File file;

    public GetResponse(File file) {
        super(MsgType.GET_RESPONSE);
        this.file = file;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        if (this.file == null) {
            jsonObject.put(MsgKey.FILE, MsgContent.NO_FILE_FOUND);
        } else {
            byte[] bytes = null;
            try {
                bytes = Files.readAllBytes(this.file.toPath());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (bytes != null) {
                jsonObject.put(MsgKey.FILE, Base64.getEncoder().encodeToString(bytes));
                System.out.println(Base64.getEncoder().encodeToString(bytes));
            }
        }
        return jsonObject;
    }
}
