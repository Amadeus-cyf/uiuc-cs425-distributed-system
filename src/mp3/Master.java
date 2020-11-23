package mp3;

import mp3.constant.MasterInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master extends Server{
    public Master() {
        super(MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }

    public void run() {
        Sender sender = new Sender(this.ipAddress, this.port, this.dataTransfer);
        Receiver receiver = new MasterReceiver(this.dataTransfer);
        CommandHandler commandHandler = new CommandHandler(sender);
        ExecutorService thread = Executors.newFixedThreadPool(1);
        thread.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        commandHandler.run();
    }

    public void main(String[] args) {
        Master master = new Master();
        master.run();
    }
}
