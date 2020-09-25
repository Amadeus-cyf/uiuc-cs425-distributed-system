package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Introducer extends BaseServer {
    public static final String IP_ADDRESS = "localhost";
    public static final int PORT = 3000;
    private volatile String mode = Mode.ALL_TO_ALL;
    private Sender sender;
    private Receiver receiver;
    private String status;
    static Logger logger = Logger.getLogger(Server.class.getName());

    public Introducer() {
        super(IP_ADDRESS, PORT);
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket);
        this.membershipList.add(new Member(this.id, this.startingTime));
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
                    server.sender.sendAllToAll();
                    try {
                        Thread.sleep(5000);
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
        checkerThread.execute(new TimeoutChecker(server.membershipList, server.mode));

        while (true) {
            for (Member member : server.membershipList) {
                logger.warning("ID: " + member.getId() + " TIMESTAMP: " + member.getTimestamp());
            }
            try {
                Thread.sleep(10000);
            } catch(Exception ignored) {

            }
        }

    }
}
