package mp3.application;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public abstract class MapleJuice<K, V> {
    protected List<Pair<K, V>> mapleOutput;
    protected Vector<Pair<K, V>> juiceOutput;

    public MapleJuice() {
        this.mapleOutput = new ArrayList<>();
        this.juiceOutput = new Vector<>();
    }

    public abstract void maple(String line);

    public abstract void juice(String filePath);

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
        String newline = "\n";
        for (Pair pair : mapleOutput) {
            StringBuilder sb = new StringBuilder();
            String line = sb.append(pair.key.toString()).append(space).append(pair.val.toString()).toString();
            try {
                outputStream.write(line.getBytes());
                outputStream.write(newline.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeJuiceOutputToFile(String filePath) {
        FileOutputStream fOut = null;
        String space = " ";
        String newline = "\n";
        try {
            fOut = new FileOutputStream(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fOut == null) {
            return;
        }
        for (Pair<K, V> pair : juiceOutput) {
            StringBuilder sb = new StringBuilder();
            String line = sb.append(pair.key.toString()).append(space).append(pair.val.toString()).toString();
            try {
                fOut.write(line.getBytes());
                fOut.write(newline.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
