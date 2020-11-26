package mp3;

import java.util.List;

public class Task {
    private List<String> assignedFiles;
    private String type;                            // either maple or juice
    private String inputFileName;
    private String outputFileName;
    private String exeFunc;
    private int isDelete;                       // only for juice

    public Task(String inputFileName, String outputFileName,  List<String> assignedFiles, String execFunc, String type, int isDelete) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.assignedFiles = assignedFiles;
        this.type = type;
        this.exeFunc = execFunc;
        this.isDelete = isDelete;
    }

    public String getInputFileName() {
        return this.inputFileName;
    }

    public String getOutputFileName() {
        return this.outputFileName;
    }

    public List<String> getAssignedFiles() {
        return this.assignedFiles;
    }

    public String getType() {
        return this.type;
    }

    public String getExeFunc() {
        return this.exeFunc;
    }

    public int getIsDelete() {
        return this.isDelete;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append(inputFileName).append("_").append(outputFileName).append("_").append(assignedFiles).toString();
    }
}
