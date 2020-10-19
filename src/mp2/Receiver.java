package mp2;

import mp2.constant.MsgContent;
import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.model.GetResponse;
import mp2.model.Message;
import org.json.JSONObject;

import java.io.File;
import java.net.DatagramPacket;
import java.util.*;

public class Receiver {
    private String ipAddress;
    private int port;
    private boolean isMaster;
    private Set<File> files;
    private UdpSocket socket;
    private final int BLOCK_SIZE = 4096;

    public Receiver(String ipAddress, int port, boolean isMaster, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isMaster = isMaster;
        this.socket = socket;
        this.files = new HashSet<>();
        if (this.port == 3000) {
            File file = new File("random.txt");
            files.add(file);
        }
        System.out.println("Current files: "+ files.toString());
        HashMap fileBlockMap = new HashMap<>();
        /*if (this.port == 3000) {
            File file = new File("/Users/amadeus.cyf/Projects/uiuc-cs425-distributed-system/src/mp2/test.pdf");
            files.add(file);
        }*/
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

    private void receiveGetRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.FILE_NAME);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        File target = null;
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                target = file;
                break;
            }
        }
        if(target == null) {
            Message response = new GetResponse(null, fileName,0, 0);
            this.socket.send(response.toJSON(), senderIpAddress, senderPort);
        } else {
            this.socket.sendFile(MsgType.GET_RESPONSE, target, fileName, senderIpAddress, senderPort);
        }
    }

    private void receiveGetResponse(JSONObject msgJson) {
        if (msgJson.get(MsgKey.FILE_BLOCK) != null && msgJson.get(MsgKey.FILE_BLOCK).equals(MsgContent.NO_FILE_FOUND)) {
            System.out.println("No such file");
            return;
        }
        File file = this.socket.receiveFile(msgJson);
        if (file != null) {
            this.files.add(file);
        }
    }

    private void receivePutRequest(JSONObject msgJson){
        System.out.println("receive put response");
        File file = this.socket.receiveFile(msgJson);
        if (file != null) {
            this.files.add(file);
        }
        System.out.println(files.size());
    }

    private void receiveDeleteRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.FILE_NAME);
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
