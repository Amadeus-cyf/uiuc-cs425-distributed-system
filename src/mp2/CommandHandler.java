package mp2;

import mp2.constant.Command;
import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Mode;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.logging.Logger;

import static mp2.constant.FilePath.ROOT;

public class CommandHandler {
    private final Logger logger = Logger.getLogger(CommandHandler.class.getName());
    private final FailureDetector failureDetector;
    private final Sender sender;
    private final Scanner scanner;
    private final Receiver receiver;

    public CommandHandler(
        Sender sender,
        FailureDetector failureDetector,
        Scanner scanner,
        Receiver receiver
    ) {
        this.sender = sender;
        this.failureDetector = failureDetector;
        this.scanner = scanner;
        this.receiver = receiver;
    }

    public void handleCommand() {
        String command = this.scanner.nextLine();
        switch (command) {
            case Command.SWITCH_MODE -> handleSwitchMode();
            case Command.PRINT_MEMBERSHIP -> handlePrintMembership();
            case Command.LEAVE -> handleLeave();
            case Command.REJOIN -> handleRejoin();
            case Command.SERVERS -> handleListServers();
            default -> handleFileSystemCommands(command);
        }
    }

    private void handleSwitchMode() {
        String newMode = this.failureDetector.getModeBuilder().toString().equals(Mode.GOSSIP) ?
            Mode.ALL_TO_ALL : Mode.GOSSIP;
        // sends switch mode message to all machines in the system
        this.failureDetector.getSender().switchMode(newMode);
    }

    private void handlePrintMembership() {
        JSONArray jsonArray = new JSONArray(this.failureDetector.getMembershipList());
        printMembershipList(jsonArray);
    }

    private void handleLeave() {
        if (this.failureDetector instanceof mp2.failureDetector.Server) {
            this.failureDetector.leave();
            logger.info("Server has left the system");
        }
    }

    private void handleRejoin() {
        if (this.failureDetector instanceof mp2.failureDetector.Server) {
            ((mp2.failureDetector.Server) this.failureDetector).rejoin();
        }
    }

    private void handleListServers() {
        if (this.receiver instanceof MasterReceiver) {
            logger.info("Current alive servers: " + ((MasterReceiver) this.receiver).servers);
        }
    }

    private void handleFileSystemCommands(String command) {
        String[] commandList = command.split(" ");
        if (commandList.length == 0) {
            logger.warning("Invalid command");
            return;
        }
        switch (commandList[0]) {
            case Command.GET -> handleGet(commandList);
            case Command.PUT -> handlePut(commandList);
            case Command.DELETE -> handleDelete(commandList);
            case Command.LS -> handleLS(commandList);
            case Command.STORAGE -> handleStorage(commandList);
            case Command.DIFF -> handleDiff(commandList);
            case Command.MORE -> handleMore(commandList);
            case Command.CLEAR -> handleClear(commandList);
            case Command.COMPARE -> handleCompare(commandList);
            default -> logger.warning("Invalid command");
        }
    }

    private void handleGet(String[] command) {
        if (command.length != 3) {
            logger.warning("Incorrect input type for get request");
        } else {
            this.sender.sendPreGetRequest(
                command[1],
                command[2]
            );
        }
    }

    private void handlePut(String[] command) {
        if (command.length != 3) {
            logger.warning("Incorrect input type for put request");
        } else {
            this.sender.sendPrePutRequest(
                command[1],
                command[2]
            );
        }
    }

    private void handleDelete(String[] command) {
        if (command.length != 2) {
            logger.warning("Incorrect input type for delete request");
        } else {
            this.sender.sendPreDelRequest(command[1]);
        }
    }

    private void handleLS(String[] command) {
        if (command.length != 2) {
            logger.warning("Incorrect input type for ls");
        } else {
            this.sender.sendLsRequest(command[1]);
        }
    }

    private void handleStorage(String[] command) {
        if (command.length != 1) {
            logger.warning("Incorrect input type for storage");
        } else {
            this.sender.sendStoreRequest();
        }
    }

    private void handleDiff(String[] command) {
        if (command.length != 3) {
            logger.warning("Incorrect input type for diff");
        } else {
            String filePath1 = ROOT + command[1];
            String filePath2 = ROOT + command[2];
            try {
                String s = null;
                Process p = Runtime.getRuntime().exec("diff " + filePath1 + " " + filePath2);
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null)
                    System.out.println(s);
                p.waitFor();
                p.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMore(String[] command) {
        if (command.length != 2) {
            logger.warning("Incorrect input type for more");
        } else {
            String filePath = command[1];
            System.out.println(filePath);
            try {
                String s = null;
                Process p = Runtime.getRuntime().exec("cat " + ROOT + filePath);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                }
                p.waitFor();
                p.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClear(String[] command) {
        if (command.length != 1) {
            logger.warning("Incorrect input type for clear");
        } else {
            for (int i = 0; i < 50; ++i) System.out.println();
        }
    }

    private void handleCompare(String[] command) {
        if (command.length != 3) {
            logger.warning("Incorrect input type for compare");
        } else {
            String filePath1 = ROOT + command[1];
            String filePath2 = ROOT + command[2];
            File file1 = new File(filePath1);
            File file2 = new File(filePath2);
            System.out.println(filePath1 + " has a size of " + file1.length());
            System.out.println(filePath2 + " has a size of " + file2.length());
            if (file1.length() == file2.length()) {
                System.out.println("Two files have a same size");
            } else {
                System.out.println("Two files have different sizes");
            }
        }
    }

    private void printMembershipList(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            Object member = jsonArray.get(i);
            System.out.println(member.toString());
        }
    }
}
