package mp3.application;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class MapleJuice<K, V>{
    protected List<Pair<K, V>> mapleOutput;
    protected List<Pair<K, V>> juiceOutput;

    public MapleJuice() {
        this.mapleOutput = new ArrayList<>();
        this.juiceOutput = new ArrayList<>();
    }

    public abstract void maple(String line);

    public abstract void juice(String key);

    public void writeMapleOutputToFile(String filePath) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outputStream == null) {
            return;
        }
        String space = " ";
        for (Pair pair : mapleOutput) {
            StringBuilder sb = new StringBuilder();
            String line = sb.append(pair.key.toString()).append(space).append(pair.val.toString()).toString();
            try {
                outputStream.write(line.getBytes());
            } catch (Exception e) {
                e.printStackTrace();;
            }
        }
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
