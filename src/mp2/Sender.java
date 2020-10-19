package mp2;

import mp2.constant.MsgType;
import mp2.model.GetRequest;
import mp2.model.Message;

import java.io.File;

public class Sender {
    private String ipAddress;
    private int port;
    private boolean isMaster;
    private UdpSocket socket;

    public Sender(String ipAddress, int port, boolean isMaster, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isMaster = isMaster;
        this.socket = socket;
    }

    public void sendGetRequest(String targetIpAddress, int targetPort) {
        System.out.println(this.ipAddress + " " + this.port);
        Message getRequest = new GetRequest("test.pdf", this.ipAddress, this.port);
        this.socket.send(getRequest.toJSON(), targetIpAddress, targetPort);
    }

    public void sendPutRequest(String localFileName, String sdfsFileName, String targetIpAddress, int targetPort) {
        // read in the local file to bytes
        File localFile = new File(localFileName);
        if (localFile.exists()) {
            this.socket.sendFile(MsgType.PUT_REQUEST, localFile, sdfsFileName, targetIpAddress, targetPort);
        }
    }
}
