package mp3;

import mp2.DataTransfer;
import mp2.ServerInfo;
import mp3.constant.*;
import mp3.message.*;
import org.json.JSONObject;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;

public class MasterReceiver extends Receiver {
    private Map<String, List<String>> intermediateMapleFiles;                       // map source file to output files generate by maple
    private Set<String> files;
    private Map<String, Integer> mapleRunningServers;                               // map source file to number of maple exe
    private Map<String, Integer> juiceRunningServers;                               // map dest file to number of juice exe
    private Map<String, String> destToIntermediate;                                 // map dest file to intermediate prefix at juice stage

    public MasterReceiver(DataTransfer dataTransfer) {
        super(MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT, dataTransfer);
        this.intermediateMapleFiles = new HashMap<>();
        this.files = new HashSet<>();
        files.add("test");
        this.mapleRunningServers = new HashMap<>();
        this.juiceRunningServers = new HashMap<>();
        this.destToIntermediate = new HashMap<>();
    }

    public void start() {
        while (true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.dataTransfer.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            receive(msg);
        }
    }

    private void receive(String msg) {
        JSONObject msgJson  = new JSONObject(msg);
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
        }
    }

    /*
    * called when maple command is sent from some server
     */
    private void handleMapleRequest(JSONObject msgJson) {
        System.out.println("Receive Maple Request: " + msgJson.toString());
        String sourceName = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String mapleExe = msgJson.getString(MsgKey.MAPLE_EXE);
        int mapleNum = msgJson.getInt(MsgKey.NUM_MAPLE);
        mapleRunningServers.put(sourceName, mapleNum);
        if (files.contains(sourceName)) {
            FileSplitter splitter = new FileSplitter(sourceName, mapleNum);
            List<String> splitFiles = splitter.split();
            ServerInfo[] serverInfos = randomPickNServers(mapleNum);
            if (splitFiles != null) {
                intermediateMapleFiles.put(sourceName, splitFiles);
                for (int i = 0; i < splitFiles.size(); i++) {
                    String splitFileName = splitFiles.get(i);
                    Message mapleFileMsg = new MapleFileMsg(sourceName, splitFileName, intermediatePrefix, mapleExe);
                    System.out.println("Send " + splitFiles.get(i) + " to " + serverInfos[i].getIpAddress() + ":" + serverInfos[i].getPort());
                    dataTransfer.send(mapleFileMsg.toJSON(), serverInfos[i].getIpAddress(), serverInfos[i].getPort());
                }
            }
        }
    }

    /*
    * called when receive maple complete msg from some server
     */
    private void handleMapleCompleteMsg(JSONObject msgJson) {
        System.out.println("Receive Maple Complete Msg: " + msgJson.toString());
        String sourceFileName = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String destFileName = msgJson.getString(MsgKey.MAPLE_INTERMEDIATE_FILE);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        String mapleOutputPath = getMapleOutputPath(sourceFileName, destFileName);
        File dir = new File(sourceFileName + "_maple_out");
        System.out.println("MAKE DIRC: " + dir.mkdir());
        this.dataTransfer.receiveFile(mapleOutputPath, FilePath.ROOT + destFileName, senderIpAddress);
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
        mapleRunningServers.put(sourceFile, mapleRunningServers.get(sourceFile)-1);
        if (mapleRunningServers.get(sourceFile) == 0) {
            // all ack receive
            System.out.println("All Maple ACK receive");
            sortPairsByKeys(sourceFile, intermediatePrefix);
        }
    }

    private void sortPairsByKeys(String sourceFile, String intermediatePrefix) {
        String directoryPath = getMapleOutputDir(sourceFile);
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        File dir = new File(intermediatePrefix);
        if (!dir.exists()) {
            dir.mkdir();
        }
        Map<String, BufferedWriter> fOutMap = new HashMap<>();
        for (File file : files) {
            BufferedReader fIn = null;
            try {
               fIn = new BufferedReader(new FileReader(file.getAbsoluteFile()));
                String line = null;
                while ((line = fIn.readLine()) != null) {
                    String[] pair = line.split(" ");
                    System.out.println("Key: " + pair[0]);
                    BufferedWriter fOut = fOutMap.get(pair[0]);
                    if (fOut == null) {
                        String path = getJuiceInputPath(intermediatePrefix, pair[0]);
                        fOut = new BufferedWriter(new FileWriter(path, true));
                        fOutMap.put(pair[0], fOut);
                    }
                    fOut.append(line);
                    fOut.newLine();
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fIn != null) {
                        fIn.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (BufferedWriter writer : fOutMap.values()) {
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
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String destFileName = msgJson.getString(MsgKey.DEST_FILE);
        String juiceExe = msgJson.getString(MsgKey.JUICE_EXE);
        int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
        this.destToIntermediate.put(destFileName, intermediatePrefix);
        File dir = new File(getJuiceInputDir(intermediatePrefix));
        File[] files = dir.listFiles();
        if (files != null) {
            int juiceNum = msgJson.getInt(MsgKey.NUM_JUICE);
            this.juiceRunningServers.put(destFileName, juiceNum);
            ServerInfo[] serverInfos = randomPickNServers(juiceNum);
            long numFilePerServer = files.length / juiceNum;
            int serverIdx = 0;
            for (int i = 0; i < files.length; i += numFilePerServer) {
                List<String> filesAssigned = new ArrayList<>();
                long end = (serverIdx == serverInfos.length - 1) ? files.length : (i + numFilePerServer);
                for (int j = i; j < end; j++) {
                    filesAssigned.add(files[j].getName());
                }
                Message juiceFilesMsg = new JuiceFilesMsg(filesAssigned, intermediatePrefix, destFileName, juiceExe, isDelete);
                this.dataTransfer.send(juiceFilesMsg.toJSON(), serverInfos[serverIdx].getIpAddress(), serverInfos[serverIdx].getPort());
                serverIdx++;
            }
        }
        // delete the dest file if it already exists
        File file = new File(destFileName);
        if (file.exists()) {
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
        String juiceIntermediateFile = msgJson.getString(MsgKey.JUICE_INTERMEDIATE_FILE);
        String destFile = msgJson.getString(MsgKey.DEST_FILE);
        String juiceOutputTmpPath = getJuiceOutputTmpPath(destFile, senderIpAddress, senderPort);
        this.dataTransfer.receiveFile(juiceOutputTmpPath, FilePath.ROOT + juiceIntermediateFile, senderIpAddress);
        String juiceOutputRemotePath = getJuiceOutputRemotePath(destFile);
        BufferedWriter fOut = null;
        BufferedReader fIn = null;
        try {
            fIn = new BufferedReader(new FileReader(juiceOutputTmpPath));
            fOut = new BufferedWriter(new FileWriter(juiceOutputRemotePath, true));
            String line = null;
            while ((line = fIn.readLine()) != null) {
                fOut.append(line);
                fOut.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fIn != null) {
                try {
                    fIn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (fOut != null) {
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
        String destFile = msgJson.getString(MsgKey.DEST_FILE);
        juiceRunningServers.put(destFile, juiceRunningServers.get(destFile) - 1);
        if (juiceRunningServers.get(destFile) <= 0) {
            int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
            if (isDelete == 1) {
                // we need to delete all juice input files
                String intermediatePrefix = destToIntermediate.get(destFile);
                File file = new File(intermediatePrefix);
                file.delete();
            }
        }
    }

    private ServerInfo[] randomPickNServers(int n) {
        Random random = new Random();
        ServerInfo[] result = new ServerInfo[n];
        int[] nums = new int[n];
        int idx = 0;
        while (idx < n) {
            int num = random.nextInt(ServerInfoConst.NUM_SERVERS);
            boolean isExist = false;
            for (int i = 0; i < idx; i++) {
                if (nums[i] == num) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                nums[idx] = num;
                idx++;
            }
        }
        for (int i = 0; i < n; i++) {
            result[i] = new ServerInfo(ServerInfoConst.SERVER_ROOT_IP, ServerInfoConst.SERVER_PORT + nums[i] * 100);
        }
        return result;
    }

    private String getMapleOutputPath(String sourceFileName, String destFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(sourceFileName).append("_maple_out/").append(destFileName).toString();
    }

    private String getMapleOutputDir(String sourceFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(sourceFileName).append("_maple_out/").toString();
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
        return sb.append(FilePath.ROOT).append(destFile).append("_").append(senderIp).append("_").append(senderPort).append("_tmp").toString();
    }

    private String getJuiceOutputRemotePath(String destFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(destFileName).toString();
    }
}
