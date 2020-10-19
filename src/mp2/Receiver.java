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
    private Map<String, PriorityQueue<JSONObject>> fileBlockMap;

    public Receiver(String ipAddress, int port, boolean isMaster, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isMaster = isMaster;
        this.socket = socket;
        this.files = new ArrayList<>();
        if (this.port == 3000) {
            File file = new File("random.txt");
            files.add(file);
        }
        System.out.println("Current files: "+ files.toString());
        fileBlockMap = new HashMap<>();
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
                Message response = new GetResponse(block, fileName, blockNum, blockSeq);
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
        String sdfsFileName = msgJson.getString(MsgKey.FILE_NAME);
        PriorityQueue<JSONObject> fileBlocks = fileBlockMap.get(sdfsFileName);
        if (fileBlocks == null) {
            fileBlocks = new PriorityQueue<>(new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    return o1.getInt(MsgKey.BLOCK_SEQ) - o2.getInt(MsgKey.BLOCK_SEQ);
                }
            });
            fileBlockMap.put(sdfsFileName, fileBlocks);
        }
        int blockNum = msgJson.getInt(MsgKey.BLOCK_NUM);
        System.out.println("receive res " + fileBlocks.size() + " " + blockNum);
        fileBlocks.add(msgJson);
        if (fileBlocks.size() >= blockNum) {
            File file = new File("random1.txt");
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

    private void receivePutRequest(JSONObject msgJson){
        if (msgJson.get(MsgKey.FILE_BLOCK) != null && msgJson.get(MsgKey.FILE_BLOCK).equals(MsgContent.NO_FILE_FOUND)) {
            System.out.println("No such file");
            return;
        }
        String sdfsFileName = msgJson.getString(MsgKey.FILE_NAME);
        PriorityQueue<JSONObject> fileBlocks = fileBlockMap.get(sdfsFileName);
        if (fileBlocks == null) {
            fileBlocks = new PriorityQueue<>(new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    return o1.getInt(MsgKey.BLOCK_SEQ) - o2.getInt(MsgKey.BLOCK_SEQ);
                }
            });
            fileBlockMap.put(sdfsFileName, fileBlocks);
        }
        int blockNum = msgJson.getInt(MsgKey.BLOCK_NUM);
        System.out.println("receive from the local file:" + fileBlocks.size() + " " + blockNum);
        fileBlocks.add(msgJson);
        if (fileBlocks.size() >= blockNum) {
            File file = new File("random1.txt");
            files.add(file);
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
