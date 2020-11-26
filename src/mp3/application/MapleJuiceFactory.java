package mp3.application;

import mp3.constant.ApplicationType;

public class MapleJuiceFactory {
    public static MapleJuice<?, ?> create(String exeName) {
        switch(exeName) {
            case (ApplicationType.WORD_COUNT):
                return new WordCount();
            case (ApplicationType.BUILDING):
                return new Building();
            case (ApplicationType.VOTING_COUNT):
                return new VotingCount();
            case (ApplicationType.VOTING_COMPARE):
                return new VotingCompare();
            default:
                return null;
        }
    }
}
