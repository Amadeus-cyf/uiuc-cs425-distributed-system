package mp2;

import mp2.constant.FilePath;
import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;

import static mp2.constant.MasterInfo.*;

public class Receiver {
    protected String ipAddress;
    protected int port;
    protected Set<File> files;
    protected UdpSocket socket;
    protected final int BLOCK_SIZE = 4096;

    public Receiver(String ipAddress, int port, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = socket;
        this.files = new HashSet<>();
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
            case (MsgType.PRE_DEL_RESPONSE):
                receivePreDelResponse(msgJson);
                break;
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
            case(MsgType.REPLICATE_REQUEST):
                receiveReplicateRequest(msgJson);
                break;
        }
    }

    private void receivePreGetResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Get Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        if(this.ipAddress.equals(targetIpAddress) && this.port == targetPort){
            String inputName = FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName;
            String outputName = FilePath.LOCAL_ROOT_DIRECTORY + localFileName;
            writeFile(inputName,outputName);
            Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.GET_ACK);
            this.socket.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
            System.out.println("sending GET ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress +  ":" + port);
        } else{
            sendGetRequest(localFileName, sdfsFileName, targetIpAddress, targetPort);
        }
    }

    private void receivePrePutResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Put Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        int len = servers.length();
        System.out.println(servers.toString());
        for (int i = 0; i < len; i ++) {
            JSONObject server = servers.getJSONObject(i);
            String targetIpAddress = server.getString("ipAddress");
            int targetPort = server.getInt("port");
            System.out.println("receivePrePutResponse: " + (i + 1) + "replica send to " + targetIpAddress + ":" + targetPort);
            // judge if we are sending the file to the server itself
            if(targetIpAddress.equals(this.ipAddress) && targetPort == this.port){
                files.add(new File(sdfsFileName));
                String inputName = FilePath.LOCAL_ROOT_DIRECTORY + localFileName;
                String outputName = FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName;
                writeFile(inputName,outputName);
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.PUT_ACK);
                this.socket.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
                System.out.println("sending PUT ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress + ":" + port);
            } else{
                sendPutRequest(localFileName, sdfsFileName, targetIpAddress, targetPort);
            }
        }
    }

    private void receivePreDelResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Delete Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        int len = servers.length();
        for (int i = 0; i < len; i ++) {
            JSONObject server = servers.getJSONObject(i);
            String targetIpAddress = server.getString("ipAddress");
            int targetPort = server.getInt("port");
            System.out.println("receivePreDelResponse: " + (i + 1) + "replica send to " + targetIpAddress + ":" + targetPort);
            // judge if we are sending the file to the server itself
            if(this.ipAddress.equals(targetIpAddress) && this.port == targetPort) {
                File file = new File(FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName);
                files.remove(file.getName()); // remove from the file list
                file.delete(); // delete the sdfs file on the disk
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.DEL_ACK);
                this.socket.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
                System.out.println("sending DELETE ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress + ":" + port);
            } else{
                sendDeleteRequest(sdfsFileName, targetIpAddress, targetPort);
            }
        }

    }

    public void sendGetRequest(String localFileName, String sdfsFilename, String targetIpAddress, int targetPort) {
        Message getRequest = new GetRequest(sdfsFilename, localFileName, this.ipAddress, this.port);
        this.socket.send(getRequest.toJSON(), targetIpAddress, targetPort);
        System.out.println("Send Get Request to server " + targetIpAddress + ":" + targetPort);
    }

    public void sendPutRequest(String localFileName, String sdfsFileName, String targetIpAddress, int targetPort) {
        File localFile = new File(FilePath.LOCAL_ROOT_DIRECTORY + localFileName);
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
        System.out.println("receive get request from " + senderIpAddress + ":" + senderPort +"with local fileName "
                + localFileName + "and sdfs fileName " + target.getName());
        if(target == null) {
            Message response = new GetResponse(null, localFileName, sdfsFileName, 0, 0);
            this.socket.send(response.toJSON(), senderIpAddress, senderPort);
        } else {
            this.socket.sendFile(MsgType.GET_RESPONSE, target, localFileName, senderIpAddress, senderPort);
        }
    }

    protected void receiveGetResponse(JSONObject msgJson) {
        System.out.println("receive get response");
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
        System.out.println("receive put request");
        File file = this.socket.receiveFile(msgJson);
        if (file != null) {
            this.files.add(file);
        }
        System.out.println(files.size());
    }

    protected void receiveDeleteRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        System.out.println("receive delete request with filename " + fileName);
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                files.remove(file);                                                     // remove from the file list
                file.delete();                                                          // delete the sdfs file on the disk
                System.out.println("Successfully delete file "+ fileName);
                System.out.println("Current files: "+ files.toString());
                // send ack message to the master server
                String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.DEL_ACK);
                this.socket.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
                System.out.println("sending DELETE ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress +
                        ":" + port);
                return;
            }
        }
        System.out.println("Delete file not found");
        return;
    }

    /*
    * send the sdfs file copy to the target server
     */
    protected void receiveReplicateRequest(JSONObject msgJson) {
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        File sdfsFile = new File(FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName);
        this.socket.sendFile(MsgType.PUT_REQUEST, sdfsFile, sdfsFileName, targetIpAddress, targetPort);
    }

    /*
     * turn bytes into string
     */
    protected String readBytes(byte[] packet, int length) {
        if (packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char)(packet[i]));
        }
        return sb.toString();
    }

    /*
    * writing a file from the input to the ouput
     */
    private void writeFile(String inputFilePath, String outputFilePath) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(inputFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int f = 0;
        try {
            while ((f = in.read()) != -1) {
                out.write(f);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}