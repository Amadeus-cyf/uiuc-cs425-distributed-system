package mp3;

import mp2.DataTransfer;
import mp3.application.MapleJuice;
import mp3.application.WordCount;
import mp3.constant.*;
import mp3.message.MapleAck;
import mp3.message.MapleCompleteMsg;
import mp3.message.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Receiver {
    protected String ipAddress;
    protected int port;
    protected DataTransfer dataTransfer;
    protected MapleJuice<?, ?> mapleJuice;
    protected int BLOCK_SIZE = 1024;

    public Receiver(String ipAddress, int port, DataTransfer dataTransfer) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = dataTransfer;
    }

    public void start() {
        while(true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.dataTransfer.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            receive(msg);
        }
    }

    private void receive(String msg) {
        System.out.println("Receive msg");
        JSONObject msgJson = new JSONObject(msg);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        switch(msgType) {
            case(MsgType.MAPLE_FILE_MSG):
                handleMapleFileMsg(msgJson);
                break;
            case(MsgType.MAPLE_ACK_REQUEST):
                handleMapleAckRequest(msgJson);
                break;
        }
    }

    protected void handleMapleFileMsg(JSONObject msgJson) {
        System.out.print("Receive Maple File Msg: " + msgJson.toString());
        String sourceFileName = msgJson.getString(MsgKey.SOURCE_FILE);
        String splitFileName = msgJson.getString(MsgKey.FILE_TO_MAPLE);
        String mapleExe = msgJson.getString(MsgKey.MAPLE_EXE);
        if (mapleExe.equals(ApplicationType.WORD_COUNT)) {
            this.mapleJuice = new WordCount();
        }
        String localSplitFilePath = FilePath.ROOT + splitFileName;
        String remoteSplitFilePath = getSplitFilePath(splitFileName);
        this.dataTransfer.receiveFile(localSplitFilePath, remoteSplitFilePath, MasterInfo.Master_IP_ADDRESS);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(localSplitFilePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (in == null) {
            return;
        }
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                mapleJuice.maple(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        String destPath = getLocalMapleOutputPath(intermediatePrefix);
        String destFileName = getMapleOutputFileName(intermediatePrefix);
        mapleJuice.writeMapleOutputToFile(destPath);
        Message mapleCompleteMsg = new MapleCompleteMsg(this.ipAddress, this.port, sourceFileName, destFileName, intermediatePrefix);
        this.dataTransfer.send(mapleCompleteMsg.toJSON(), MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }

    protected void handleMapleAckRequest(JSONObject msgJson) {
        System.out.println("Receive Maple ACK Request: " + msgJson.toString());
        String sourceFile = msgJson.getString(MsgKey.SOURCE_FILE);
        String prefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        Message mapleAck = new MapleAck(sourceFile, prefix);
        this.dataTransfer.send(mapleAck.toJSON(), MasterInfo.Master_IP_ADDRESS, MasterInfo.MASTER_PORT);
    }

    protected void handleJuiceFilesMsg(JSONObject msgJson) {
        System.out.println("Receive Juice Files Msg: " + msgJson.toString());
        JSONArray filesToJuice = msgJson.getJSONArray(MsgKey.FILES_TO_JUICE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        for (int i = 0; i < filesToJuice.length(); i++) {
            String fileName = filesToJuice.getString(i);
            this.dataTransfer.receiveFile(getJuiceInputLocalPath(fileName), getJuiceInputRemotePath(intermediatePrefix, fileName), MasterInfo.Master_IP_ADDRESS);
        }
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0; i < filesToJuice.length(); i++) {
            service.execute(new JuiceFile(filesToJuice, i));
        }
    }

    protected String getSplitFilePath(String splitFileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(FilePath.SPLIT_DIRECTORY).append(splitFileName).toString();
    }

    protected String getLocalMapleOutputPath(String prefix) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(prefix).append("_").append(this.ipAddress).append("_").append(this.port).toString();
    }

    protected String getMapleOutputFileName(String prefix) {
        StringBuilder sb = new StringBuilder();
        return sb.append(prefix).append("_").append(this.ipAddress).append("_").append(this.port).toString();
    }

    protected String getJuiceInputLocalPath(String fileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(fileName).toString();
    }

    protected String getJuiceOutputLocalPath() {
        StringBuilder sb = new StringBuilder();
        return sb.append(this.ipAddress).append("_").append(this.port).append("_juice_out").toString();
    }

    protected String getJuiceInputRemotePath(String intermediatePrefix, String fileName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(FilePath.ROOT).append(intermediatePrefix).append("/").append(fileName).toString();
    }

    /*
     * turn bytes into string
     */
    protected String readBytes(byte[] packet, int length) {
        if (packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char)(packet[i]));
        }
        return sb.toString();
    }

    private class JuiceFile implements Runnable {
        private JSONArray filesToJuice;
        private int idx;

        JuiceFile(JSONArray filesToJuice, int idx) {
            this.idx = idx;
            this.filesToJuice = filesToJuice;
        }

        @Override
        public void run() {
            String fileName = filesToJuice.getString(idx);
            BufferedReader fIn = null;
            try {
                fIn = new BufferedReader(new FileReader(getJuiceInputLocalPath(fileName)));
                String line = null;
                while ((line = fIn.readLine()) != null) {
                    mapleJuice.juice(line);
                }
                mapleJuice.writeJuiceOutputToFile(getJuiceOutputLocalPath());
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
            }
        }
    }
}
