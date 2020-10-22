package mp2;

import mp2.message.*;


import java.util.logging.Logger;

import static mp2.constant.MasterInfo.*;

public class Sender {
    private Logger logger = Logger.getLogger(Sender.class.getName());
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


    public void sendPreGetRequest(String sdfsFileName, String localFileName) {
        Message request = new PreGetRequest(this.ipAddress, this.port, sdfsFileName, localFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        logger.info("PreGet request send to master " + MASTER_IP_ADDRESS + ":" + MASTER_PORT);

    }

    public void sendPrePutRequest(String localFileName, String sdfsFileName) {
        Message request = new PrePutRequest(this.ipAddress, this.port, sdfsFileName, localFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        logger.info("PrePut request send to master " + MASTER_IP_ADDRESS + ":" + MASTER_PORT);
    }

    public void sendPreDelRequest(String sdfsFileName) {
        Message request = new PreDelRequest(this.ipAddress, this.port, sdfsFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        logger.info("PreDelete request send to master " + MASTER_IP_ADDRESS + ":" + MASTER_PORT);
    }

    public void sendLsRequest(String sdfsFileName) {
        Message request = new LsRequest(this.ipAddress, this.port, sdfsFileName);
        this.socket.send(request.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        logger.info("ls Request sends to master");
    }

    public void sendStoreRequest() {
        Message request = new StoreRequest();
        this.socket.send(request.toJSON(), this.ipAddress, this.port);
        logger.info("Receive Store Request");
    }
}