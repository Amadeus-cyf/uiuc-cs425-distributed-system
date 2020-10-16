package mp2;

import mp2.model.GetRequest;
import mp2.model.Message;

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
        Message getRequest = new GetRequest("tes.txt", this.ipAddress, this.port);
        this.socket.send(getRequest.toJSON(), targetIpAddress, targetPort);
    }
}
