package mp2;
import mp2.failureDetector.FailureDetector;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends BaseServer {
    private String ipAddress;
    private int port;

    public Server(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void run() {
        UdpSocket socket = new UdpSocket(this.ipAddress, this.port);
        FailureDetector failureDetector = new mp2.failureDetector.Server(this.ipAddress, this.port+1);
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
        CommandHandler commandHandler = new CommandHandler(sender, failureDetector, scanner, receiver);
        System.out.println(this.ipAddress + ":" + this.port);
        failureDetector.run();
        while (true) {
            commandHandler.handleCommand();
        }
    }

    public static void main(String[] args) {
        Server server = new Server("localhost", 3600);
        server.run();
    }
}
