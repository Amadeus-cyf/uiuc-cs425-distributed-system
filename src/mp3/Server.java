package mp3;

import mp2.DataTransfer;
import mp2.failureDetector.FailureDetector;

public class Server {
    protected String ipAddress;
    protected int port;
    protected DataTransfer dataTransfer;

    public Server(
        String ipAddress,
        int port
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = new DataTransfer(
            ipAddress,
            port
        );
    }

    static void main(String[] args) {
        Server server = new Server(
            args[0],
            3000
        );
        server.run();
    }

    public void run() {
        FailureDetector failureDetector = new mp2.failureDetector.Server(
            this.ipAddress,
            this.port + 1
        );
        Sender sender = new Sender(
            this.ipAddress,
            this.port,
            this.dataTransfer
        );
        Receiver receiver = new Receiver(
            this.ipAddress,
            this.port,
            this.dataTransfer
        );
        failureDetector.run();
        CommandHandler commandHandler = new CommandHandler(
            sender,
            failureDetector
        );
        receiver.start();
        commandHandler.run();
    }
}
