package mp1;

import org.json.JSONArray;

import java.util.Scanner;
import java.util.logging.Logger;

public class CommandHandler {
    private static Logger logger = Logger.getLogger(CommandHandler.class.getName());
    private BaseServer server;

    public CommandHandler(BaseServer server) {
        this.server = server;
    }

    public void handleCommand() {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String command = scanner.nextLine();
            if (command.equals("C")) {
                String newMode = this.server.getModeBuilder().toString().equals(Mode.GOSSIP) ? Mode.ALL_TO_ALL : Mode.GOSSIP;
                logger.warning(newMode);
                // sends switch mode message to all machines in the system
                this.server.getSender().switchMode(newMode);
            } else if (command.equals("PM")) {
                JSONArray jsonArray = new JSONArray(server.membershipList);
                System.out.println(jsonArray.toString());
            } else if (command.equals("EXIT")) {
                this.server.disconnect();
            }
        }
    }
}
