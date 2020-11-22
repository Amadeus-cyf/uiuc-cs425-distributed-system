package mp2.failureDetector;

import mp2.failureDetector.model.Member;
import mp2.message.FPRejoinMessage;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static mp2.constant.MasterSdfsInfo.MASTER_SDFS_IP_ADDRESS;
import static mp2.constant.MasterSdfsInfo.MASTER_SDFS_PORT;

public class Server extends FailureDetector {
    public Sender sender;
    public Receiver receiver;
    private Long heartbeatCounter = 0L;
    private TimeoutChecker checker;

    public Server(String ipAddress, int port) {
        super(ipAddress, port);
    }

    @Override
    public void run() {
        join();
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
        this.checker = new TimeoutChecker(membershipList, modeBuilder, id, this.socket);
        checkerThread.execute(this.checker);
    }

    public void join() {
        this.status = Status.RUNNING;
        this.startingTime = new Timestamp(System.currentTimeMillis());
        this.id = createId();
        this.sender = new Sender(this.id, this.ipAddress, this.port, this.membershipList, this.modeBuilder, this.statusBuilder, this.socket, this.heartbeatCounter);
        this.receiver = new Receiver(this.id, this.ipAddress, this.port, this.membershipList, this.modeBuilder, this.statusBuilder, this.socket, this.heartbeatCounter);
        Member member = new Member(this.id, this.startingTime,this.heartbeatCounter);
        this.membershipList.add(member);
        // sender send a message to the ip address and port of the introducer
        this.sender.sendJoinRequest();
    }

    public void rejoin() {
        this.statusBuilder.setLength(0);
        this.statusBuilder.append(Status.RUNNING);
        this.join();
        this.checker.resetId(this.id);
        FPRejoinMessage fpRejoinMessage = new FPRejoinMessage(this.ipAddress, this.port);
        this.socket.send(fpRejoinMessage.toJSON(), MASTER_SDFS_IP_ADDRESS, MASTER_SDFS_PORT);
        this.socket.send(fpRejoinMessage.toJSON(), this.ipAddress, this.port-1);
    }

    @Override
    public Sender getSender() {
        return this.sender;
    }
}
