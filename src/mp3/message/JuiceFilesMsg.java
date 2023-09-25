package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

import java.util.List;

public class JuiceFilesMsg extends Message {
    private final List<String> filesToJuice;
    private final String intermediatePrefix;
    private final String destFileName;
    private final String juiceExe;
    private final int isDelete;

    public JuiceFilesMsg(
        List<String> filesToJuice,
        String intermediatePrefix,
        String destFileName,
        String juiceExe,
        int isDelete
    ) {
        super(MsgType.JUICE_FILES_MSG);
        this.filesToJuice = filesToJuice;
        this.intermediatePrefix = intermediatePrefix;
        this.destFileName = destFileName;
        this.juiceExe = juiceExe;
        this.isDelete = isDelete;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject.put(
            MsgKey.MSG_TYPE,
            this.msgType
        ).put(
            MsgKey.FILES_TO_JUICE,
            this.filesToJuice
        ).put(
            MsgKey.INTERMEDIATE_PREFIX,
            this.intermediatePrefix
        ).put(
            MsgKey.JUICE_EXE,
            this.juiceExe
        ).put(
            MsgKey.DEST_FILE,
            this.destFileName
        ).put(
            MsgKey.IS_DELETE,
            this.isDelete
        );
    }
}
