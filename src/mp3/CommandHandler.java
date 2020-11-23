package mp3;

import java.util.Scanner;

public class CommandHandler {
    private Sender sender;
    private final String MAPLE = "maple";
    private final String JUICE = "juice";
    private final int MAPLE_COMMAND_LENGTH = 5;

    public CommandHandler(Sender sender) {
        this.sender = sender;
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
            }
        }
    }
}
