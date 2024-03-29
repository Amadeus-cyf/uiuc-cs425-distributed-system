package mp2.failureDetector;

import mp2.failureDetector.model.Member;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Introducer extends FailureDetector {
    private final String mode = Mode.GOSSIP;
    private final Sender sender;
    private final Receiver receiver;

    public Introducer(
        String ipAddress,
        int port
    ) {
        super(
            ipAddress,
            port
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

    @Override
    public void run() {
        ExecutorService sendThread = Executors.newSingleThreadExecutor();
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
                        e.printStackTrace();
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
        checkerThread.execute(new TimeoutChecker(
            this.membershipList,
            this.modeBuilder,
            this.id,
            this.socket
        ));
    }

    @Override
    public Sender getSender() {
        return this.sender;
    }
}
