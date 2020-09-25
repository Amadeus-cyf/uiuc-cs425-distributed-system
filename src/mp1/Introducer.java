package mp1;

public class Introducer extends BaseServer {
    public static final String IP_ADDRESS = "localhost";
    public static final int PORT = 3000;
    private Sender sender;
    private Receiver receiver;

    public Introducer() {
        super(IP_ADDRESS, PORT);
    }
}
