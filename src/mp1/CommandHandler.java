package mp1;

import org.json.JSONArray;

import java.util.Scanner;
import java.util.logging.Logger;

public class CommandHandler {
    private static final Logger logger = Logger.getLogger(CommandHandler.class.getName());
    private final BaseServer server;

    public CommandHandler(BaseServer server) {
        this.server = server;
    }

    public void handleCommand(Scanner scanner) {
        String command = scanner.nextLine();
        switch (command) {
            case Command.SWITCH_MODE:
                String newMode = this.server.getModeBuilder().toString().equals(Mode.GOSSIP)
                    ? Mode.ALL_TO_ALL : Mode.GOSSIP;
                logger.warning(newMode);
                // sends switch mode message to all machines in the system
                this.server.getSender().switchMode(newMode);
                break;
            case Command.PRINT_MEMBERSHIP:
                JSONArray jsonArray = new JSONArray(server.membershipList);
                System.out.println(jsonArray);
                break;
            case Command.LEAVE:
                if (this.server instanceof Server) {
                    this.server.leave();
                }
                break;
            case Command.REJOIN:
                if (this.server instanceof Server) {
                    ((Server) this.server).rejoin();
                }
                break;
            default:
                logger.warning("invalid command");
                break;
        }
    }
}
