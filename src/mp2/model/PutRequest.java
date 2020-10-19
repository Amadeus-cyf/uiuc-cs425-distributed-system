package mp2.model;

import mp2.constant.MsgType;
import org.json.JSONObject;
import java.util.Base64;




public class PutRequest extends FileBlockMessage {
    public PutRequest(byte[] file, String fileName, int blockNum, int blockSeq) {
        super(MsgType.PUT_REQUEST, file, fileName, blockNum, blockSeq);
    }
}
