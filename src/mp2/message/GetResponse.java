package mp2.message;

import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

import java.util.Base64;

public class GetResponse extends FileBlockMessage {
    private String sdfsFileName;
    public GetResponse(byte[] file, String localFileName, String sdfsFileName, int blockNum, int blockSeq) {
        super(MsgType.GET_RESPONSE, file, localFileName, blockNum, blockSeq);
        this.sdfsFileName = sdfsFileName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        if (this.file == null) {
            jsonObject.put(MsgKey.FILE_BLOCK, MsgContent.FILE_NOT_FOUND);
        } else {
            if (this.file != null) {
                jsonObject.put(MsgKey.FILE_BLOCK, Base64.getEncoder().encodeToString(this.file));
            }
        }
        jsonObject.put(MsgKey.LOCAL_FILE_NAME, this.fileName);
        jsonObject.put(MsgKey.SDFS_FILE_NAME, this.sdfsFileName);
        jsonObject.put(MsgKey.BLOCK_NUM, this.blockNum);
        jsonObject.put(MsgKey.BLOCK_SEQ, this.blockSeq);
        return jsonObject;
    }
}