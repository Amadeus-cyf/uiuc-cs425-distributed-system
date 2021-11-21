package mp3;

import mp3.constant.FilePath;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileSplitter {
    public List<String> split(String fileName, int numSplits) {
        File file = new File(fileName);
        long bytePerSplit = file.length() / numSplits;
        long remainBytes = file.length() % numSplits;
        BufferedReader fIn = null;
        List<String> splitFiles = new ArrayList<>();
        try {
            fIn = new BufferedReader(new FileReader(FilePath.ROOT + file.getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(fIn == null) {
            return null;
        }
        String sourceName = file.getName();
        String line = null;
        String newline = "\n";
        for(int i = 1; i < numSplits + 1; i++) {
            FileOutputStream outputStream = null;
            StringBuilder sb = new StringBuilder();
            final String path = sb.append(FilePath.INTERMEDIATE_PATH).append(FilePath.SPLIT_DIRECTORY).append(sourceName).append("_split_").append(i).toString();
            File split  = new File(FilePath.INTERMEDIATE_PATH + FilePath.SPLIT_DIRECTORY);
            if(!split.exists()) {
                System.out.println("Create Split directory for splitted files: " + split.mkdirs());
            }
            try {
                outputStream = new FileOutputStream(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(outputStream == null) {
                return null;
            }
            long bytesRead = bytePerSplit;
            if(i == numSplits) {
                bytesRead += remainBytes;
            }
            try {
                long sizeRead = 0;
                while(sizeRead < bytesRead && (line = fIn.readLine()) != null) {
                    outputStream.write(line.getBytes());
                    outputStream.write(newline.getBytes());
                    sizeRead += (line.length() + 1);
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
            fIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return splitFiles;
    }
}
