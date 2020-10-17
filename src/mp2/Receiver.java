package mp2;

import mp2.model.GetResponse;
import mp2.model.Message;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.util.*;

public class Receiver {
    private String ipAddress;
    private int port;
    private boolean isMaster;
    private List<File> files;
    private UdpSocket socket;
    private final int BLOCK_SIZE = 4096;
    private PriorityQueue<JSONObject> fileBlocks;

    public Receiver(String ipAddress, int port, boolean isMaster, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isMaster = isMaster;
        this.socket = socket;
        this.files = new ArrayList<>();
        if (this.port == 3000) {
            File file = new File("/Users/amadeus.cyf/Projects/uiuc-cs425-distributed-system/src/mp2/test.pdf");
            files.add(file);
        }
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
            Message response = new GetResponse(null, 0, 0);
            this.socket.send(response.toJSON(), senderIpAddress, senderPort);
        } else {
            byte[] bytes = null;
            try {
                bytes = Files.readAllBytes(target.toPath());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (bytes == null) {
                return;
            }
            int blockSeq = 0;
            int blockNum = bytes.length / BLOCK_SIZE;
            if (bytes.length % BLOCK_SIZE > 0) {
                blockNum++;
            }
            System.out.println("Block NUM: " + blockNum);
            while(blockSeq < blockNum){
                try {
                    Thread.sleep(10);
                } catch (Exception e) {

                }
                int start = blockSeq * BLOCK_SIZE;
                int end = Math.min((blockSeq + 1) * BLOCK_SIZE, bytes.length);
                byte[] block = Arrays.copyOfRange(bytes, start, end);
                Message response = new GetResponse(block, blockNum, blockSeq);
                blockSeq++;
                this.socket.send(response.toJSON(), senderIpAddress, senderPort);
                System.out.println(blockSeq + " sent to" + senderIpAddress + " " + senderPort);
            }
        }
    }

    private void receiveGetResponse(JSONObject msgJson) {
        if (msgJson.get(MsgKey.FILE_BLOCK) != null && msgJson.get(MsgKey.FILE_BLOCK).equals(MsgContent.NO_FILE_FOUND)) {
            System.out.println("No such file");
            return;
        }
        if (this.fileBlocks == null) {
            this.fileBlocks = new PriorityQueue<>(new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    return o1.getInt(MsgKey.BLOCK_SEQ) - o2.getInt(MsgKey.BLOCK_SEQ);
                }
            });
        }
        int blockNum = msgJson.getInt(MsgKey.BLOCK_NUM);
        System.out.println("receive res " + fileBlocks.size() + " " + blockNum);
        fileBlocks.add(msgJson);
        if (fileBlocks.size() >= blockNum) {
            File file = new File("test1.pdf");
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(file);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (fOut == null) {
                return;
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
        }
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
