package mp3.application;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Building extends MapleJuice<Integer, Long> {
    @Override
    public void maple(String line) {
        String[] infoList = line.split(" "); // 'BIN','SHAPE_AREA', 'FEAT_CODE', 'HEIGHTROOF', 'CNSTRCT_YR'
        double shapeArea = Double.parseDouble(infoList[1]);
        double height;
        if (infoList.length < 4 || infoList[3].equals("")) {
            return;
        } else {
            height = Double.parseDouble(infoList[3]);
        }
        double constructedY;
        if (infoList.length < 5) {
            return;
        } else {
            constructedY = Double.parseDouble(infoList[4]);
        }
        //  = Double.parseDouble(infoList[4]);
        // we want to get the buildings with area > 2000 and height > 100 and constructed after 2000
        if (shapeArea > 2000.0 && height > 100.0 && constructedY > 2000.0) {
            mapleOutput.add(new Pair<>(
                1,
                1L
            )); // key = 1 means we have found one of the suitable buildings
        }
    }

    @Override
    public void juice(String filePath) {
        BufferedReader fIn;
        try {
            fIn = new BufferedReader(new FileReader(filePath));
            String line = fIn.readLine();
            String key = line.split(" ")[0];
            long count = 1;
            while (fIn.readLine() != null) {
                count++;
            }
            System.out.println(new StringBuilder().append("count: ").append(count));
            if (key != null) {
                int k = Integer.parseInt(key);
                juiceOutput.add(new Pair<>(
                    k,
                    count
                ));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
