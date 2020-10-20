package mp2;

import mp2.model.ServerInfo;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static mp2.constant.MasterInfo.masterIpAddress;
import static mp2.constant.MasterInfo.masterPort;

public class Master extends BaseServer {
    private Map<String, Queue<JSONObject>> messageMap;
    private Map<String, Boolean> fileStatus;
    private Map<String, Set<ServerInfo>> fileStorageInfo;
    private String ipAddress;
    private int port;

    public Master() {
        this.ipAddress = masterIpAddress;
        this.port = masterPort;
        this.messageMap = new HashMap<>();
        this.fileStatus = new HashMap<>();
        this.fileStorageInfo = new HashMap<>();
    }

    public void run() {
        UdpSocket socket = new UdpSocket(this.ipAddress, this.port);
        Receiver receiver = new MasterReceiver(this.ipAddress, this.port, socket, messageMap, fileStatus, fileStorageInfo);
        Sender sender = new Sender(this.ipAddress, this.port, true, socket);
        ExecutorService receiveThread = Executors.newSingleThreadExecutor();
        receiveThread.execute(new Runnable() {
            @Override
            public void run() {
                receiver.start();
            }
        });
        Scanner scanner = new Scanner(System.in);
        System.out.println(this.ipAddress + ":" + this.port);
        while(true) {
            String line = scanner.nextLine();
            sender.sendPrePutRequest("random1.txt", "random1_sdfs.txt");
        }
    }

    public static void main(String[] args) {
        Master server = new Master();
        server.run();
    }


}
