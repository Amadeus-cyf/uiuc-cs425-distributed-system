package mp2.model;

import mp2.constant.MsgType;

public class PutResponse extends FileBlockMessage {
    public PutResponse(byte[] file, String fileName, int blockNum, int blockSeq) {
        super(MsgType.PUT_RESPONSE, file, fileName, blockNum, blockSeq);
    }
}
