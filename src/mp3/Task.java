package mp3;

import java.util.List;

/**
 * @param type     either maple or juice
 * @param isDelete only for juice
 */
public record Task(String inputFileName, String outputFileName, List<String> assignedFiles, String exeFunc, String type,
                   int isDelete) {

    @Override
    public String toString() {
        return inputFileName + "_" + outputFileName + "_" + assignedFiles;
    }
}
