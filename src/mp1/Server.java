package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.ArrayList;
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
        this.ipAddress = ipAddress;
        this.port = port;
        this.membershipList = new ArrayList<>();
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        bind();
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.mode, this.socket);

        this.membershipList.add(new Member("localhost_3000_" + startingTime.toString(), startingTime));
        this.membershipList.add(new Member("localhost_4000_" + startingTime.toString(), startingTime));
        this.membershipList.add(new Member("localhost_5000_" + startingTime.toString(), startingTime));
    }

    public static void main(String[] args) {
        Server server = new Server("localhost", 4000);
        ExecutorService sendThread= Executors.newSingleThreadExecutor();
        ExecutorService receiveThread = Executors.newSingleThreadExecutor();
        ExecutorService checkerThread = Executors.newSingleThreadExecutor();
        sendThread.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    server.sender.sendAllToAll();
                    try {
                        Thread.sleep(200);
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
                Thread.sleep(1000);
            } catch(Exception ignored) {

            }
        }
    }

    /*private void join() {
        if (this.isIntroducer || (this.status != null && this.status.equals(Status.WORKING))) {
            return;
        }
        this.id = createId();
        this.startingTime = new Timestamp(System.currentTimeMillis());
        Member member = new Member(this.id, this.startingTime);
        // create sender and receiver
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.mode);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.mode);
        // sender send a message to the ip address and port of the introducer
        sender.sendMembership(Introducer.IP_ADDRESS, Introducer.PORT);
        receiver.receiveAndInitMembership();
    }*/
}
