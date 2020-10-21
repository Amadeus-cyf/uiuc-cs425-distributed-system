package mp2.failureDetector;

import mp2.failureDetector.model.Member;

import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Introducer extends FailureDetector {
    public static final String IP_ADDRESS = "fa20-cs425-g53-01.cs.illinois.edu";
    public static final int PORT = 3000;
    private volatile String mode = Mode.GOSSIP;
    private Sender sender;
    private Receiver receiver;
    private Long heartbeatCounter = 0L;

    public Introducer() {
        super(IP_ADDRESS, PORT);
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.modeBuilder, this.statusBuilder, this.socket, this.heartbeatCounter);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.modeBuilder, this.statusBuilder, this.socket, this.heartbeatCounter);
        this.membershipList.add(new Member(this.id, this.startingTime, this.heartbeatCounter));
    }

    @Override
    public void run() {
        ExecutorService sendThread= Executors.newSingleThreadExecutor();
        ExecutorService receiveThread = Executors.newSingleThreadExecutor();
        ExecutorService checkerThread = Executors.newSingleThreadExecutor();
        sendThread.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    sender.send();
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
                receiver.start();
            }
        });
        checkerThread.execute(new TimeoutChecker(this.membershipList, this.modeBuilder, this.id, this.socket));
        CommandHandler commandHandler = new CommandHandler(this);
        Scanner scanner = new Scanner(System.in);
        while(true) {
            commandHandler.handleCommand(scanner);
        }
    }

    @Override
    public Sender getSender() {
        return this.sender;
    }
}
