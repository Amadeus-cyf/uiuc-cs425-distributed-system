package mp3;

import mp2.DataTransfer;

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
        Sender sender = new Sender(this.ipAddress, this.port, this.dataTransfer);
        Receiver receiver = new Receiver(this.ipAddress, this.port, this.dataTransfer);
        CommandHandler commandHandler = new CommandHandler(sender);
        ExecutorService thread = Executors.newFixedThreadPool(1);
        thread.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        commandHandler.run();
    }

    public void main(String[] args) {
        Server server = new Server("localhost", 3100);
        server.run();
    }
}
