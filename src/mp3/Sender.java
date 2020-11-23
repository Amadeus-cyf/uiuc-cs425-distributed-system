package mp3;

import mp2.DataTransfer;
import mp3.constant.MasterInfo;
import mp3.message.MapleRequest;
import mp3.message.Message;

public class Sender {
    private String ipAddress;
    private int port;
    private DataTransfer dataTransfer;

    public Sender(String ipAddress, int port, DataTransfer dataTransfer) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = dataTransfer;
    }

    public void sendMapleRequest(String mapleExe, int numMaples, String intermediatePrefix, String sourceFile) {
        Message mapleRequest = new MapleRequest(mapleExe, numMaples, intermediatePrefix, sourceFile);
        this.dataTransfer.send(mapleRequest.toJSON(), MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }
}
