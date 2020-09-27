package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends BaseServer {
    public Sender sender;
    public Receiver receiver;
    private Long heartbeatCounter = 0L;
    private TimeoutChecker checker;

    public Server(String ipAddress, int port) {
        super(ipAddress, port);
    }

    public static void main(String[] args) {
        Server server = new Server(args[0], 3000);
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
        server.checker = new TimeoutChecker(server.membershipList, server.modeBuilder, server.id);
        checkerThread.execute(server.checker);
        CommandHandler commandHandler = new CommandHandler(server);
        Scanner scanner = new Scanner(System.in);
        while(true) {
            commandHandler.handleCommand(scanner);
        }
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
    }

    @Override
    public Sender getSender() {
        return this.sender;
    }
}
