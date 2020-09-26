package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server extends BaseServer {
    private volatile String mode = Mode.GOSSIP;
    private String status;
    static Logger logger = Logger.getLogger(Server.class.getName());
    public Sender sender;
    public Receiver receiver;
    private Long heartbeatCounter = 0L;


    public Server(String ipAddress, int port) {
        super(ipAddress, port);
    }

    public static void main(String[] args) {
        Server server = new Server("localhost", 3303);
        server.join();
        ExecutorService sendThread= Executors.newSingleThreadExecutor();
        ExecutorService receiveThread = Executors.newSingleThreadExecutor();
        ExecutorService checkerThread = Executors.newSingleThreadExecutor();
        sendThread.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    server.sender.send();
                    try {
                        Thread.sleep(3000);
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
        checkerThread.execute(new TimeoutChecker(server.membershipList, server.mode, server.id));

        while (true) {
            for (Member member : server.membershipList) {
                logger.warning("ID: " + member.getId() + " TIMESTAMP: " + member.getTimestamp());
            }
            try {
                Thread.sleep(3000);
            } catch(Exception ignored) {

            }
        }
    }

    public void join() {
        if (this.status != null && this.status.equals(Status.WORKING)) {
            return;
        }
        this.status = Status.WORKING;
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket, this.heartbeatCounter);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket, this.heartbeatCounter);
        Member member = new Member(this.id, this.startingTime,this.heartbeatCounter);
        this.membershipList.add(member);
        // sender send a message to the ip address and port of the introducer
        this.sender.sendJoinRequest();
    }
}
