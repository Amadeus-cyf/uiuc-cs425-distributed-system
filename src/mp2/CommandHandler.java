package mp2;

import mp2.constant.Command;
import mp2.failureDetector.FailureDetector;
import mp2.failureDetector.Mode;
import mp2.failureDetector.Server;
import org.json.JSONArray;

import java.io.*;
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
                if (commandList.length != 2) {
                    logger.warning("Incorrect input type for more");
                } else {
                    String filePath = commandList[1];
                    System.out.println(filePath);
                    try {
                        String s = null;
                        System.out.println("more " + ROOT + filePath);
                        Process p = Runtime.getRuntime().exec("cat " + ROOT + filePath);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                        bw.write("yes");
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
            } else if (commandList[0].equals(Command.CLEAR)) {
                if (commandList.length != 1) {
                    logger.warning("Incorrect input type for clear");
                } else {
                    for (int i = 0; i < 50; ++i) System.out.println();
                }
            } else if(commandList[0].equals(Command.COMPARE)) {
                if(commandList.length != 3) {
                    logger.warning("Incorrect input type for compare");
                } else {
                    String filePath1 = ROOT + commandList[1];
                    String filePath2 = ROOT + commandList[2];
                    File file1 = new File(filePath1);
                    File file2 = new File(filePath2);
                    System.out.println(filePath1 + " has a size of " + file1.length());
                    System.out.println(filePath2 + " has a size of " + file2.length());
                    if(file1.length() == file2.length()) {
                        System.out.println("Two files have a same size");
                    } else {
                        System.out.println("Two files have different sizes");
                    }
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
