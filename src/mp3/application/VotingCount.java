package mp3.application;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class VotingCount extends MapleJuice {
    @Override
    public void maple(String line) {
        String[] infoList = line.split(" "); // name1, name2, name3...
        int numCandidate = infoList.length;
        // loop through all the pairs
        for(int i=0; i < numCandidate-1; i++) {
            for(int j=i+1; j < numCandidate; j++) {
                String A = infoList[i];
                String B = infoList[j]; // name1 is preferable than name2
                if(A.compareTo(B) < 0) { // name1 is lexicographically smaller
                    String key = A + '_' + B;
                    mapleOutput.add(new Pair<>(key,1));
                } else {
                    String key = B + '_' + A;
                    mapleOutput.add(new Pair<>(key,0));
                }
            }
        }
    }

    @Override
    public void juice(String filePath) {
        BufferedReader fIn;
        try{
            fIn = new BufferedReader(new FileReader(filePath));
            String line = fIn.readLine();
            String[] pair = line.split(" ");
            String[] key = pair[0].split("_");
            // compare two names
            String A = key[0];
            String B = key[1];
            int val = Integer.parseInt(pair[1]);
            int countZero = 0;
            int countOne = 0;
            if(val == 0) {
                countZero++;
            } else {
                countOne++;
            }
            while((line = fIn.readLine()) != null) { // votePair, 0/1
                val = Integer.parseInt(line.split(" ")[1]);
                if(val == 0) {
                    countZero++;
                } else {
                    countOne++;
                }
            }
            // compare number of 1s and 0s
            if(key != null) {
                if(countOne >= countZero) {
                    juiceOutput.add(new Pair<>(A, B));
                } else {
                    juiceOutput.add(new Pair<>(B, A));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

