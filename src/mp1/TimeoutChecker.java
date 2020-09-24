package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.List;

public class TimeoutChecker implements Runnable {
    private final List<Member> membershipList;
    private final long MAX_TIME_LIMIT = 3000;
    private volatile String mode;

    public TimeoutChecker(List<Member> membershipList, String mode) {
        this.membershipList = membershipList;
        this.mode = mode;
    }

    /*
     * iterate over
     */
    public void run() {
        for (Member member : membershipList) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            if (timestamp.getTime() - member.getTimestamp().getTime() > MAX_TIME_LIMIT)  {
                synchronized (member) {
                    member.setStatue(Status.FAIL);
                }
            }
        }
    }
}
