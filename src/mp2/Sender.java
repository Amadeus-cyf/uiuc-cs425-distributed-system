package mp2;

import mp2.constant.MsgKey;
import mp2.model.*;
import mp2.constant.MsgType;


import java.io.File;

import static mp2.constant.MasterInfo.masterIpAddress;
import static mp2.constant.MasterInfo.masterPort;

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


    public void sendPreGetRequest(String sdfsFileName, String localFileName) {
        Message request = new PreGetRequest(this.ipAddress, this.port, sdfsFileName, localFileName);
        this.socket.send(request.toJSON(), masterIpAddress, masterPort);
        System.out.println("Pre Get request send to master " + masterIpAddress + ":" + masterPort);

    }

    public void sendPrePutRequest(String localFileName, String sdfsFileName) {
        Message request = new PrePutRequest(this.ipAddress, this.port, sdfsFileName, localFileName);
        this.socket.send(request.toJSON(), masterIpAddress, masterPort);
        System.out.println("Pre Put request send to master " + masterIpAddress + ":" + masterPort);
    }

    public void sendPreDelRequest(String sdfsFileName) {
        Message request = new PreDelRequest(this.ipAddress, this.port, sdfsFileName);
        this.socket.send(request.toJSON(), masterIpAddress, masterPort);
        System.out.println("Pre Delete request send to master " + masterIpAddress + ":" + masterPort);
    }
}