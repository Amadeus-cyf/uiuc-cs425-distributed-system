package mp3;

import mp2.DataTransfer;
import mp3.constant.MasterInfo;
import mp3.message.JuiceRequest;
import mp3.message.MapleRequest;
import mp3.message.Message;

public class Sender {
    private final String ipAddress;
    private final int port;
    private final DataTransfer dataTransfer;

    public Sender(
        String ipAddress,
        int port,
        DataTransfer dataTransfer
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = dataTransfer;
    }

    public void sendMapleRequest(
        String mapleExe,
        int numMaples,
        String intermediatePrefix,
        String sourceFile
    ) {
        Message mapleRequest = new MapleRequest(
            mapleExe,
            numMaples,
            intermediatePrefix,
            sourceFile
        );
        this.dataTransfer.send(
            mapleRequest.toJSON(),
            MasterInfo.Master_IP_ADDRESS,
            MasterInfo.MASTER_PORT
        );
    }

    public void sendJuiceRequest(
        String juiceExe,
        int numJuice,
        String intermediatePrefix,
        String destFile,
        int isDelete
    ) {
        Message juiceRequest = new JuiceRequest(
            juiceExe,
            numJuice,
            intermediatePrefix,
            destFile,
            isDelete
        );
        this.dataTransfer.send(
            juiceRequest.toJSON(),
            MasterInfo.Master_IP_ADDRESS,
            MasterInfo.MASTER_PORT
        );
    }
}
