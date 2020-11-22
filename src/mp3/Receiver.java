package mp3;

import mp2.DataTransfer;
import mp3.application.MapleJuice;
import mp3.constant.FilePath;
import mp3.constant.MasterInfo;
import mp3.constant.MsgKey;
import mp3.message.MapleAck;
import mp3.message.MapleCompleteMsg;
import mp3.message.Message;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;

public class Receiver {
    protected String ipAddress;
    protected int port;
    protected DataTransfer dataTransfer;
    protected MapleJuice<?, ?> mapleJuice;

    public Receiver(String ipAddress, int port, MapleJuice<?, ?> mapleJuice) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = new DataTransfer(ipAddress, port);
        this.mapleJuice = mapleJuice;
    }

    private void handleMapleFileMsg(JSONObject msgJson) {
        String sourceFileName = msgJson.getString(MsgKey.SOURCE_FILE);
        String splitFileName = msgJson.getString(MsgKey.SPLIT_FILE);
        String localSplitFilePath = FilePath.ROOT + splitFileName;
        StringBuilder sb = new StringBuilder();
        String remoteSplitFilePath = sb.append(FilePath.ROOT).append(FilePath.SPLIT_DIRECTORY).append(splitFileName).toString();
        this.dataTransfer.receiveFile(localSplitFilePath, remoteSplitFilePath, MasterInfo.Master_IP_ADDRESS);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(localSplitFilePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (in == null) {
            return;
        }
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                mapleJuice.maple(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.setLength(0);
        String destFileName = getMapleOutputFileName(sourceFileName);
        mapleJuice.writeMapleOutputToFile(FilePath.ROOT + destFileName);
        Message mapleCompleteMsg = new MapleCompleteMsg(this.ipAddress, this.port, sourceFileName, destFileName);
        this.dataTransfer.send(mapleCompleteMsg.toJSON(), MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }

    private void handleMapleAckRequest(JSONObject jsonObject) {
        String sourceFile = jsonObject.getString(MsgKey.SOURCE_FILE);
        Message mapleAck = new MapleAck(sourceFile);
        this.dataTransfer.send(mapleAck.toJSON(), MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }

    protected String getSplitFilePath(String splitFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(FilePath.SPLIT_DIRECTORY).append(splitFileName).toString();
    }

    protected String getMapleOutputFileName(String sourceFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(sourceFileName).append("_maple_").append(ipAddress).append("_").append(port).toString();
    }
}
