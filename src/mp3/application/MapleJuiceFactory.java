package mp3.application;

import mp3.constant.ApplicationType;

public class MapleJuiceFactory {
    public static MapleJuice<?, ?> create(String exeName) {
        return switch (exeName) {
            case (ApplicationType.WORD_COUNT) -> new WordCount();
            case (ApplicationType.BUILDING) -> new Building();
            case (ApplicationType.VOTING_COUNT) -> new VotingCount();
            case (ApplicationType.VOTING_COMPARE) -> new VotingCompare();
            default -> null;
        };
    }
}
