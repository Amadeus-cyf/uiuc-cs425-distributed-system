package mp3;

import mp2.DataTransfer;
import mp2.ServerInfo;
import mp3.constant.*;
import mp3.message.MapleFileMsg;
import mp3.message.Message;
import mp3.message.MapleAckRequest;
import org.json.JSONObject;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;

public class MasterReceiver extends Receiver {
    private Map<String, List<String>> intermediateFiles;                    // map source file to output files generate by maple
    private Set<String> files;
    private Map<String, Integer> runningServers;                            // map source file to number of maple exe

    public MasterReceiver(DataTransfer dataTransfer) {
        super(MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT, dataTransfer);
        this.intermediateFiles = new HashMap<>();
        this.files = new HashSet<>();
        this.runningServers = new HashMap<>();
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
        }
    }

    private void handleMapleRequest(JSONObject msgJson) {
        String sourceName = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String mapleExe = msgJson.getString(MsgKey.MAPLE_EXE);
        int mapleNum = msgJson.getInt(MsgKey.NUM_MAPLE);
        runningServers.put(sourceName, mapleNum);
        if (files.contains(sourceName)) {
            FileSplitter splitter = new FileSplitter(sourceName, mapleNum);
            List<String> splitFiles = splitter.split();
            ServerInfo[] serverInfos = randomPickNServers(mapleNum);
            if (splitFiles != null) {
                intermediateFiles.put(sourceName, splitFiles);
                for (int i = 0; i < splitFiles.size(); i++) {
                    String splitFileName = splitFiles.get(i);
                    Message mapleFileMsg = new MapleFileMsg(sourceName, splitFileName, intermediatePrefix, mapleExe);
                    dataTransfer.send(mapleFileMsg.toJSON(), serverInfos[i].getIpAddress(), serverInfos[i].getPort());
                }
            }
        }
    }

    /*
    * called when receive maple complete msg from some server
     */
    private void handleMapleCompleteMsg(JSONObject msgJson) {
        String sourceFileName = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String destFileName = msgJson.getString(MsgKey.DEST_FILE);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        String mapleOutputPath = getMapleOutputPath(sourceFileName, destFileName);
        this.dataTransfer.receiveFile(mapleOutputPath, FilePath.ROOT + destFileName, senderIpAddress);
        Message requestAck = new MapleAckRequest(sourceFileName, intermediatePrefix);
        this.dataTransfer.send(requestAck.toJSON(), senderIpAddress, senderPort);
    }

    private void handleMapleAck(JSONObject msgJson) {
        String sourceFile = msgJson.getString(MsgKey.SOURCE_FILE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        runningServers.put(sourceFile, runningServers.get(sourceFile)-1);
        if (runningServers.get(sourceFile) == 0) {
            // all ack receive
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
        for (File file : files) {
            BufferedReader in = null;
            BufferedWriter out = null;
            try {
               in = new BufferedReader(new FileReader(file.getAbsoluteFile()));
               out = null;
                String line = null;
                while ((line = in.readLine()) != null) {
                    String[] pair = line.split(" ");
                    String path = getJuiceInputPath(sourceFile, intermediatePrefix, pair[0]);
                    out = new BufferedWriter(new FileWriter(path));
                    out.write(line);
                    out.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleJuiceRequest(JSONObject msgJson) {

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

    private String getJuiceInputPath(String sourceFile, String intermediatePrefix, String key) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(sourceFile).append("_").append(intermediatePrefix).append("/").append(intermediatePrefix).append("_").append(key).toString();
    }

    private String getMapleOutputPath(String sourceFileName, String destFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(sourceFileName).append("_maple_out/").append(destFileName).toString();
    }

    private String getMapleOutputDir(String intermediatePrefix) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(intermediatePrefix).append("_maple_out/").toString();
    }
}
