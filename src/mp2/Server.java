package mp2;
import mp2.failureDetector.FailureDetector;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends BaseServer {
    private String ipAddress;
    private int port;
    private FailureDetector failureDetector;

    public Server(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.failureDetector = new mp2.failureDetector.Server(this.ipAddress, this.port);
    }

    public void run() {
        UdpSocket socket = new UdpSocket(this.ipAddress, this.port);
        Receiver receiver = new Receiver(this.ipAddress, this.port, socket);
        Sender sender = new Sender(this.ipAddress, this.port, false, socket);
        ExecutorService receiveThread = Executors.newSingleThreadExecutor();
        receiveThread.execute(new Runnable() {
            @Override
            public void run() {
                receiver.start();
            }
        });
        Scanner scanner = new Scanner(System.in);
        System.out.println(this.ipAddress + ":" + this.port);
        // this.failureDetector.run();
        while(true) {
            String line = scanner.nextLine();
            sender.sendPrePutRequest("random.txt", "random_sdfs.txt");
            sender.sendPreGetRequest("random_sdfs.txt", "random_copy.txt");
            sender.sendPreDelRequest("random_sdfs.txt");
        }
    }

    public static void main(String[] args) {
        Server server = new Server("localhost", 3400);
        server.run();
    }
}
