package mp1.model;

import mp1.Mode;

import java.util.List;

public class GossipHeartBeat extends HeartBeat {
    List<Member> membershipList;

    public GossipHeartBeat(Mode mode, List<Member> membershipList) {
        super(mode);
        this.membershipList = membershipList;
    }
}
