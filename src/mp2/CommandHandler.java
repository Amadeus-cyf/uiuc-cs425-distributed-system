package mp2;

import mp2.constant.Command;
import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Mode;
import mp2.failureDetector.Server;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.logging.Logger;

import static mp2.constant.FilePath.ROOT;

public class CommandHandler {
    private Logger logger = Logger.getLogger(CommandHandler.class.getName());
    private FailureDetector failureDetector;
    private Sender sender;
    private Scanner scanner;
    private Receiver receiver;

    public CommandHandler(Sender sender, FailureDetector failureDetector, Scanner scanner, Receiver receiver) {
        this.sender = sender;
        this.failureDetector = failureDetector;
        this.scanner = scanner;
        this.receiver = receiver;
    }

    public void handleCommand() {
        String command = this.scanner.nextLine();
        if(command.equals(Command.SWITCH_MODE)) {
            String newMode = this.failureDetector.getModeBuilder().toString().equals(Mode.GOSSIP) ? Mode.ALL_TO_ALL : Mode.GOSSIP;
            // sends switch mode message to all machines in the system
            this.failureDetector.getSender().switchMode(newMode);
        } else if(command.equals(Command.PRINT_MEMBERSHIP)) {
            JSONArray jsonArray = new JSONArray(this.failureDetector.getMembershipList());
            printMembershipList(jsonArray);
        } else if (command.equals(Command.LEAVE)) {
            if(this.failureDetector instanceof mp2.failureDetector.Server) {
                this.failureDetector.leave();
                logger.info("Server has left the system");
            }
        } else if (command.equals(Command.REJOIN)) {
            if(this.failureDetector instanceof mp2.failureDetector.Server) {
                ((Server) this.failureDetector).rejoin();
            }
        } else if(command.equals(Command.SERVERS)) {
            if(this.receiver instanceof MasterReceiver) {
                logger.info("Current alive servers: " + ((MasterReceiver) this.receiver).servers);
            }
        } else {
            String[] commandList = command.split(" ");
            if(commandList[0].equals(Command.GET)) {
                if(commandList.length != 3) {
                    logger.warning("Incorrect input type for get request");
                } else {
                    this.sender.sendPreGetRequest(commandList[1], commandList[2]);
                }
            } else if(commandList[0].equals(Command.PUT)) {
                if(commandList.length != 3) {
                    logger.warning("Incorrect input type for put request");
                } else {
                    this.sender.sendPrePutRequest(commandList[1], commandList[2]);
                }
            } else if(commandList[0].equals(Command.DELETE)) {
                if(commandList.length != 2) {
                    logger.warning("Incorrect input type for delete request");
                } else {
                    this.sender.sendPreDelRequest(commandList[1]);
                }
            } else if(commandList[0].equals(Command.LS)) {
                if(commandList.length != 2) {
                    logger.warning("Incorrect input type for ls");
                } else {
                    this.sender.sendLsRequest(commandList[1]);
                }
            } else if (commandList[0].equals(Command.STORAGE)) {
                if (commandList.length != 1) {
                    logger.warning("Incorrect input type for storage");
                } else {
                    this.sender.sendStoreRequest();
                }
            } else if(commandList[0].equals(Command.DIFF)) {
                if (commandList.length != 3) {
                    logger.warning("Incorrect input type for diff");
                } else {
                    String filePath1 = ROOT + commandList[1];
                    String filePath2 = ROOT + commandList[2];
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
            } else if (commandList[0].equals(Command.MORE)) {
                String filePath = commandList[0];
                try {
                    String s = null;
                    Process p = Runtime.getRuntime().exec("more " + ROOT + filePath);
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((s = br.readLine()) != null) {
                        System.out.println(s);
                    }
                    p.waitFor();
                    p.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (commandList[0].equals(Command.CLEAR)) {
                try {
                    Process p = Runtime.getRuntime().exec("clear");
                    p.waitFor();
                    p.exitValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                logger.warning("Command not found");
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
