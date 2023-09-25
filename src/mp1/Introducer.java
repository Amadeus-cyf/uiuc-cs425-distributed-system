package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Introducer extends BaseServer {
    public static final String IP_ADDRESS = "fa20-cs425-g53-01.cs.illinois.edu";
    public static final int PORT = 3000;
    private final String mode = Mode.GOSSIP;
    private final Sender sender;
    private final Receiver receiver;

    public Introducer() {
        super(
            IP_ADDRESS,
            PORT
        );
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        long heartbeatCounter = 0L;
        this.sender = new Sender(
            this.id,
            this.ipAddress,
            this.port,
            this.membershipList,
            this.modeBuilder,
            this.statusBuilder,
            this.socket
        );
        this.receiver = new Receiver(
            this.id,
            this.ipAddress,
            this.port,
            this.membershipList,
            this.modeBuilder,
            this.statusBuilder,
            this.socket
        );
        this.membershipList.add(new Member(
            this.id,
            this.startingTime,
            heartbeatCounter
        ));
    }

    public static void main(String[] args) {
        Introducer server = new Introducer();
        server.sender.start();
        server.receiver.start();
        ExecutorService checkerThread = Executors.newSingleThreadExecutor();
        checkerThread.execute(new TimeoutChecker(
            server.membershipList,
            server.modeBuilder,
            server.id
        ));
        CommandHandler commandHandler = new CommandHandler(server);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            commandHandler.handleCommand(scanner);
        }
    }

    @Override
    public Sender getSender() {
        return this.sender;
    }
}
