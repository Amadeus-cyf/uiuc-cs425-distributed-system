package mp3;

import mp3.constant.FilePath;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileSplitter {
    private File file;
    private int numSplits;

    public FileSplitter(String fileName, int numSplits) {
        this.file = new File(fileName);
        this.numSplits = numSplits;
    }

    public List<String> split() {
        long bytePerSplit = file.length() / numSplits;
        long remainBytes = file.length() % numSplits;
        BufferedReader in = null;
        List<String> splitFiles = new ArrayList<>();
        try {
            in = new BufferedReader(new FileReader(file.getAbsolutePath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (in == null) {
            return null;
        }
        String sourceName = file.getName();
        for (int i = 1; i < numSplits + 1; i++) {
            FileOutputStream outputStream = null;
            StringBuilder sb = new StringBuilder();
            final String path = sb.append(FilePath.ROOT).append(FilePath.SPLIT_DIRECTORY).append(sourceName).append("_split_").append(i).toString();
            try {
                outputStream = new FileOutputStream(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (outputStream == null) {
                return null;
            }
            long bytesRead = bytePerSplit;
            if (i == numSplits) {
                bytesRead += remainBytes;
            }
            try {
                String line = null;
                long sizeRead = 0;
                while ((line = in.readLine()) != null && sizeRead < bytesRead) {
                    outputStream.write(line.getBytes());
                    sizeRead += line.length();
                }
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.setLength(0);
            String splitFileName = sb.append(sourceName).append("_split_").append(i).toString();
            splitFiles.add(splitFileName);
        }
        try {
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return splitFiles;
    }
}
