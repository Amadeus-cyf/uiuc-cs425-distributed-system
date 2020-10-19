package mp2.model;

import mp2.constant.MsgType;

public class GetResponse extends FileBlockMessage {
    public GetResponse(byte[] file, String fileName, int blockNum, int blockSeq) {
        super(MsgType.GET_RESPONSE, file, fileName, blockNum, blockSeq);
    }
}