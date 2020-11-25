package mp3.application;

import mp3.constant.ApplicationType;

public class MapleJuiceFactory {
    public static MapleJuice<?, ?> create(String exeName) {
        switch(exeName) {
            case (ApplicationType.WORD_COUNT):
                return new WordCount();
            default:
                return null;
        }
    }
}
