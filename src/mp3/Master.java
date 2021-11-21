package mp3;

import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Introducer;
import mp3.constant.MasterInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master extends Server{
    public Master() {
        super(MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }

    public void run() {
        FailureDetector failureDetector = new Introducer(MasterInfo.MASTER_FD_IP_ADDRESS, MasterInfo.MASTER_FD_PORT);
        Sender sender = new Sender(this.ipAddress, this.port, this.dataTransfer);
        Receiver receiver = new MasterReceiver(this.dataTransfer);
        failureDetector.run();
        CommandHandler commandHandler = new CommandHandler(sender, failureDetector);
        receiver.start();
        commandHandler.run();
    }

    public static void main(String[] args) {
        Master master = new Master();
        master.run();
    }
}
