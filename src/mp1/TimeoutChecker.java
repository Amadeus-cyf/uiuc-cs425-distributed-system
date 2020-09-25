package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class TimeoutChecker implements Runnable {
    private final List<Member> membershipList;
    private final long MAX_TIME_LIMIT = 5000;
    private volatile String mode;
    static Logger logger = Logger.getLogger(TimeoutChecker.class.getName());

    public TimeoutChecker(List<Member> membershipList, String mode) {
        this.membershipList = membershipList;
        this.mode = mode;
    }

    /*
     * iterate over
     */
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
            for (Member member : membershipList) {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                logger.warning("CHECKER  " + member.getId() + "   " + timestamp.getTime() + "   "  + member.getTimestamp().getTime());
                if ((timestamp.getTime() - member.getTimestamp().getTime()) > MAX_TIME_LIMIT)  {
                    member.setStatue(Status.FAIL);
                    logger.warning("TIMEOUT: SERVER - " + member.getId());
                } else if (member.getStatus().equals(Status.FAIL)) {
                    member.setStatue(Status.WORKING);
                }
            }
        }
    }
}
