package mp3;

import mp2.DataTransfer;
import mp2.failureDetector.FailureDetector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    protected String ipAddress;
    protected int port;
    protected DataTransfer dataTransfer;

    public Server(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = new DataTransfer(ipAddress, port);
    }

    public void run() {
        FailureDetector failureDetector = new mp2.failureDetector.Server(this.ipAddress, this.port+1);
        Sender sender = new Sender(this.ipAddress, this.port, this.dataTransfer);
        Receiver receiver = new Receiver(this.ipAddress, this.port, this.dataTransfer);
        failureDetector.run();
        CommandHandler commandHandler = new CommandHandler(sender, failureDetector);
        ExecutorService thread = Executors.newFixedThreadPool(1);
        thread.execute(new Runnable() {
            @Override
            public void run() {
                receiver.start();
            }
        });
        commandHandler.run();
    }
    public static void main(String[] args) {
        Server server = new Server("localhost", 3500);
        server.run();
    }
}
