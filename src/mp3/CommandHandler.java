package mp3;

import mp2.constant.Command;
import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Mode;
import mp3.constant.ApplicationType;
import org.json.JSONArray;

import java.io.File;
import java.util.Scanner;

public class CommandHandler {
    private final String MAPLE = "maple";
    private final String JUICE = "juice";
    private final String MAPLE_JUICE = "mapleJuice";
    private final String space = " ";
    private final int MAPLE_COMMAND_LENGTH = 5;
    private final int JUICE_COMMAND_LENGTH = 6;
    private final int MAPLE_JUICE_COMMAND_LENGTH = 9;
    private final Sender sender;
    private final FailureDetector failureDetector;

    public CommandHandler(
        Sender sender,
        FailureDetector failureDetector
    ) {
        this.sender = sender;
        this.failureDetector = failureDetector;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            String[] info = command.split(space);
            if (info.length == 0) {
                System.out.println("Invalid command");
                continue;
            }
            switch (info[0]) {
                case MAPLE -> handleMaple(info);
                case JUICE -> handleJuice(info);
                case MAPLE_JUICE -> handleMapleJuice(info);
                default -> handleMembershipCommand(command);
            }
        }
    }

    private boolean isExeInvalid(String exeName) {
        return !(exeName.equals(ApplicationType.WORD_COUNT)
            || exeName.equals(ApplicationType.BUILDING)
            || exeName.equals(ApplicationType.VOTING_COUNT)
            || exeName.equals(ApplicationType.VOTING_COMPARE));
    }

    private boolean isInputFileNotExist(String fileName) {
        File file = new File(fileName);
        return !(file.exists());
    }

    private void handleMaple(String[] info) {
        if (info.length != MAPLE_COMMAND_LENGTH) {
            System.out.println("Invalid command");
        } else {
            String mapleExe = info[1];
            if (isExeInvalid(mapleExe)) {
                System.out.println("Invalid Maple Exe function");
                return;
            }
            int mapleNum = Integer.parseInt(info[2]);
            if (mapleNum <= 0) {
                System.out.println("Please enter positive number of execution servers");
                return;
            }
            String intermediatePrefix = info[3];
            String source = info[4];
            this.sender.sendMapleRequest(
                mapleExe,
                mapleNum,
                intermediatePrefix,
                source
            );
        }
    }

    private void handleJuice(String[] info) {
        if (info.length != JUICE_COMMAND_LENGTH) {
            System.out.println("Invalid command");
        } else {
            String juiceExe = info[1];
            if (isExeInvalid(juiceExe)) {
                System.out.println("Invalid Juice Exe function");
                return;
            }
            String intermediatePrefix = info[3];
            int juiceNum = Integer.parseInt(info[2]);
            if (juiceNum <= 0) {
                System.out.println("Please enter positive number of execution servers");
                return;
            }
            String destFile = info[4];
            int isDelete = Integer.parseInt(info[5]);
            this.sender.sendJuiceRequest(
                juiceExe,
                juiceNum,
                intermediatePrefix,
                destFile,
                isDelete
            );
        }
    }

    private void handleMapleJuice(String[] info) {
        if (info.length != MAPLE_JUICE_COMMAND_LENGTH) {
            System.out.println("Invalid command");
        } else {
            String mapleExe = info[1];
            if (isExeInvalid(mapleExe)) {
                System.out.println("Invalid Maple Exe function");
                return;
            }
            String juiceExe = info[5];
            if (isExeInvalid(juiceExe)) {
                System.out.println("Invalid Juice Exe function");
                return;
            }
            String source = info[4];
            int mapleNum = Integer.parseInt(info[2]);
            if (mapleNum < 0) {
                System.out.println("Please enter positive number of execution servers");
                return;
            }
            int juiceNum = Integer.parseInt(info[6]);
            if (juiceNum <= 0) {
                System.out.println("Please enter positive number of execution servers");
                return;
            }
            String intermediatePrefix = info[3];
            this.sender.sendMapleRequest(
                mapleExe,
                mapleNum,
                intermediatePrefix,
                source
            );
            String destFile = info[7];
            int isDelete = Integer.parseInt(info[8]);
            this.sender.sendJuiceRequest(
                juiceExe,
                juiceNum,
                intermediatePrefix,
                destFile,
                isDelete
            );
        }
    }

    private void handleMembershipCommand(String command) {
        switch (command) {
            case Command.SWITCH_MODE -> handleSwitchMode();
            case Command.PRINT_MEMBERSHIP -> handlePrintMembership();
            case Command.LEAVE -> handleLeave();
            case Command.REJOIN -> handleRejoin();
            default -> System.out.println("Invalid command");
        }
    }

    private void handleSwitchMode() {
        String newMode = this.failureDetector.getModeBuilder().toString().equals(Mode.GOSSIP)
            ? Mode.ALL_TO_ALL : Mode.GOSSIP;
        // sends switch mode message to all machines in the system
        this.failureDetector.getSender().switchMode(newMode);
    }

    private void handlePrintMembership() {
        JSONArray jsonArray = new JSONArray(failureDetector.getMembershipList());
        System.out.println(jsonArray);
    }

    private void handleLeave() {
        if (this.failureDetector instanceof mp2.failureDetector.Server) {
            this.failureDetector.leave();
        }
    }

    private void handleRejoin() {
        if (this.failureDetector instanceof mp2.failureDetector.Server) {
            ((mp2.failureDetector.Server) this.failureDetector).rejoin();
        }
    }
}
