package mp1;

public class Mode {
    public static final String ALL_TO_ALL = "ALL_TO_ALL";
    public static final String GOSSIP = "GOSSIP";
    public static final String JOIN = "JOIN";                       // send message to introducer requesting join the system
    public static final String AGREE_JOIN = "AGREE_JOIN";           // introducer agree the joining request and send its membership
}
