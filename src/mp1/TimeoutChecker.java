package mp1;

import mp1.model.Member;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static mp1.Introducer.IP_ADDRESS;
import static mp1.Introducer.PORT;

public class TimeoutChecker implements Runnable {
    private final List<Member> membershipList;
    private final long ALLTOALL_FAIL_TIME_LIMIT = 3000;
    private final long GOSSIP_FAIL_TIME_LIMIT = 3000;
    private StringBuilder modeBuilder;
    private volatile String mode;
    private String id;
    static Logger logger = Logger.getLogger(TimeoutChecker.class.getName());

    public TimeoutChecker(List<Member> membershipList, StringBuilder modeBuilder, String id) {
        this.membershipList = membershipList;
        this.modeBuilder = modeBuilder;
        this.mode = modeBuilder.toString();
        this.id = id;
    }

    /*
     * run the timeoutchecker based on different modes
     */
    public void run() {
        while(true) {
            switch (this.modeBuilder.toString()) {
                case(Mode.ALL_TO_ALL):
                    logger.warning("TIME CHECKER ALL TO ALL");
                    allToAllTimeoutChecker();
                    break;
                case(Mode.GOSSIP):
                    logger.warning("TIME CHECKER GOSSIP");
                    gossipTimeoutChecker();
                    break;
                default:
                    break;
            }
        }
    }

    private void allToAllTimeoutChecker() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        for (Member member : membershipList) {
            if(isIntroducer(member.getId()) || member.getId().equals(id)) {
                continue;
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            //logger.warning("ALL2ALL-CHECKER  " + member.getId() + "   " + timestamp.getTime() + "   "  + member.getTimestamp().getTime());
            if ((timestamp.getTime() - member.getTimestamp().getTime()) >= ALLTOALL_FAIL_TIME_LIMIT)  {
                member.setStatus(Status.FAIL);
                //logger.warning("ALL2ALL-FAIL: SERVER - " + member.getId());
            } else if (member.getStatus().equals(Status.FAIL)) {
                member.setStatus(Status.WORKING);
            }
        }
    }

    private void gossipTimeoutChecker() {
        try{
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        List<Member> delList = new ArrayList<>();
        for (Member member : membershipList) {
            if(isIntroducer(member.getId()) || member.getId().equals(id)) {
                continue;
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            //logger.warning("GOSSIP-CHECKER  " + member.getId() + "   " + timestamp.getTime() + "   "  + member.getTimestamp().getTime());
            if((timestamp.getTime() - member.getTimestamp().getTime()) >= 2*GOSSIP_FAIL_TIME_LIMIT){
                logger.warning("GOSSIP-CLEANOUT: SERVER - " + member.getId());
                // go through the membershiplist, delete the member
                delList.add(member);
            } else if((timestamp.getTime() - member.getTimestamp().getTime()) >= GOSSIP_FAIL_TIME_LIMIT && member.getStatus().equals(Status.WORKING)){
                member.setStatus(Status.FAIL);
                logger.warning("GOSSIP-FAIL: SERVER - " + member.getId());
            }
        }
        for (Member member : delList) {
            membershipList.remove(member);
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
