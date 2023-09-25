package mp2;

import mp2.failureDetector.FailureDetector;

import java.util.Scanner;

public class Server {
    private final String ipAddress;
    private final int port;

    public Server(
        String ipAddress,
        int port
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("enter ip address");
            return;
        }
        Server server = new Server(
            args[0],
            3300
        );
        server.run();
    }

    public void run() {
        DataTransfer dataTransfer = new DataTransfer(
            this.ipAddress,
            this.port
        );
        FailureDetector failureDetector = new mp2.failureDetector.Server(
            this.ipAddress,
            this.port + 1
        );
        Receiver receiver = new Receiver(
            this.ipAddress,
            this.port,
            dataTransfer
        );
        Sender sender = new Sender(
            this.ipAddress,
            this.port,
            dataTransfer
        );
        receiver.start();
        Scanner scanner = new Scanner(System.in);
        CommandHandler commandHandler = new CommandHandler(
            sender,
            failureDetector,
            scanner,
            receiver
        );
        System.out.println(this.ipAddress + ":" + this.port);
        failureDetector.run();
        while (true) {
            commandHandler.handleCommand();
        }
    }
}
