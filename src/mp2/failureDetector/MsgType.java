package mp2.failureDetector;

public class MsgType {
    public static final String ALL_TO_ALL_MSG = "ALL_TO_ALL_MSG";
    public static final String GOSSIP_MSG = "GOSSIP_MSG";
    // send message to introducer requesting join the system
    public static final String JOIN_MSG = "JOIN_MSG";
    // introducer agree the joining request and send its membership
    public static final String AGREE_JOIN = "AGREE_JOIN_MSG";
    public static final String SWITCH_MODE = "SWITCH_MODE";
}
