package mp3.application;

import java.io.BufferedReader;
import java.io.FileReader;

public class WordCount extends MapleJuice<String, Integer> {
    @Override
    public void maple(String line) {
        String[] infoList = line.split(" ");
        for (String info : infoList) {
            mapleOutput.add(new Pair<>(info, 1));
        }
    }

    @Override
    public void juice(String filePath) {
        BufferedReader fIn = null;
        try {
            fIn = new BufferedReader(new FileReader(filePath));
            String line = fIn.readLine();
            String key = line.split("_")[0];
            int count = 0;
            while((line = fIn.readLine()) != null) {
                count++;
            }
            if (key != null) {
                    juiceOutput.add(new Pair<>(key, count));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
