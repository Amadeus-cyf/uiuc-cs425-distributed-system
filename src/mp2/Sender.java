package mp2;

import mp2.message.*;

import static mp2.constant.MasterInfo.*;

public class Sender {
    private String ipAddress;
    private int port;
    private DataTransfer socket;

    public Sender(String ipAddress, int port, DataTransfer dataTransfer) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = dataTransfer;
    }


    public void sendPreGetRequest(String sdfsFileName, String localFileName) {
        Message request = new PreGetRequest(this.ipAddress, this.port, sdfsFileName, localFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        System.out.println("PreGet request send to master " + MASTER_IP_ADDRESS + ":" + MASTER_PORT);

    }

    public void sendPrePutRequest(String localFileName, String sdfsFileName) {
        Message request = new PrePutRequest(this.ipAddress, this.port, sdfsFileName, localFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        System.out.println("PrePut request send to master " + MASTER_IP_ADDRESS + ":" + MASTER_PORT);
    }

    public void sendPreDelRequest(String sdfsFileName) {
        Message request = new PreDelRequest(this.ipAddress, this.port, sdfsFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        System.out.println("PreDelete request send to master " + MASTER_IP_ADDRESS + ":" + MASTER_PORT);
    }

    public void sendLsRequest(String sdfsFileName) {
        Message request = new LsRequest(this.ipAddress, this.port, sdfsFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        System.out.println("ls Request sends to master");
    }

    public void sendStoreRequest() {
        Message request = new StoreRequest();
        this.socket.send(request.toJSON(), this.ipAddress, this.port);
    }
}