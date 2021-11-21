package mp2;

import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Introducer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static mp2.constant.MasterFdInfo.MASTER_FD_IP_ADDRESS;
import static mp2.constant.MasterFdInfo.MASTER_FD_PORT;
import static mp2.constant.MasterSdfsInfo.*;

public class Master {
    private String ipAddress;
    private int port;

    public Master() {
        this.ipAddress = MASTER_SDFS_IP_ADDRESS;
        this.port = MASTER_SDFS_PORT;
    }

    public void run() {
        DataTransfer dataTransfer = new DataTransfer(this.ipAddress, this.port);
        FailureDetector failureDetector = new Introducer(MASTER_FD_IP_ADDRESS, MASTER_FD_PORT);
        Receiver receiver = new MasterReceiver(this.ipAddress, this.port, dataTransfer);
        Sender sender = new Sender(this.ipAddress, this.port, dataTransfer);
        receiver.start();
        Scanner scanner = new Scanner(System.in);
        CommandHandler commandHandler = new CommandHandler(sender, failureDetector, scanner, receiver);
        failureDetector.run();
        System.out.println(this.ipAddress + ":" + this.port);
        while(true) {
            commandHandler.handleCommand();
        }
    }

    public static void main(String[] args) {
        Master server = new Master();
        server.run();
    }
}
