package mp2.failureDetector;

import mp2.DataTransfer;
import mp2.constant.MasterFdInfo;
import mp2.constant.MasterSdfsInfo;
import mp2.failureDetector.model.Member;
import mp2.message.FailMessage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TimeoutChecker implements Runnable {
    private static final Logger logger = Logger.getLogger(TimeoutChecker.class.getName());
    private final List<Member> membershipList;
    private final long ALLTOALL_FAIL_TIME_LIMIT = 3000;
    private final long GOSSIP_FAIL_TIME_LIMIT = 5000;
    private final StringBuilder modeBuilder;
    private final String mode;
    private final DataTransfer socket;
    private String id;

    public TimeoutChecker(
        List<Member> membershipList,
        StringBuilder modeBuilder,
        String id,
        DataTransfer socket
    ) {
        this.membershipList = membershipList;
        this.modeBuilder = modeBuilder;
        this.mode = modeBuilder.toString();
        this.id = id;
        this.socket = socket;
    }

    public void resetId(String id) {
        this.id = id;
    }

    /*
     * run the timeoutchecker based on different modes
     */
    public void run() {
        while (true) {
            switch (this.modeBuilder.toString()) {
                case (Mode.ALL_TO_ALL) ->
                    //logger.warning("TIME CHECKER ALL TO ALL");
                    allToAllTimeoutChecker();
                case (Mode.GOSSIP) ->
                    //logger.warning("TIME CHECKER GOSSIP");
                    gossipTimeoutChecker();
                default -> {
                }
            }
        }
    }

    private void allToAllTimeoutChecker() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        List<Member> delList = new ArrayList<>();
        for (Member member : membershipList) {
            if (isIntroducer(member.getId()) || member.getId().equals(this.id)) {
                continue;
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            if ((timestamp.getTime() - member.getTimestamp().getTime()) >= ALLTOALL_FAIL_TIME_LIMIT) {
                member.setStatus(Status.FAIL);
                delList.add(member);
                logger.warning("ALL_TO_ALL LEAVE/FAIL " + member.getId());
                sendFailMessage(member);
            }
        }
        for (Member member : delList) {
            membershipList.remove(member);
        }
    }

    private void gossipTimeoutChecker() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        List<Member> delList = new ArrayList<>();
        for (Member member : membershipList) {
            if (isIntroducer(member.getId()) || member.getId().equals(id)) {
                continue;
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            if ((timestamp.getTime() - member.getTimestamp().getTime()) >= 2 * GOSSIP_FAIL_TIME_LIMIT) {
                System.out.println("GOSSIP CLEANOUT " + member.getId());
                // go through the membershiplist, delete the member
                delList.add(member);
            } else if ((timestamp.getTime() - member.getTimestamp().getTime()) >= GOSSIP_FAIL_TIME_LIMIT
                && (!member.getStatus().equals(Status.FAIL))) {
                member.setStatus(Status.FAIL);
                System.out.println("GOSSIP LEAVE/FAIL  " + member.getId());
                sendFailMessage(member);
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
        return ipAddress.equals(MasterFdInfo.MASTER_FD_IP_ADDRESS) && port == MasterFdInfo.MASTER_FD_PORT;
    }

    private void sendFailMessage(Member member) {
        String[] idInfo = member.getId().split("_");
        String failIpAddress = idInfo[0];
        int failPort = Integer.parseInt(idInfo[1]);
        FailMessage failMessage = new FailMessage(
            failIpAddress,
            failPort
        );
        this.socket.send(
            failMessage.toJSON(),
            MasterSdfsInfo.MASTER_SDFS_IP_ADDRESS,
            MasterSdfsInfo.MASTER_SDFS_PORT
        );
        System.out.println("Server " + failIpAddress + ":" + failPort + " fails. Send Fail Message to Server Master");
    }
}
