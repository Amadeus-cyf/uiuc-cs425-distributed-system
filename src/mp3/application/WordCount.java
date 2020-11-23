package mp3.application;

public class WordCount extends MapleJuice<String, Integer> {
    @Override
    public void maple(String line) {
        String[] infoList = line.split(" ");
        for (String info : infoList) {
            mapleOutput.add(new Pair<>(info, 1));
        }
    }

    @Override
    public void juice(String key) {

    }
}
