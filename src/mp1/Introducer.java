package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Introducer extends BaseServer {
    public static final String IP_ADDRESS = "localhost";
    public static final int PORT = 3000;
    private StringBuilder modeBuilder;
    private volatile String mode = Mode.GOSSIP;
    private Sender sender;
    private Receiver receiver;
    private String status;
    static Logger logger = Logger.getLogger(Server.class.getName());
    private Long heartbeatCounter = 0L;

    public Introducer() {
        super(IP_ADDRESS, PORT);
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        this.modeBuilder = new StringBuilder();
        this.modeBuilder.append(Mode.GOSSIP);
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.modeBuilder, this.socket, this.heartbeatCounter);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.modeBuilder, this.socket, this.heartbeatCounter);
        this.membershipList.add(new Member(this.id, this.startingTime, this.heartbeatCounter));
    }

    public static void main(String[] args) {
        Introducer server = new Introducer();
        ExecutorService sendThread= Executors.newSingleThreadExecutor();
        ExecutorService receiveThread = Executors.newSingleThreadExecutor();
        ExecutorService checkerThread = Executors.newSingleThreadExecutor();
        sendThread.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    server.sender.send();
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {

                    }
                }
            }
        });
        receiveThread.execute(new Runnable() {
            @Override
            public void run() {
                server.receiver.start();
            }
        });
        checkerThread.execute(new TimeoutChecker(server.membershipList, server.modeBuilder, server.id));
        handleCommandInput(server);
    }

    private static void handleCommandInput(Introducer server) {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String command = scanner.nextLine();
            if (command.equals("C")) {
                String newMode = server.modeBuilder.toString().equals(Mode.GOSSIP) ? Mode.ALL_TO_ALL : Mode.GOSSIP;
                logger.warning(newMode);
                server.sender.switchMode(newMode);
            }
        }
    }
}
