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
import java.util.logging.Logger;

import static mp2.constant.MasterInfo.*;
import static mp2.constant.MsgContent.FILE_NOT_FOUND;

public class Receiver {
    private Logger logger = Logger.getLogger(Receiver.class.getName());
    protected String ipAddress;
    protected int port;
    protected Set<File> files;
    protected DataTransfer dataTransfer;
    protected final int BLOCK_SIZE = 1024 * 16;

    public Receiver(String ipAddress, int port, DataTransfer socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = socket;
        this.files = new HashSet<>();
        logger.info("Current files: "+ files.toString());
    }

    public void start() {
        while (true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.dataTransfer.receive(receivedPacket);
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
            case(MsgType.GET_RESPONSE):
                receiveGetResponse(msgJson);
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
            case(MsgType.LS_RESPONSE):
                receiveLsResponse(msgJson);
                break;
            case(MsgType.STORE_REQUEST):
                receiveStoreRequest();
                break;
            case(MsgType.ERROR_RESPONSE):
                receiveErrorResponse();
                break;
        }
    }

    protected void receivePreGetResponse(JSONObject msgJson) {
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
            this.dataTransfer.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
            System.out.println("Receive PreGet Response: sending GET ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress +  ":" + port);
        } else{
            sendGetAndReceiveFile(localFileName, sdfsFileName, targetIpAddress, targetPort);
        }
    }

    protected void receivePrePutResponse(JSONObject msgJson) {
        System.out.println("Receive PrePut Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        for (int i = 0; i < servers.length(); i++) {
            JSONObject server = servers.getJSONObject(i);
            String targetIpAddress = server.getString(MsgKey.IP_ADDRESS);
            int targetPort = server.getInt(MsgKey.PORT);
            updateFileAndNotify(localFileName, sdfsFileName, targetIpAddress, targetPort);
        }
        System.out.println("PUT REQUEST SEND");
    }

    protected void receivePreDelResponse(JSONObject msgJson) {
        logger.info("Receive Pre Delete Response from master");
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        int len = servers.length();
        for (int i = 0; i < len; i ++) {
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
                this.dataTransfer.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
                System.out.println("Sending DELETE ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress + ":" + port);
            } else{
                sendDeleteRequest(sdfsFileName, targetIpAddress, targetPort);
            }
        }

    }

