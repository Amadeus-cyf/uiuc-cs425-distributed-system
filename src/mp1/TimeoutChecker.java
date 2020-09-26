package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import static mp1.Introducer.IP_ADDRESS;
import static mp1.Introducer.PORT;

public class TimeoutChecker implements Runnable {
    private final List<Member> membershipList;
    private final long ALLTOALL_FAIL_TIME_LIMIT = 3000;
    private final long GOSSIP_SUSPECT_TIME_LIMIT = 5000;
    private final long GOSSIP_FAIL_TIME_LIMIT = GOSSIP_SUSPECT_TIME_LIMIT;
    private volatile String mode;
    private String id;
    static Logger logger = Logger.getLogger(TimeoutChecker.class.getName());

    public TimeoutChecker(List<Member> membershipList, String mode, String id) {
        this.membershipList = membershipList;
        this.mode = mode;
        this.id = id;
    }

    /*
     * run the timeoutchecker based on different modes
     */
    public void run() {
        switch (mode) {
            case(Mode.ALL_TO_ALL):
                allToAllTimeoutChecker();
                break;
            case(Mode.GOSSIP):
                gossipTimeoutChecker();
                break;
            default:
                break;
        }

    }

    private void allToAllTimeoutChecker() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
            for (Member member : membershipList) {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                logger.warning("ALL2ALL-CHECKER  " + member.getId() + "   " + timestamp.getTime() + "   "  + member.getTimestamp().getTime());
                if ((timestamp.getTime() - member.getTimestamp().getTime()) >= ALLTOALL_FAIL_TIME_LIMIT)  {
                    member.setStatus(Status.FAIL);
                    logger.warning("ALL2ALL-TIMEOUT: SERVER - " + member.getId());
                } else if (member.getStatus().equals(Status.FAIL)) {
                    member.setStatus(Status.WORKING);
                }
            }
        }
    }

    private void gossipTimeoutChecker() {
        while (true) {
            try{

                Thread.sleep(3000);

            } catch (InterruptedException e) {
            }
            for (Member member : membershipList) {
                if(isIntroducer(member.getId())){
                    continue;
                }
                if(member.getId().equals(id)) {
                    continue;
                }
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                logger.warning("GOSSIPCHECKER  " + member.getId() + "   " + timestamp.getTime() + "   "  + member.getTimestamp().getTime());
                if((timestamp.getTime() - member.getTimestamp().getTime()) >= GOSSIP_FAIL_TIME_LIMIT && member.getStatus().equals(Status.SUSPECT)){
                    member.setStatus(Status.FAIL);
                    logger.warning("GOSSIP-FAIL: SERVER - " + member.getId());
                } else if((timestamp.getTime() - member.getTimestamp().getTime()) >= GOSSIP_SUSPECT_TIME_LIMIT && member.getStatus().equals(Status.WORKING)){
                    member.setStatus(Status.SUSPECT);
                    logger.warning("GOSSIP-SUSPECT: SERVER - " + member.getId());
                }
            }
        }
    }

    private boolean isIntroducer(String id) {
        String[] idInfo = id.split("_");
        String ipAddress = idInfo[0];
        int port = Integer.parseInt(idInfo[1]);
        if(ipAddress.equals(IP_ADDRESS) && port == PORT){
            return true;
        }
        return false;
    }

}
