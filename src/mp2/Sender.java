package mp2;

import mp2.message.LsRequest;
import mp2.message.Message;
import mp2.message.PreDelRequest;
import mp2.message.PreGetRequest;
import mp2.message.PrePutRequest;
import mp2.message.StoreRequest;

import static mp2.constant.MasterSdfsInfo.MASTER_SDFS_IP_ADDRESS;
import static mp2.constant.MasterSdfsInfo.MASTER_SDFS_PORT;

public class Sender {
    private final String ipAddress;
    private final int port;
    private final DataTransfer socket;

    public Sender(
        String ipAddress,
        int port,
        DataTransfer dataTransfer
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = dataTransfer;
    }

    public void sendPreGetRequest(
        String sdfsFileName,
        String localFileName
    ) {
        Message request = new PreGetRequest(
            this.ipAddress,
            this.port,
            sdfsFileName,
            localFileName
        );
        this.socket.send(
            request.toJSON(),
            MASTER_SDFS_IP_ADDRESS,
            MASTER_SDFS_PORT
        );
        System.out.println("PreGet request send to master " + MASTER_SDFS_IP_ADDRESS + ":" + MASTER_SDFS_PORT);

    }

    public void sendPrePutRequest(
        String localFileName,
        String sdfsFileName
    ) {
        Message request = new PrePutRequest(
            this.ipAddress,
            this.port,
            sdfsFileName,
            localFileName
        );
        this.socket.send(
            request.toJSON(),
            MASTER_SDFS_IP_ADDRESS,
            MASTER_SDFS_PORT
        );
        System.out.println("PrePut request send to master " + MASTER_SDFS_IP_ADDRESS + ":" + MASTER_SDFS_PORT);
    }

    public void sendPreDelRequest(String sdfsFileName) {
        Message request = new PreDelRequest(
            this.ipAddress,
            this.port,
            sdfsFileName
        );
        this.socket.send(
            request.toJSON(),
            MASTER_SDFS_IP_ADDRESS,
            MASTER_SDFS_PORT
        );
        System.out.println("PreDelete request send to master " + MASTER_SDFS_IP_ADDRESS + ":" + MASTER_SDFS_PORT);
    }

    public void sendLsRequest(String sdfsFileName) {
        Message request = new LsRequest(
            this.ipAddress,
            this.port,
            sdfsFileName
        );
        this.socket.send(
            request.toJSON(),
            MASTER_SDFS_IP_ADDRESS,
            MASTER_SDFS_PORT
        );
        System.out.println("ls Request sends to master");
    }

    public void sendStoreRequest() {
        Message request = new StoreRequest();
        this.socket.send(
            request.toJSON(),
            this.ipAddress,
            this.port
        );
    }
}