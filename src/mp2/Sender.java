package mp2;

import mp2.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.Arrays;

public class Sender {
    private String ipAddress;
    private int port;
    private boolean isMaster;
    private UdpSocket socket;
    private final int BLOCK_SIZE = 4096;

    public Sender(String ipAddress, int port, boolean isMaster, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isMaster = isMaster;
        this.socket = socket;
    }

    public void sendGetRequest(String targetIpAddress, int targetPort) {
        System.out.println(this.ipAddress + " " + this.port);
        Message getRequest = new GetRequest("random.txt", this.ipAddress, this.port);
        this.socket.send(getRequest.toJSON(), targetIpAddress, targetPort);
    }

    public void sendPutRequest(String fileName, String targetIpAddress, int targetPort) {
        // read in the local file to bytes
        File file = new File(fileName);
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (bytes == null) {
            return;
        }
        //send the file blocks to the target server
        int blockSeq = 0;
        int blockNum = bytes.length / BLOCK_SIZE;
        if (bytes.length % BLOCK_SIZE > 0) {
            blockNum++;
        }
        System.out.println("Block NUM: " + blockNum);
        while (blockSeq < blockNum) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {

            }
            int start = blockSeq * BLOCK_SIZE;
            int end = Math.min((blockSeq + 1) * BLOCK_SIZE, bytes.length);
            byte[] block = Arrays.copyOfRange(bytes, start, end);
            Message request = new PutRequest(block, fileName, blockNum, blockSeq);
            blockSeq++;
            this.socket.send(request.toJSON(), targetIpAddress, targetPort);
            System.out.println("Local file" + blockSeq + " sent to " + targetIpAddress + " " + targetPort);
        }
    }

    public void sendDeleteRequest(String fileName, String targetIpAddress, int targetPort) {
        Message request = new DeleteRequest(fileName, targetIpAddress, targetPort);
        this.socket.send(request.toJSON(), targetIpAddress, targetPort);
        System.out.println("Delete request of file " + fileName + " send to " + targetIpAddress + " " + targetPort);
    }
}
