package mp2;

import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.DatagramPacket;
import java.util.*;

public class Receiver {
    protected String ipAddress;
    protected int port;
    protected Set<File> files;
    protected UdpSocket socket;
    private final int BLOCK_SIZE = 4096;

    public Receiver(String ipAddress, int port, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = socket;
        this.files = new HashSet<>();
//        if (this.port == 3000) {
//            File file = new File("random.txt");
//            files.add(file);
//        }
        System.out.println("Current files: "+ files.toString());
    }

    public void start() {
        while (true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.socket.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            receive(msg);
        }
    }

    private void receive(String msg) {
        JSONObject msgJson  = new JSONObject(msg);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        switch(msgType) {
            case(MsgType.PRE_GET_RESPONSE):
                receivePreGetResponse(msgJson);
                break;
            case(MsgType.PRE_PUT_RESPONSE):
                receivePrePutResponse(msgJson);
                break;
            case(MsgType.PRE_DEL_RESPONSE):
                receivePreDelResponse(msgJson);
            case(MsgType.GET_REQUEST):
                receiveGetRequest(msgJson);
                break;
            case(MsgType.GET_RESPONSE):
                receiveGetResponse(msgJson);
                break;
            case(MsgType.PUT_REQUEST):
                receivePutRequest(msgJson);
                break;
            case(MsgType.DEL_REQUEST):
                receiveDeleteRequest(msgJson);
                break;
        }
    }

    private void receivePreGetResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Get Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        sendGetRequest(localFileName, sdfsFileName, targetIpAddress, targetPort);
    }

    private void receivePrePutResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Put Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        int len = servers.length();
        for (int i = 0; i < len; i ++) {
            ServerInfo server = (ServerInfo) servers.get(i);
            System.out.println("receivePrePutResponse: " + (i + 1) + "replica send to " + server.getIpAddress() + ":" + server.getPort());
            sendPutRequest(localFileName, sdfsFileName, server.getIpAddress(), server.getPort());
        }

    }

    private void receivePreDelResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Delete Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        int len = servers.length();
        for (int i = 0; i < len; i ++) {
            ServerInfo server = (ServerInfo) servers.get(i);
            System.out.println("receivePreDelResponse: " + (i + 1) + "replica send to " + server.getIpAddress() + ":" + server.getPort());
            sendDeleteRequest(sdfsFileName, server.getIpAddress(), server.getPort());
        }
    }

    public void sendGetRequest(String localFileName, String sdfsFilename, String targetIpAddress, int targetPort) {
        Message getRequest = new GetRequest(sdfsFilename, localFileName, this.ipAddress, this.port);
        this.socket.send(getRequest.toJSON(), targetIpAddress, targetPort);
        System.out.println("Send Get Request to server " + targetIpAddress + ":" + targetPort);
    }

    public void sendPutRequest(String localFileName, String sdfsFileName, String targetIpAddress, int targetPort) {
        File localFile = new File(localFileName);
        if (localFile.exists()) {
            this.socket.sendFile(MsgType.PUT_REQUEST, localFile, sdfsFileName, targetIpAddress, targetPort);
            System.out.println("Send Put Request to server " + targetIpAddress + ":" + targetPort);
        } else {
            System.out.println("PUT REQUEST: LOCAL FILE NOT EXISTS");
        }
    }

    public void sendDeleteRequest(String sdfsFileName, String targetIpAddress, int targetPort) {
        Message request = new DeleteRequest(sdfsFileName, targetIpAddress, targetPort);
        this.socket.send(request.toJSON(), targetIpAddress, targetPort);
        System.out.println("Delete request of file " + sdfsFileName + " send to " + targetIpAddress + " " + targetPort);
    }

    protected void receiveGetRequest(JSONObject msgJson) {
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        File target = null;
        for (File file : files) {
            if (file.getName().equals(sdfsFileName)) {
                target = file;
                break;
            }
        }
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        if(target == null) {
            Message response = new GetResponse(null, localFileName,0, 0);
            this.socket.send(response.toJSON(), senderIpAddress, senderPort);
        } else {
            this.socket.sendFile(MsgType.GET_RESPONSE, target, localFileName, senderIpAddress, senderPort);
        }
    }

    protected void receiveGetResponse(JSONObject msgJson) {
        if (msgJson.get(MsgKey.FILE_BLOCK) != null && msgJson.get(MsgKey.FILE_BLOCK).equals(MsgContent.FILE_NOT_FOUND)) {
            System.out.println("No such file");
            return;
        }
        File file = this.socket.receiveFile(msgJson);
        if (file != null) {
            this.files.add(file);
        }
    }

    protected void receivePutRequest(JSONObject msgJson){
        System.out.println("receive put response");
        File file = this.socket.receiveFile(msgJson);
        if (file != null) {
            this.files.add(file);
        }
        System.out.println(files.size());
    }

    protected void receiveDeleteRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                files.remove(file); // remove from the file list
                file.delete(); // delete the sdfs file on the disk
                System.out.println("Successfully delete file "+ fileName);
                System.out.println("Current files: "+ files.toString());
                return;
            }
        }
        System.out.println("Delete file not found");
        return;
    }

    /*
     * turn bytes into string
     */
    private String readBytes(byte[] packet, int length) {
        if (packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char)(packet[i]));
        }
        return sb.toString();
    }
}
