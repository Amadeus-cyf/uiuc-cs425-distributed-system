package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server extends BaseServer {
    private volatile String mode = Mode.ALL_TO_ALL;
    private String status;
    static Logger logger = Logger.getLogger(Server.class.getName());
    public Sender sender;
    public Receiver receiver;


    public Server(String ipAddress, int port) {
        super(ipAddress, port);
    }

    public static void main(String[] args) {
        Server server = new Server("localhost", 4000);
        server.join();
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

    public void join() {
        if (this.status != null && this.status.equals(Status.WORKING)) {
            return;
        }
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket);
        Member member = new Member(this.id, this.startingTime);
        this.membershipList.add(member);
        // sender send a message to the ip address and port of the introducer
        this.sender.sendJoinRequest();
    }
}
