package mp2;

import mp2.constant.FilePath;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.message.*;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

import static mp2.constant.MasterInfo.*;

public class UdpSocket {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;
    private static Logger logger = Logger.getLogger(UdpSocket.class.getName());
    private final int BLOCK_SIZE = 4096;
    private Map<String, PriorityQueue<JSONObject>> fileBlockMap;

    public UdpSocket(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        fileBlockMap = new HashMap<>();
        bind();
    }

    /*
     * bind the socket to the ip address and port
     */
    public void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            this.socket = new DatagramSocket(port, address);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * send message to the target ip address and port
     */
    public void send(JSONObject msg, String targetIpAddress, int targetPort) {
        if (msg == null) {
            return;
        }
        byte[] buffer = msg.toString().getBytes();
        try {
            InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, targetPort);
            this.socket.send(packet);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * @param target the file we send
     * @param fileName the name of the file in receiver where we write content of target into
     */
    public void sendFile(String msgType, File target, String fileName, String targetIpAddress, int targetPort) {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(target.toPath());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (bytes == null) {
            return;
        }
        System.out.println("file length:" + bytes.length);
        int blockSeq = 0;
        int blockNum = bytes.length / BLOCK_SIZE;
        if (bytes.length % BLOCK_SIZE > 0) {
            blockNum++;
        }
        System.out.println("Block NUM: " + blockNum);
        // split file sent into blocks and send file blocks
        while(blockSeq < blockNum){
            try {
                Thread.sleep(10);
            } catch (Exception e) {

            }
            int start = blockSeq * BLOCK_SIZE;
            int end = Math.min((blockSeq + 1) * BLOCK_SIZE, bytes.length);
            byte[] block = Arrays.copyOfRange(bytes, start, end);
            Message response = null;
            if (msgType.equals(MsgType.GET_RESPONSE)) {
                response = new GetResponse(block, fileName, target.getName(), blockNum, blockSeq);
            } else if (msgType.equals(MsgType.PUT_REQUEST)) {
                response = new PutRequest(block, fileName, blockNum, blockSeq);
            }
            if (response != null) {
                blockSeq++;
                send(response.toJSON(), targetIpAddress, targetPort);
//                System.out.println(blockSeq + " sent to" + targetIpAddress + " " + targetPort);
            }
        }
    }

    public void receive(DatagramPacket receivedPacket) {
        try {
            this.socket.receive(receivedPacket);
        } catch(Exception exception) {
            exception.printStackTrace();
        }
    }

    public File receiveFile(JSONObject msgJson) {
        String fileName = null;                                 // the name of file we write received file blocks into
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        if (msgType.equals(MsgType.PUT_REQUEST)) {
            fileName = FilePath.SDFS_ROOT_DIRECTORY + msgJson.getString(MsgKey.SDFS_FILE_NAME);
        } else if (msgType.equals(MsgType.GET_RESPONSE)) {
            fileName = FilePath.LOCAL_ROOT_DIRECTORY + msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        } else {
            return null;
        }
        PriorityQueue<JSONObject> fileBlocks = fileBlockMap.get(fileName);
        if (fileBlocks == null) {
            fileBlocks = new PriorityQueue<>(new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    return o1.getInt(MsgKey.BLOCK_SEQ) - o2.getInt(MsgKey.BLOCK_SEQ);
                }
            });
            fileBlockMap.put(fileName, fileBlocks);
        }
        int blockNum = msgJson.getInt(MsgKey.BLOCK_NUM);
        System.out.println("receive from the local file:" + fileBlocks.size() + " " + blockNum);
        fileBlocks.add(msgJson);
        if (fileBlocks.size() >= blockNum) {
            // the receiver has received all file blocks
            File file = new File(fileName);
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(file);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (fOut == null) {
                return null;
            }
            while (!fileBlocks.isEmpty()) {
                JSONObject block = fileBlocks.poll();
                byte[] bytes = Base64.getDecoder().decode(block.getString(MsgKey.FILE_BLOCK));
                try {
                    fOut.write(bytes, 0, bytes.length);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            try {
                fOut.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            // send ack message to the master server
            if(msgType.equals(MsgType.PUT_REQUEST)) {
               String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
               Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.PUT_ACK);
               this.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
               System.out.println("sending PUT ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress +
                       ":" + port);
               System.out.println(ack.toJSON().toString());
            }
            if(msgType.equals(MsgType.GET_RESPONSE)){
                // send ack message to the master server
                String sdfsFileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
                Message ack = new Ack(sdfsFileName, this.ipAddress, this.port, MsgType.GET_ACK);
                this.send(ack.toJSON(), MASTER_IP_ADDRESS, MASTER_PORT);
                System.out.println("sending GET ACK message to master: sdfsFileName" + sdfsFileName + " from server" + ipAddress +
                        ":" + port);
            }
            return file;
        }
        return null;
    }

    /*
     * whether a server is the master
     */
    private Boolean isMaster(String ipAddress, int port) {
        return ipAddress.equals(MASTER_IP_ADDRESS) && port == MASTER_PORT;
    }
}
