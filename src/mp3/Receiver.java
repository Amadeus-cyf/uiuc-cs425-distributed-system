package mp3;

import mp2.DataTransfer;
import mp3.application.MapleJuice;
import mp3.application.MapleJuiceFactory;
import mp3.constant.FilePath;
import mp3.constant.MasterInfo;
import mp3.constant.MsgKey;
import mp3.constant.MsgType;
import mp3.message.JuiceAck;
import mp3.message.JuiceCompleteMsg;
import mp3.message.MapleAck;
import mp3.message.MapleCompleteMsg;
import mp3.message.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Receiver {
    protected String ipAddress;
    protected int port;
    protected DataTransfer dataTransfer;
    protected MapleJuice<?, ?> mapleJuice;
    protected int BLOCK_SIZE = 1024;

    public Receiver(
        String ipAddress,
        int port,
        DataTransfer dataTransfer
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataTransfer = dataTransfer;
        File dir = new File(FilePath.INTERMEDIATE_PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public void start() {
        ExecutorService service = Executors.newFixedThreadPool(5);
        while (true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(
                buffer,
                buffer.length
            );
            this.dataTransfer.receive(receivedPacket);
            String msg = readBytes(
                buffer,
                receivedPacket.getLength()
            );
            service.execute(() -> {
                receive(msg);
            });
        }
    }

    private void receive(String msg) {
        System.out.println("Receive msg");
        JSONObject msgJson = new JSONObject(msg);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        switch (msgType) {
            case (MsgType.MAPLE_FILE_MSG) -> handleMapleFileMsg(msgJson);
            case (MsgType.MAPLE_ACK_REQUEST) -> handleMapleAckRequest(msgJson);
            case (MsgType.JUICE_FILES_MSG) -> handleJuiceFilesMsg(msgJson);
            case (MsgType.JUICE_ACK_REQUEST) -> handleJuiceAckRequest(msgJson);
        }
    }

    /*
     * called when receive request from master for mapling some part of the input
     */
    protected void handleMapleFileMsg(JSONObject msgJson) {
        System.out.println("Receive Maple File Msg: " + msgJson.toString());
        File dir = new File(FilePath.INTERMEDIATE_PATH);
        if (!dir.exists()) {
            System.out.println("Create Root directory for intermediate files: " + dir.mkdirs());
        }
        String sourceFileName = msgJson.getString(MsgKey.SOURCE_FILE);
        String splitFileName = msgJson.getString(MsgKey.FILE_TO_MAPLE);
        String mapleExe = msgJson.getString(MsgKey.MAPLE_EXE);
        this.mapleJuice = MapleJuiceFactory.create(mapleExe);
        if (this.mapleJuice == null) {
            return;
        }
        String localSplitFilePath = FilePath.INTERMEDIATE_PATH + splitFileName;
        String remoteSplitFilePath = getSplitFilePath(splitFileName);
        this.dataTransfer.receiveFile(
            localSplitFilePath,
            remoteSplitFilePath,
            MasterInfo.Master_IP_ADDRESS
        );
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
        String destPath = getLocalMapleOutputPath(
            intermediatePrefix,
            splitFileName
        );
        String destFileName = getMapleOutputFileName(
            intermediatePrefix,
            splitFileName
        );
        mapleJuice.writeMapleOutputToFile(destPath);
        Message mapleCompleteMsg = new MapleCompleteMsg(
            this.ipAddress,
            this.port,
            sourceFileName,
            destFileName,
            intermediatePrefix
        );
        this.dataTransfer.send(
            mapleCompleteMsg.toJSON(),
            MasterInfo.Master_IP_ADDRESS,
            MasterInfo.MASTER_PORT
        );
    }

    /*
     * called when receive master's request for ack in maple stage
     */
    protected void handleMapleAckRequest(JSONObject msgJson) {
        System.out.println("Receive Maple ACK Request: " + msgJson.toString());
        String sourceFile = msgJson.getString(MsgKey.SOURCE_FILE);
        String prefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        Message mapleAck = new MapleAck(
            sourceFile,
            prefix,
            this.ipAddress,
            this.port
        );
        this.dataTransfer.send(
            mapleAck.toJSON(),
            MasterInfo.Master_IP_ADDRESS,
            MasterInfo.MASTER_PORT
        );
    }

    /*
     * called when receive master's message for juicing some part of the input
     */
    protected void handleJuiceFilesMsg(JSONObject msgJson) {
        System.out.println("Receive Juice Files Msg: " + msgJson.toString());
        JSONArray filesToJuice = msgJson.getJSONArray(MsgKey.FILES_TO_JUICE);
        String intermediatePrefix = msgJson.getString(MsgKey.INTERMEDIATE_PREFIX);
        for (int i = 0; i < filesToJuice.length(); i++) {
            String fileName = filesToJuice.getString(i);
            this.dataTransfer.receiveFile(
                getJuiceInputLocalPath(fileName),
                getJuiceInputRemotePath(
                    intermediatePrefix,
                    fileName
                ),
                MasterInfo.Master_IP_ADDRESS
            );
        }
        String juiceExe = msgJson.getString(MsgKey.JUICE_EXE);
        this.mapleJuice = MapleJuiceFactory.create(juiceExe);
        if (this.mapleJuice == null) {
            return;
        }
        ExecutorService service = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(filesToJuice.length());
        for (int i = 0; i < filesToJuice.length(); i++) {
            service.execute(new JuiceFile(
                filesToJuice,
                i,
                latch
            ));
        }
        while (latch.getCount() > 0) {
            Thread.yield();
        }
        String juiceOutputFilePath = getJuiceOutputLocalPath(intermediatePrefix);
        this.mapleJuice.writeJuiceOutputToFile(juiceOutputFilePath);
        int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
        String destFile = msgJson.getString(MsgKey.DEST_FILE);
        Message juiceCompleteMsg = new JuiceCompleteMsg(
            this.ipAddress,
            this.port,
            juiceOutputFilePath,
            destFile,
            isDelete
        );
        this.dataTransfer.send(
            juiceCompleteMsg.toJSON(),
            MasterInfo.Master_IP_ADDRESS,
            MasterInfo.MASTER_PORT
        );
    }

    /*
     * called when receive master's request for ack at the juice stage
     */
    protected void handleJuiceAckRequest(JSONObject msgJson) {
        System.out.println("Receive Juice ACK Request: " + msgJson.toString());
        String destFile = msgJson.getString(MsgKey.DEST_FILE);
        int isDelete = msgJson.getInt(MsgKey.IS_DELETE);
        Message juiceAck = new JuiceAck(
            destFile,
            isDelete,
            this.ipAddress,
            this.port
        );
        this.dataTransfer.send(
            juiceAck.toJSON(),
            MasterInfo.Master_IP_ADDRESS,
            MasterInfo.MASTER_PORT
        );
        if (!this.ipAddress.equals(MasterInfo.Master_IP_ADDRESS) || this.port != MasterInfo.MASTER_PORT) {
            deleteDir(FilePath.INTERMEDIATE_PATH);
        }
    }

    protected void deleteDir(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            dir.delete();
            return;
        }
        for (File file : files) {
            deleteDir(file.getAbsolutePath());
        }
        dir.delete();
    }

    private String getSplitFilePath(String splitFileName) {
        String sb = FilePath.INTERMEDIATE_PATH +
            FilePath.SPLIT_DIRECTORY +
            splitFileName;
        return sb;
    }

    private String getLocalMapleOutputPath(
        String prefix,
        String splitFileName
    ) {
        String sb = FilePath.INTERMEDIATE_PATH +
            prefix +
            "_" +
            splitFileName +
            "_" +
            this.ipAddress +
            "_" +
            this.port;
        return sb;
    }

    private String getMapleOutputFileName(
        String prefix,
        String splitFileName
    ) {
        String sb = prefix +
            "_" +
            splitFileName +
            "_" +
            this.ipAddress +
            "_" +
            this.port;
        return sb;
    }

    private String getJuiceInputLocalPath(String fileName) {
        return FilePath.INTERMEDIATE_PATH + fileName;
    }

    private String getJuiceOutputLocalPath(String intermediatePrefix) {
        StringBuilder sb = new StringBuilder();
        String filePath = sb.append(FilePath.INTERMEDIATE_PATH)
            .append(this.ipAddress)
            .append("_")
            .append(this.port)
            .append("_")
            .append(intermediatePrefix)
            .append("_juice_out")
            .toString();
        System.out.println("Juice output path: " + filePath);
        File file = new File(filePath);
        if (file.exists()) {
            filePath = sb.append("_1").toString();
        }
        return filePath;
    }

    private String getJuiceInputRemotePath(
        String intermediatePrefix,
        String fileName
    ) {
        String sb = FilePath.ROOT +
            intermediatePrefix +
            "/" +
            fileName;
        return sb;
    }

    /*
     * turn bytes into string
     */
    protected String readBytes(
        byte[] packet,
        int length
    ) {
        if (packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) (packet[i]));
        }
        return sb.toString();
    }

    private class JuiceFile implements Runnable {
        private final JSONArray filesToJuice;
        private final int idx;
        private final CountDownLatch latch;

        JuiceFile(
            JSONArray filesToJuice,
            int idx,
            CountDownLatch latch
        ) {
            this.idx = idx;
            this.filesToJuice = filesToJuice;
            this.latch = latch;
        }

        @Override
        public void run() {
            String fileName = filesToJuice.getString(idx);
            String filePath = getJuiceInputLocalPath(fileName);
            mapleJuice.juice(filePath);
            latch.countDown();
        }
    }
}
