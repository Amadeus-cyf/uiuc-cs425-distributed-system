package mp3;

import mp2.constant.Command;
import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Mode;
import org.json.JSONArray;

import java.util.Scanner;

public class CommandHandler {
    private Sender sender;
    private FailureDetector failureDetector;
    private final String MAPLE = "maple";
    private final String JUICE = "juice";
    private final String MAPLE_JUICE = "mapleJuice";
    private final int MAPLE_COMMAND_LENGTH = 5;
    private final int JUICE_COMMAND_LENGTH = 6;
    private final int MAPLE_JUICE_COMMAND_LENGTH = 9;

    public CommandHandler(Sender sender, FailureDetector failureDetector) {
        this.sender = sender;
        this.failureDetector = failureDetector;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            String[] info = command.split(" ");
            if (info.length == 0) {
                System.out.println("Invalid command");
            } else if (info[0].equals(MAPLE)) {
                if (info.length != MAPLE_COMMAND_LENGTH) {
                    System.out.println("Invalid command");
                } else {
                    String mapleExe = info[1];
                    int mapleNum = Integer.parseInt(info[2]);
                    String intermediatePrefix = info[3];
                    String source = info[4];
                    this.sender.sendMapleRequest(mapleExe, mapleNum, intermediatePrefix, source);
                }
            } else if (info[0].equals(JUICE)) {
                if (info.length != JUICE_COMMAND_LENGTH) {
                    System.out.println("Invalid command");
                } else {
                    String juiceExe = info[1];
                    int juiceNum = Integer.parseInt(info[2]);
                    String intermediatePrefix = info[3];
                    String destFile = info[4];
                    int isDelete = Integer.parseInt(info[5]);
                    this.sender.sendJuiceRequest(juiceExe, juiceNum, intermediatePrefix, destFile, isDelete);
                }
            } else if (info[0].equals(MAPLE_JUICE)) {
                if (info.length != MAPLE_JUICE_COMMAND_LENGTH) {
                    System.out.println("Invalid command");
                } else {
                    String mapleExe = info[1];
                    int mapleNum = Integer.parseInt(info[2]);
                    String intermediatePrefix = info[3];
                    String source = info[4];
                    this.sender.sendMapleRequest(mapleExe, mapleNum, intermediatePrefix, source);
                    String juiceExe = info[5];
                    int juiceNum = Integer.parseInt(info[6]);
                    String destFile = info[7];
                    int isDelete = Integer.parseInt(info[8]);
                    this.sender.sendJuiceRequest(juiceExe, juiceNum, intermediatePrefix, destFile, isDelete);
                }
            } else {
                if (command.equals(Command.SWITCH_MODE)) {
                    String newMode = this.failureDetector.getModeBuilder().toString().equals(Mode.GOSSIP) ? Mode.ALL_TO_ALL : Mode.GOSSIP;
                    // sends switch mode message to all machines in the system
                    this.failureDetector.getSender().switchMode(newMode);
                } else if (command.equals(Command.PRINT_MEMBERSHIP)) {
                    JSONArray jsonArray = new JSONArray(failureDetector.getMembershipList());
                    System.out.println(jsonArray.toString());
                } else if (command.equals(Command.LEAVE)) {
                    if (this.failureDetector instanceof mp2.failureDetector.Server) {
                        this.failureDetector.leave();
                    }
                } else if (command.equals(Command.REJOIN)) {
                    if (this.failureDetector instanceof mp2.failureDetector.Server) {
                        ((mp2.failureDetector.Server) this.failureDetector).rejoin();
                    }
                } else {
                    System.out.println("Invalid command");
                }
            }
        }
    }
}