    private void sendGetAndReceiveFile(String localFileName, String sdfsFilename, String targetIpAddress, int targetPort) {
        int result = this.dataTransfer.receiveFile(FilePath.ROOT + FilePath.LOCAL_ROOT_DIRECTORY + localFileName,
                FilePath.ROOT + FilePath.SDFS_ROOT_DIRECTORY + sdfsFilename, targetIpAddress);
        if (result == 0) {
            Ack getAck = new Ack(sdfsFilename, this.ipAddress, this.port, MsgType.GET_ACK);
            this.dataTransfer.send(getAck.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
        } else {
            System.out.println("GET FAILED ......");
        }
    }

    private void updateFileAndNotify(String localFileName, String sdfsFileName, String targetIpAddress, int targetPort) {
        File localFile = new File(FilePath.LOCAL_ROOT_DIRECTORY + localFileName);
        System.out.println("send file");
        int result = this.dataTransfer.sendFile(localFile.getAbsolutePath(),
                FilePath.ROOT + FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName, targetIpAddress);
        if (result == 0) {
            PutNotify putNotify = new PutNotify(sdfsFileName);
            this.dataTransfer.send(putNotify.toJSON(), targetIpAddress, targetPort);
        } else {
            System.out.println("PUT FAILED ......");
        }
    }

    private void sendDeleteRequest(String sdfsFileName, String targetIpAddress, int targetPort) {
        Message request = new DeleteRequest(sdfsFileName, targetIpAddress, targetPort);
        this.dataTransfer.send(request.toJSON(), targetIpAddress, targetPort);
        logger.info("Delete request of file " + sdfsFileName + " send to " + targetIpAddress + " " + targetPort);
    }

    /*protected void receiveGetRequest(JSONObject msgJson) {
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        File targetSdfsFile= null;
        for (File file : files) {
            if (file.getName().equals(sdfsFileName)) {
                targetSdfsFile = file;
                break;
            }
        }
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        if(targetSdfsFile == null) {
            Message response = new GetResponse(null, localFileName, sdfsFileName, 0, 0);
            this.socket.send(response.toJSON(), senderIpAddress, senderPort);
        } else {
            this.socket.sendFile(targetSdfsFile.getAbsolutePath(), FilePath.ROOT + FilePath.LOCAL_ROOT_DIRECTORY + localFileName, senderIpAddress);
        }
    }*/

    protected void receiveGetResponse(final JSONObject msgJson) {
        if (msgJson.get(MsgKey.FILE_BLOCK) != null && msgJson.get(MsgKey.FILE_BLOCK).equals(FILE_NOT_FOUND)) {
            logger.info("Receive Get Response " + FILE_NOT_FOUND);
            return;
        }
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        String localFile = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        String sdfsFile = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        //this.socket.receiveFile(sdfsFile, localFile);
    }

    /*
    * replica receive file already updated info and send ack to master
     */
    protected void receivePutNotify(JSONObject msgJson){
        logger.info("Receive Put Request: Receive put request");
        String sdfsFile = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        this.files.add(new File(FilePath.SDFS_ROOT_DIRECTORY + sdfsFile));
        System.out.println("RECEIVE PUT REQUEST FOR " + sdfsFile);
        Ack putAck = new Ack(sdfsFile, ipAddress, port,  MsgType.PUT_ACK);
        this.dataTransfer.send(putAck.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
    }

    protected void receiveDeleteRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        logger.info("Receive Delete Request with filename " + fileName);
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                files.remove(file);                                                     // remove from the file list
                file.delete();                                                          // delete the sdfs file on the disk
                logger.info("Receiver Delete Request: Successfully delete file "+ fileName);
                logger.info("Receiver Delete Request: Current files: "+ files.toString());
                // send ack message to the master server
                String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.DEL_ACK);
                this.dataTransfer.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
                logger.info("Receiver Delete Request: sending DELETE ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress +
                        ":" + port);
                return;
            }
        }
        logger.info("Receiver, receive delete request: " + FILE_NOT_FOUND);
    }

    /*
    * send the sdfs file copy to the target server
     */
    protected void receiveReplicateRequest(JSONObject msgJson) {
        String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        JSONArray targetServers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        for (int i = 0; i < targetServers.length(); i++) {
            JSONObject server  = targetServers.getJSONObject(i);
            String targetIpAddress = server.getString(MsgKey.IP_ADDRESS);
            int targetPort = server.getInt(MsgKey.PORT);
            String sourceAndDest = FilePath.ROOT + FilePath.SDFS_ROOT_DIRECTORY + sdfsFileName;
            this.dataTransfer.sendFile(sourceAndDest, sourceAndDest, targetIpAddress);
        }
    }

    protected void receiveLsResponse(JSONObject msgJson) {
        JSONArray servers = msgJson.getJSONArray(MsgKey.TARGET_SERVERS);
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        int len = servers.length();
        System.out.println("List all the servers stored the file " + fileName + ":");
        for (int i = 0; i < len; i ++) {
            JSONObject server = servers.getJSONObject(i);
            String replicaIpAddress = server.getString("ipAddress");
            int replicaPort = server.getInt("port");
            System.out.println("Receive Ls Response: Replicate server is " + replicaIpAddress + ":" + replicaPort);
        }
    }

    protected void receiveStoreRequest() {
        System.out.println("Print all stored sdfs files on this server: " + files.toString());
    }

    protected void receiveErrorResponse(){
        logger.info("Ls Error Response " + FILE_NOT_FOUND);
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
    protected void writeFile(String inputFilePath, String outputFilePath) {
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