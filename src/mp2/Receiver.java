package mp2;

import mp2.constant.FilePath;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static mp2.constant.MasterSdfsInfo.*;
import static mp2.constant.MsgContent.FILE_NOT_FOUND;

public class Receiver {
    protected String ipAddress;
    protected int port;
    private Set<File> files;
    protected DataTransfer dataTransfer;
    protected final int BLOCK_SIZE = 1024 * 4;

    public Receiver(String ipAddress, int port, DataTransfer dataTransfer) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = dataTransfer;
        this.files = new HashSet<>();
        System.out.println("Current files: "+ files.toString());
    }

    public void start() {
        ExecutorService service = Executors.newFixedThreadPool(5);
        while(true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.dataTransfer.receive(receivedPacket);
            service.execute(() -> {
                String msg = readBytes(buffer, receivedPacket.getLength());
                receive(msg);
            });
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
            case(MsgType.PUT_NOTIFY):
                receivePutNotify(msgJson);
                break;
            case(MsgType.DEL_REQUEST):
                receiveDeleteRequest(msgJson);
                break;
            case(MsgType.REPLICATE_REQUEST):
                receiveReplicateRequest(msgJson);
                break;
            case(MsgType.REPLICATE_NOTIFY):
                receiveReplicateNotify(msgJson);
                break;
            case(MsgType.LS_RESPONSE):
                receiveLsResponse(msgJson);
                break;
            case(MsgType.STORE_REQUEST):
                receiveStoreRequest();
                break;
            case(MsgType.ERROR_RESPONSE):
                receiveErrorResponse(msgJson);
                break;
            case(MsgType.FP_REJOIN_MSG):
                receiveFPReJoinMsg(msgJson);
                break;
            default:
                System.out.println("invalid message type");
        }
    }

    /*
     * download remote file to local and send ack to master if success
     */
    protected void receivePreGetResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Get Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        System.out.println("RECEIVE PreGet Response: download file " + sdfsFileName + "from " + targetIpAddress + ":" + port);
        sendGetAndReceiveFile(localFileName, sdfsFileName, targetIpAddress, targetPort);
    }

    protected void receivePrePutResponse(JSONObject msgJson) {
        System.out.println("Receive PrePut Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        for(int i = 0; i < servers.length(); i++) {
            JSONObject server = servers.getJSONObject(i);
            String targetIpAddress = server.getString(MsgKey.IP_ADDRESS);
            int targetPort = server.getInt(MsgKey.PORT);
            updateFileAndNotify(localFileName, sdfsFileName, targetIpAddress, targetPort);
        }
        System.out.println("PUT REQUEST SEND");
    }

    protected void receivePreDelResponse(JSONObject msgJson) {
        System.out.println("Receive Pre Delete Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        int len = servers.length();
        for(int i = 0; i < len; i ++) {
            JSONObject server = servers.getJSONObject(i);
            String targetIpAddress = server.getString("ipAddress");
            int targetPort = server.getInt("port");
            System.out.println("Receive PreDelResponse: " + (i + 1) + "replica send to " + targetIpAddress + ":" + targetPort);
            // judge if we are sending the file to the server itself
            if(this.ipAddress.equals(targetIpAddress) && this.port == targetPort) {
                File file = new File(FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName);
                files.remove(file); // remove from the file list
                file.delete(); // delete the sdfs file on the disk
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.DEL_ACK);
                this.dataTransfer.send(ack.toJSON(), MASTER_SDFS_IP_ADDRESS, MASTER_SDFS_PORT);
                System.out.println("Sending DELETE ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress + ":" + port);
            } else{
                sendDeleteRequest(sdfsFileName, targetIpAddress, targetPort);
            }
        }

    }

    private void sendGetAndReceiveFile(String localFileName, String sdfsFilename, String targetIpAddress, int targetPort) {
        int result = this.dataTransfer.receiveFile(FilePath.ROOT + FilePath.LOCAL_ROOT_DIRECTORY + localFileName,
                FilePath.ROOT + FilePath.SDFS_ROOT_DIRECTORY + sdfsFilename, targetIpAddress);
        if(result == 0) {
            Ack getAck = new Ack(sdfsFilename, this.ipAddress, this.port, MsgType.GET_ACK);
            this.dataTransfer.send(getAck.toJSON(), MASTER_SDFS_IP_ADDRESS, MASTER_SDFS_PORT);
        } else {
            System.out.println("GET FAILED ......");
        }
    }

    private void updateFileAndNotify(String localFileName, String sdfsFileName, String targetIpAddress, int targetPort) {
        System.out.println("send file");
        int result = this.dataTransfer.sendFile(FilePath.ROOT + FilePath.LOCAL_ROOT_DIRECTORY + localFileName,
                FilePath.ROOT + FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName, targetIpAddress);
        if(result == 0) {
            PutNotify putNotify = new PutNotify(sdfsFileName);
            this.dataTransfer.send(putNotify.toJSON(), targetIpAddress, targetPort);
        } else {
            System.out.println("PUT FAILED ......");
        }
    }

    private void sendDeleteRequest(String sdfsFileName, String targetIpAddress, int targetPort) {
        Message request = new DeleteRequest(sdfsFileName, targetIpAddress, targetPort);
        this.dataTransfer.send(request.toJSON(), targetIpAddress, targetPort);
        System.out.println("Delete request of file " + sdfsFileName + " send to " + targetIpAddress + " " + targetPort);
    }

    /*
    * replica receive file already updated info and send ack to master
     */
    protected void receivePutNotify(JSONObject msgJson){
        System.out.println("Receive Put Request: Receive put request");
        String sdfsFile = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        this.files.add(new File(FilePath.SDFS_ROOT_DIRECTORY + sdfsFile));
        System.out.println("RECEIVE PUT REQUEST FOR " + sdfsFile);
        Ack putAck = new Ack(sdfsFile, ipAddress, port,  MsgType.PUT_ACK);
        this.dataTransfer.send(putAck.toJSON(), MASTER_SDFS_IP_ADDRESS, MASTER_SDFS_PORT);
    }

    protected void receiveDeleteRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        System.out.println("Receive Delete Request with filename " + fileName);
        for(File file : files) {
            if(file.getName().equals(fileName)) {
                files.remove(file);                                                     // remove from the file list
                file.delete();                                                          // delete the sdfs file on the disk
                System.out.println("Receiver Delete Request: Successfully delete file "+ fileName);
                System.out.println("Receiver Delete Request: Current files: "+ files.toString());
                // send ack message to the master server
                String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.DEL_ACK);
                this.dataTransfer.send(ack.toJSON(), MASTER_SDFS_IP_ADDRESS, MASTER_SDFS_PORT);
                System.out.println("Receiver Delete Request: sending DELETE ACK message to master: " + sdfsFileName + " from server " + ipAddress +
                        ":" + port);
                return;
            }
        }
        System.out.println("Receiver, receive delete request: " + FILE_NOT_FOUND);
    }

    /*
    * send the sdfs file copy to the target server
     */
    protected void receiveReplicateRequest(JSONObject msgJson) {
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        System.out.println("Receive Replicate Request for " + sdfsFileName);
        JSONArray targetServers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        for(int i = 0; i < targetServers.length(); i++) {
            JSONObject server  = targetServers.getJSONObject(i);
            String targetIpAddress = server.getString(MsgKey.IP_ADDRESS);
            int targetPort = server.getInt(MsgKey.PORT);
            System.out.println("FILE SEND TO " + targetIpAddress + ":" + targetPort);
            String sourceAndDest = FilePath.ROOT + FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName;
            int result = this.dataTransfer.sendFile(sourceAndDest, sourceAndDest, targetIpAddress);
            System.out.println("SCP result is " + result);
            if(result == 0) {
                System.out.println("NOTIFY " + targetIpAddress + ":" + targetPort);
                ReplicateNotify replicateNotify = new ReplicateNotify(sdfsFileName);
                this.dataTransfer.send(replicateNotify.toJSON(), targetIpAddress, targetPort);
            }
        }
    }

    protected void receiveReplicateNotify(JSONObject msgJson) {
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        this.files.add(new File(FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName));
        Ack replicateAck = new Ack(sdfsFileName, ipAddress, port,  MsgType.PUT_ACK);
        this.dataTransfer.send(replicateAck.toJSON(), MASTER_SDFS_IP_ADDRESS, MASTER_SDFS_PORT);
    }

    protected void receiveLsResponse(JSONObject msgJson) {
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        int len = servers.length();
        System.out.println("List all the servers stored the file " + fileName + ":");
        for(int i = 0; i < len; i ++) {
            JSONObject server = servers.getJSONObject(i);
            String replicaIpAddress = server.getString("ipAddress");
            int replicaPort = server.getInt("port");
            System.out.println("Receive Ls Response: Replicate server is " + replicaIpAddress + ":" + replicaPort);
        }
    }

    protected void receiveStoreRequest() {
        System.out.println("Print all stored sdfs files on this server: ");
        for(File file : this.files) {
            System.out.println(file.toPath());
        }
    }

    protected void receiveErrorResponse(JSONObject msgJson){
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String error = msgJson.getString(MsgKey.ERROR);
        if(sdfsFileName != null) {
            System.out.println(error + ": " + sdfsFileName);
        } else {
            System.out.println(FILE_NOT_FOUND);
        }
    }

    private void receiveFPReJoinMsg(JSONObject msgJson) {
        System.out.println("Receive REJOIN/FALSE POSITIVE MESSAGE");
        this.files.clear();
    }

    /*
     * turn bytes into string
     */
    protected String readBytes(byte[] packet, int length) {
        if(packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            sb.append((char)(packet[i]));
        }
        return sb.toString();
    }
}
