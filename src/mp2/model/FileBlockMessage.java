package mp2.model;

import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import org.json.JSONObject;

import java.util.Base64;

public class FileBlockMessage extends Message {
    protected String fileName;              // filename is the name of file in receiver where we write file blocks into
    protected byte[] file;
    protected int blockSeq;
    protected int blockNum;

    public FileBlockMessage(String msgType, byte[] file, String fileName, int blockNum, int blockSeq) {
        super(msgType);
        this.file = file;
        this.fileName = fileName;
        this.blockNum = blockNum;
        this.blockSeq = blockSeq;
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
        if (msgType.equals(MsgType.GET_RESPONSE)) {
            jsonObject.put(MsgKey.LOCAL_FILE_NAME, this.fileName);
        } else if (msgType.equals(MsgType.PUT_REQUEST)) {
            jsonObject.put(MsgKey.SDFS_FILE_NAME, this.fileName);
        }
        jsonObject.put(MsgKey.BLOCK_NUM, this.blockNum);
        jsonObject.put(MsgKey.BLOCK_SEQ, this.blockSeq);
        return jsonObject;
    }
}
