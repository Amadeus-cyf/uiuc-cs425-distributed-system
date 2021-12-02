package mp3;

import mp2.DataTransfer;
import mp2.ServerInfo;
import mp3.constant.*;
import mp3.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterReceiver extends Receiver {
    private Set<ServerInfo> servers;
    private Map<String, Integer> mapleRunningServers;                               // map source file to number of maple exe
    private Map<String, Integer> juiceRunningServers;                               // map dest file to number of juice exe
    private Map<String, String> destToIntermediate;                                 // map dest file to intermediate prefix at juice stage
    private Map<String, Set<ServerInfo>> runningServers;                            // map input file name to those servers assigned tasks
    private Map<ServerInfo, Task> assignedTasks;                                    // map server to its assigned tasks
    private JSONObject juiceRequest = null;
    private boolean isMapleComplete = false;
    private final String MAPLE = "maple";
    private final String JUICE = "juice";

    public MasterReceiver(DataTransfer dataTransfer) {
        super(MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT, dataTransfer);
        this.mapleRunningServers = new ConcurrentHashMap<>();
        this.juiceRunningServers = new ConcurrentHashMap<>();
        this.destToIntermediate = new ConcurrentHashMap<>();
        this.runningServers = new ConcurrentHashMap<>();
        this.assignedTasks = new ConcurrentHashMap<>();
        this.servers = ConcurrentHashMap.newKeySet();
        this.servers.add(new ServerInfo(MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT));
    }

    public void start() {
        ExecutorService service = Executors.newFixedThreadPool(5);
        while(true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.dataTransfer.receive(receivedPacket);
            service.execute(() -> {
                String msg = readBytes(buffer, receivedPacket.getLength());
                receive(msg);
            });
        }
    }

    private void receive(String msg) {
        JSONObject msgJson  = new JSONObject(msg);
        System.out.println(msgJson.toString());
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        switch(msgType) {
            case(MsgType.MAPLE_REQUEST):
                handleMapleRequest(msgJson);
                break;
            case(MsgType.MAPLE_FILE_MSG):
                handleMapleFileMsg(msgJson);
                break;
            case(MsgType.MAPLE_COMPLETE_MSG):
                handleMapleCompleteMsg(msgJson);
                break;
            case(MsgType.MAPLE_ACK_REQUEST):
                handleMapleAckRequest(msgJson);
                break;
            case(MsgType.MAPLE_ACK):
                handleMapleAck(msgJson);
                break;
            case(MsgType.JUICE_REQUEST):
                handleJuiceRequest(msgJson);
                break;
            case(MsgType.JUICE_FILES_MSG):
                handleJuiceFilesMsg(msgJson);
                break;
            case(MsgType.JUICE_COMPLETE_MSG):
                handleJuiceCompleteMsg(msgJson);
                break;
            case(MsgType.JUICE_ACK_REQUEST):
                handleJuiceAckRequest(msgJson);
                break;
            case(MsgType.JUICE_ACK):
                handleJuiceAck(msgJson);
                break;
            case(MsgType.JOIN_REQUEST):
                handleJoinRequest(msgJson);
                break;
            case(MsgType.SERVER_FAIL):
                handleFailure(msgJson);
                break;
            case(MsgType.FP_REJOIN_MSG):
                handleFPRejoinMsg(msgJson);
                break;
        }
    }

    /*
    * called when maple command is sent from some server
     */
    private void handleMapleRequest(JSONObject msgJson) {
        File dir = new File(FilePath.INTERMEDIATE_PATH);
        if(!dir.exists()) {
            System.out.println("Create Root directory for intermediate files: " + dir.mkdirs());
        }
        System.out.println("Receive Maple Request: " + msgJson.toString());
        System.out.println(this.servers);
        String sourceName = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String mapleExe = msgJson.getString(MsgKey.MAPLE_EXE);
        int mapleNum = msgJson.getInt(MsgKey.NUM_MAPLE);
        mapleNum = Math.min(mapleNum, this.servers.size());
        mapleRunningServers.put(sourceName, mapleNum);
        FileSplitter splitter = new FileSplitter();
        List<String> splitFiles = splitter.split(sourceName, mapleNum);
        System.out.println(splitFiles);
        ServerInfo[] serverInfos = randomPickNServers(mapleNum);
        Set<ServerInfo> set = runningServers.computeIfAbsent(sourceName, k -> ConcurrentHashMap.newKeySet());
        Collections.addAll(set, serverInfos);
        if(splitFiles != null) {
            for(int i = 0; i < splitFiles.size(); i++) {
                String splitFileName = splitFiles.get(i);
                Message mapleFileMsg = new MapleFileMsg(sourceName, splitFileName, intermediatePrefix, mapleExe);
                List<String> assignedFiles = new ArrayList<>();
                assignedFiles.add(splitFileName);
                assignedTasks.put(serverInfos[i], new Task(sourceName, intermediatePrefix, assignedFiles, mapleExe, MAPLE, 0));
                System.out.println("Send " + splitFiles.get(i) + " to " + serverInfos[i].getIpAddress() + ":" + serverInfos[i].getPort());
                dataTransfer.send(mapleFileMsg.toJSON(), serverInfos[i].getIpAddress(), serverInfos[i].getPort());
            }
        }
    }

    /*
    * called when receive maple complete msg from some server
     */
    private void handleMapleCompleteMsg(JSONObject msgJson) {
        System.out.println("Receive Maple Complete Msg: " + msgJson.toString());
        String sourceFileName = msgJson.getString(MsgKey.SOURCE_FILE);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        Set<ServerInfo> mapleServers = this.runningServers.get(sourceFileName);
        if(mapleServers == null || !(mapleServers.contains(new ServerInfo(senderIpAddress, senderPort)))) {
            // false positive occurs
            return;
        }
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String destFileName = msgJson.getString(MsgKey.MAPLE_INTERMEDIATE_FILE);
        String mapleOutputPath = getMapleOutputPath(sourceFileName, intermediatePrefix, destFileName);
        File dir = new File(getMapleOutputDir(sourceFileName, intermediatePrefix));
        dir.mkdir();
        this.dataTransfer.receiveFile(mapleOutputPath, FilePath.INTERMEDIATE_PATH + destFileName, senderIpAddress);
        Message requestAck = new MapleAckRequest(sourceFileName, intermediatePrefix);
        this.dataTransfer.send(requestAck.toJSON(), senderIpAddress, senderPort);
    }

    /*
    * called when receive maple ACK message from other servers
     */
    private void handleMapleAck(JSONObject msgJson) {
        System.out.println("Receive Maple ACK: " + msgJson.toString());
        String sourceFile = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        ServerInfo serverInfo = new ServerInfo(senderIpAddress, senderPort);
        System.out.println("Server " + senderIpAddress + ":" + senderPort +  " complete maple tasks: " + assignedTasks.get(serverInfo));
        this.assignedTasks.remove(serverInfo);
        System.out.println("Server " + senderIpAddress + ":" + senderPort +  " finish");
        mapleRunningServers.put(sourceFile, mapleRunningServers.get(sourceFile)-1);
        if(mapleRunningServers.get(sourceFile) == 0) {
            // all ack receive
            System.out.println("All Maple ACK receive");
            sortPairsByKeys(sourceFile, intermediatePrefix);
            System.out.println("Maple stage completes.");
            // clear assigned servers
            this.runningServers.remove(sourceFile);
            this.isMapleComplete = true;
            if(this.juiceRequest != null) {
                // the command is mapleJuice
                handleJuiceRequest(this.juiceRequest);
            }
        }
    }

    private void sortPairsByKeys(String sourceFile, String intermediatePrefix) {
        String directoryPath = getMapleOutputDir(sourceFile, intermediatePrefix);
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if(files == null) {
            return;
        }
        File dir = new File(getJuiceInputDir(intermediatePrefix));
        if(!dir.exists()) {
            dir.mkdir();
        }
        Map<String, BufferedWriter> fOutMap = new HashMap<>();
        for(File file : files) {
            BufferedReader fIn = null;
            try {
               fIn = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = null;
                while((line = fIn.readLine()) != null) {
                    String[] pair = line.split(" ");
                    BufferedWriter fOut = fOutMap.get(pair[0]);
                    if(fOut == null) {
                        String path = getJuiceInputPath(intermediatePrefix, pair[0]);
                        fOut = new BufferedWriter(new FileWriter(path));
                        fOutMap.put(pair[0], fOut);
                    }
                    fOut.append(line);
                    fOut.newLine();
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if(fIn != null) {
                        fIn.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for(BufferedWriter writer : fOutMap.values()) {
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * called when receive juice command from some server
     */
    private void handleJuiceRequest(JSONObject msgJson) {
        System.out.println("Receive Juice Request: " + msgJson.toString());
        if(!isMapleComplete && juiceRequest == null) {
            System.out.println("MAPLE NOT COMPLETE");
            this.juiceRequest = msgJson;
            return;
        }
        this.juiceRequest = null;
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        File dir = new File(getJuiceInputDir(intermediatePrefix));
        String destFileName = msgJson.getString(MsgKey.DEST_FILE);
        String juiceExe = msgJson.getString(MsgKey.JUICE_EXE);
        int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
        this.destToIntermediate.put(destFileName, intermediatePrefix);
        File[] files = dir.listFiles();
        if(files != null) {
            int juiceNum = Math.min(files.length, msgJson.getInt(MsgKey.NUM_JUICE));
            juiceNum = Math.min(this.servers.size(), juiceNum);
            this.juiceRunningServers.put(destFileName, juiceNum);
            ServerInfo[] serverInfos = randomPickNServers(juiceNum);
            for(ServerInfo serverInfo : serverInfos) {
                System.out.println("Server " + serverInfo.getIpAddress() + ":" + serverInfo.getPort() + " selected for juice");
            }
            Set<ServerInfo> set = runningServers.computeIfAbsent(intermediatePrefix, k -> new HashSet<>());
            Collections.addAll(set, serverInfos);
            long numFilePerServer = files.length / juiceNum;
            int serverIdx = 0;
            for(int i = 0; i < files.length; i += numFilePerServer) {
                List<String> filesAssigned = new ArrayList<>();
                long end = (serverIdx == juiceNum - 1) ? files.length : (i + numFilePerServer);
                for(int j = i; j < end; j++) {
                    filesAssigned.add(files[j].getName());
                }
                this.assignedTasks.put(serverInfos[serverIdx], new Task(intermediatePrefix, destFileName, filesAssigned, juiceExe, JUICE, isDelete));
                System.out.println(serverInfos[serverIdx].getIpAddress() + ":" + serverInfos[serverIdx].getPort() + " has " + filesAssigned);
                Message juiceFilesMsg = new JuiceFilesMsg(filesAssigned, intermediatePrefix, destFileName, juiceExe, isDelete);
                this.dataTransfer.send(juiceFilesMsg.toJSON(), serverInfos[serverIdx].getIpAddress(), serverInfos[serverIdx].getPort());
                serverIdx++;
                if(serverIdx >= juiceNum) {
                    break;
                }
            }
        }
        // delete the dest file if it already exists
        File file = new File(destFileName);
        if(file.exists()) {
            file.delete();
        }
    }

    /*
     * called when receive juice complete msg from some server
     */
    private void handleJuiceCompleteMsg(JSONObject msgJson) {
        System.out.println("Receive Juice Complete Msg: " + msgJson.toString());
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        String destFile = msgJson.getString(MsgKey.DEST_FILE);
        String intermediatePrefix = this.destToIntermediate.get(destFile);
        Set<ServerInfo> juiceServers = this.runningServers.get(intermediatePrefix);
        if(juiceServers == null || !(juiceServers.contains(new ServerInfo(senderIpAddress, senderPort)))) {
            // false positive occurs
            return;
        }
        String juiceIntermediateFile = msgJson.getString(MsgKey.JUICE_INTERMEDIATE_FILE);
        String juiceOutputTmpPath = getJuiceOutputTmpPath(destFile, senderIpAddress, senderPort);
        this.dataTransfer.receiveFile(juiceOutputTmpPath, juiceIntermediateFile, senderIpAddress);
        String juiceOutputRemotePath = getJuiceOutputRemotePath(destFile);
        BufferedWriter fOut = null;
        BufferedReader fIn = null;
        try {
            fIn = new BufferedReader(new FileReader(juiceOutputTmpPath));
            fOut = new BufferedWriter(new FileWriter(juiceOutputRemotePath, true));
            String line = null;
            while((line = fIn.readLine()) != null) {
                fOut.write(line);
                fOut.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fIn != null) {
                try {
                    fIn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(fOut != null) {
                try {
                    fOut.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
        Message juiceRequestAck = new JuiceAckRequest(destFile, isDelete);
        this.dataTransfer.send(juiceRequestAck.toJSON(), senderIpAddress, senderPort);
    }

    /*
    * called when receive juice ack messages from other servers
     */
    private void handleJuiceAck(JSONObject msgJson) {
        System.out.println("Receive Juice Ack: " + msgJson.toString());
        String destFile = msgJson.getString(MsgKey.DEST_FILE);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        ServerInfo serverInfo = new ServerInfo(senderIpAddress, senderPort);
        System.out.println("Server " + senderIpAddress + ":" + senderPort + " completes juice tasks: " + assignedTasks.get(serverInfo));
        this.assignedTasks.remove(serverInfo);
        System.out.println("Server " + senderIpAddress + ":" + senderPort +  " finish");
        juiceRunningServers.put(destFile, juiceRunningServers.get(destFile) - 1);
        if(juiceRunningServers.get(destFile) <= 0) {
            int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
            System.out.println("Delete intermediate directory");
            // delete intermediate directory
            deleteDir(FilePath.INTERMEDIATE_PATH);
            String intermediatePrefix = destToIntermediate.get(destFile);
            if(isDelete == 1) {
                File file = new File(intermediatePrefix);
                // we need to delete all juice input files
                System.out.println("Delete files in " + intermediatePrefix);
                deleteDir(file.getAbsolutePath());
            }
            this.runningServers.remove(intermediatePrefix);
            this.juiceRequest = null;
            this.isMapleComplete = false;
            System.out.println("Juice Stage completes.");
        }
    }

    /*
    * called when new servers join the system
     */
    private void handleJoinRequest(JSONObject msgJson) {
        System.out.println("Receive Join Request " + msgJson.toString());
        JSONArray serverList = msgJson.getJSONArray(mp2.constant.MsgKey.MEMBERSHIP_LIST);
        for(int i = 0; i < serverList.length(); i++) {
            JSONObject server = serverList.getJSONObject(i);
            String ipAddress = server.getString(MsgKey.IP_ADDRESS);
            int port = server.getInt(MsgKey.PORT);
            ServerInfo serverInfo = new ServerInfo(ipAddress, port);
            if(!servers.contains(serverInfo)) {
                System.out.println("Server " + ipAddress + ":" + port + " joins the system");
                servers.add(serverInfo);
            }
        }
    }

    /*
    * called when failure detected
     */
    private void handleFailure(JSONObject msgJson) {
        System.out.println("Receive Failure " + msgJson.toString());
        String failedIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int failedPort = msgJson.getInt(MsgKey.PORT);
        ServerInfo failedServer = new ServerInfo(failedIpAddress, failedPort);
        if(!this.servers.contains(failedServer)) {
            System.out.println("Server " + failedIpAddress + ":" + failedPort + " has already be cleaned out");
            return;
        }
        this.servers.remove(failedServer);
        for(Map.Entry<String, Set<ServerInfo>> entry : this.runningServers.entrySet()) {
            Set<ServerInfo> executingServers = entry.getValue();
            if(executingServers.contains(failedServer)) {
                executingServers.remove(failedServer);
                System.out.println(entry.getKey() + " needs to be assigned");
            }
        }
        Task task = this.assignedTasks.get(failedServer);
        this.assignedTasks.remove(failedServer);
        if(task != null) {
            System.out.println("Reassign tasks");
            // reassign the task to a free server
            ServerInfo server = findFreeServer();
            System.out.println("Select free server " + server.getIpAddress() + ":" + server.getPort());
            String inputFileName = task.getInputFileName();
            this.runningServers.get(inputFileName).add(server);
            this.assignedTasks.put(server, task);
            String type = task.getType();
            System.out.println("Task type: " + type);
            if(type.equals(MAPLE)) {
                System.out.println("Assign " + task.getAssignedFiles().get(0) + " to " + server.getIpAddress() + ":" + server.getPort());
                // currently at maple stage
                System.out.println("Assign maple tasks to newly selected server " + server.getIpAddress() + ":" + server.getPort());
                MapleFileMsg mapleFileMsg = new MapleFileMsg(inputFileName, task.getAssignedFiles().get(0), task.getOutputFileName(), task.getExeFunc());
                this.dataTransfer.send(mapleFileMsg.toJSON(), server.getIpAddress(), server.getPort());
            } else if(type.equals(JUICE)){
                System.out.println("Assign juice tasks to newly selected server " + server.getIpAddress() + ":" + server.getPort());
                JuiceFilesMsg juiceFilesMsg = new JuiceFilesMsg(task.getAssignedFiles(), inputFileName, task.getOutputFileName(), task.getExeFunc(), task.getIsDelete());
                this.dataTransfer.send(juiceFilesMsg.toJSON(), server.getIpAddress(), server.getPort());
            } else {
                System.out.println("Invalid task type");
            }
        }
    }

    /*
    * called when false positive occurs or a server rejoins the system
     */
    private void handleFPRejoinMsg(JSONObject msgJson) {
        System.out.println("Receive FP Rejoin Message " + msgJson.toString());
        String ipAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int port = msgJson.getInt(MsgKey.PORT);
        System.out.println("Server " + ipAddress + ":" + port + " joins the system");
        this.servers.add(new ServerInfo(ipAddress, port));
    }

    private ServerInfo[] randomPickNServers(int n) {
        Random random = new Random();
        ServerInfo[] result = new ServerInfo[n];
        int idx = 0;
        int ceil = Math.min(n, this.servers.size());
        int[] nums = new int[ceil];
        while(idx < n) {
            int num = random.nextInt(ceil);
            boolean isExist = false;
            for(int i = 0; i < idx; i++) {
                if(nums[i] == num) {
                    isExist = true;
                    break;
                }
            }
            if(!isExist) {
                nums[idx] = num;
                idx++;
            }
        }
        List<ServerInfo> serverList = new ArrayList<>(this.servers);
        for(int i = 0; i < n; i++) {
            result[i] = serverList.get(nums[i]);
        }
        return result;
    }

    private ServerInfo findFreeServer() {
        for(ServerInfo server : this.servers) {
            if(assignedTasks.get(server) == null) {
                return server;
            }
        }
        // there is no free server, then randomly pick a server to assign the task
        ServerInfo[] serverInfo = randomPickNServers(1);
        return serverInfo[0];
    }

    private String getMapleOutputPath(String sourceFileName, String intermediatePrefix, String destFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.INTERMEDIATE_PATH).append(sourceFileName).append("_").append(intermediatePrefix).append("_maple_out/").append(destFileName).toString();
    }

    private String getMapleOutputDir(String sourceFileName, String intermediatePrefix) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.INTERMEDIATE_PATH).append(sourceFileName).append("_").append(intermediatePrefix).append("_maple_out/").toString();
    }

    private String getJuiceInputPath(String intermediatePrefix, String key) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(intermediatePrefix).append("/").append(intermediatePrefix).append("_").append(key).toString();
    }

    private String getJuiceInputDir(String intermediatePrefix) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(intermediatePrefix).toString();
    }

    private String getJuiceOutputTmpPath(String destFile, String senderIp, int senderPort) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.INTERMEDIATE_PATH).append(destFile).append("_").append(senderIp).append("_").append(senderPort).append("_tmp").toString();
    }

    private String getJuiceOutputRemotePath(String destFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(destFileName).toString();
    }
}
