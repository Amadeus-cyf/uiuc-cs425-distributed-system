package mp3.message;

import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import org.json.JSONObject;

import java.util.List;

public class JuiceFilesMsg extends Message {
    private List<String> filesToJuice;
    private String destFileName;
    private String juiceExe;
    private int isDelete;

    public JuiceFilesMsg(List<String> filesToJuice, String destFileName, String juiceExe, int isDelete) {
        super(MsgType.JUICE_FILES_MSG);
        this.filesToJuice = filesToJuice;
        this.destFileName = destFileName;
        this.juiceExe = juiceExe;
        this.isDelete = isDelete;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MsgKey.MSG_TYPE, this.msgType);
        jsonObject.put(MsgKey.FILES_TO_JUICE, this.filesToJuice);
        jsonObject.put(MsgKey.JUICE_EXE, this.juiceExe);
        jsonObject.put(MsgKey.DEST_FILE, this.destFileName);
        jsonObject.put(MsgKey.IS_DELETE, this.isDelete);
        return jsonObject;
    }
}
