package mp3.application;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingCompare extends MapleJuice<String, Object> {
    @Override
    public void maple(String line) {
        String[] infoList = line.split(" "); // name1, name2 (n1 dominates n2)
        String A = infoList[0];
        String B = infoList[1];
        mapleOutput.add(new Pair<>(
            A,
            B
        ));
    }

    @Override
    public void juice(String filePath) { // 1 juice with all pairs of A dominates B
        BufferedReader fIn = null;
        Map<String, Integer> compare = new HashMap<String, Integer>();
        try {
            fIn = new BufferedReader(new FileReader(filePath));
            String line = fIn.readLine();
            String key = line.split(" ")[0];
            compare.put(
                key,
                1
            );
            while ((line = fIn.readLine()) != null) {
                key = line.split(" ")[0];
                if (compare.containsKey(key)) {
                    int curVote = compare.get(key);
                    compare.put(
                        key,
                        curVote + 1
                    );
                } else {
                    compare.put(
                        key,
                        1
                    );
                }
            }
            int numCandidate = compare.size();
            int maxVote = 0;
            int val;
            for (Map.Entry<String, Integer> entry : compare.entrySet()) {
                key = entry.getKey();
                val = entry.getValue();
                if (val == numCandidate - 1) {
                    juiceOutput.add(new Pair<>(
                        key,
                        "Condorcet winner!"
                    ));
                    return;
                }
                if (val > maxVote) {
                    maxVote = val;
                }
            }
            List<String> highest = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : compare.entrySet()) {
                key = entry.getKey();
                val = entry.getValue();
                if (val == maxVote) {
                    highest.add(key);
                }
            }
            juiceOutput.add(new Pair<>(
                highest.toString(),
                "No Condorcet winner, Highest Condorcet counts"
            ));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
