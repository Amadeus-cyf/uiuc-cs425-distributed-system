package mp2;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static mp2.constant.MasterInfo.*;

public class MasterReceiver extends Receiver {
    private Logger logger = Logger.getLogger(MasterReceiver.class.getName());
    private Map<String, Queue<JSONObject>> messageMap;
    private Map<String, Status> fileStatus;                      // the first of boolean array isReading, the second is isWriting
    private Map<String, Set<ServerInfo>> fileStorageInfo;
    private Map<ServerInfo, Set<String>> serverStorageInfo;      // all file names a server store
    private Map<String, Set<ServerInfo>> ackResponse;
    private Map<String, Integer> getReqNum;                     // record current processing get request number for each file
    private Set<ServerInfo> servers;
    private final int REPLICA_NUM = 4;
    private Set<ServerInfo> failServers;

    public MasterReceiver(String ipAddress, int port, UdpSocket socket) {
        super(ipAddress, port, socket);
        this.messageMap = new HashMap<>();
        this.fileStatus = new HashMap<>();
        this.fileStorageInfo = new HashMap<>();
        this.ackResponse = new HashMap<>();
        this.servers = new HashSet<>();
        this.serverStorageInfo = new HashMap<>();
        this.getReqNum = new HashMap<>();
        this.failServers = new HashSet<>();
    }

    public void start() {
        ExecutorService replicaThread = Executors.newSingleThreadExecutor();
        replicaThread.execute(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    replicateFile();
                }
            }
        });
        while (true) {
            byte[] buffer = new byte[BLOCK_SIZE * 2];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.socket.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            receive(msg);
        }
}

    private void receive(String msg) {
        JSONObject msgJson = new JSONObject(msg);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        logger.info("Master Receiver: Receive " + msgType);
        switch(msgType) {
            case(MsgType.PRE_GET_REQUEST):
            case(MsgType.PRE_PUT_REQUEST):
            case(MsgType.PRE_DEL_REQUEST):
                receiveRequest(msgJson);
                break;
            case(MsgType.PRE_GET_RESPONSE):
                receivePreGetResponse(msgJson);
                break;
            case(MsgType.PRE_PUT_RESPONSE):
                receivePrePutResponse(msgJson);
                break;
            case(MsgType.PRE_DEL_RESPONSE):
                receivePreDelResponse(msgJson);
                break;
            case(MsgType.PUT_ACK):
            case(MsgType.GET_ACK):
            case(MsgType.DEL_ACK):
                receiveACK(msgJson);
                break;
            case(MsgType.GET_REQUEST):
                receiveGetRequest(msgJson);
                break;
            case(MsgType.GET_RESPONSE):
                receiveGetResponse(msgJson);
                break;
            case(MsgType.PUT_REQUEST):
                receivePutRequest(msgJson);
                break;
            case(MsgType.DEL_REQUEST):
                receiveDeleteRequest(msgJson);
                break;
            case(MsgType.JOIN_REQUEST):
                receiveMembership(msgJson);
                break;
            case(MsgType.SERVER_FAIL):
                receiveFail(msgJson);
                break;
            case(MsgType.REPLICATE_REQUEST):
                receiveReplicateRequest(msgJson);
                break;
            case(MsgType.LS_REQUEST):
                receiveLsRequest(msgJson);
                break;
            case(MsgType.STORE_REQUEST):
                receiveStoreRequest();
                break;
            case(MsgType.ERROR_RESPONSE):
                receiveErrorResponse();
                break;
        }
    }

    /*
     * receive request for get, put and delete
     */
    private void receiveRequest(JSONObject jsonObject){
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
        logger.info("MASTER RECEIVE: RECEIVE REQUEST " + msgType);
        Status currentStatus = fileStatus.get(fileName);
        if (currentStatus == null || !currentStatus.isWriting) {
            Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
            String targetIpAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
            int targetPort = jsonObject.getInt(MsgKey.PORT);
            if (targetServers != null) {
                if (msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    if (currentStatus != null && currentStatus.isReading) {
                        addRequestToQueue(fileName, jsonObject);
                    } else {
                        // return all servers storing the file
                        fileStatus.put(fileName, new Status(false, true));
                        String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                        PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, targetServers);
                        this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                        logger.info("Master: SEND PRE_PUT_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                    }
                } else if (msgType.equals(MsgType.PRE_DEL_REQUEST)) {
                    if (currentStatus != null && currentStatus.isReading) {
                        addRequestToQueue(fileName, jsonObject);
                    } else {
                        fileStatus.put(fileName, new Status(false, true));
                        PreDelResponse preDelResponse = new PreDelResponse(fileName, targetServers);
                        this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                        logger.info("Master: SEND PRE_DEL_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                    }
                } else if (msgType.equals(MsgType.PRE_GET_REQUEST)) {
                    if (currentStatus != null && currentStatus.isReading) {
                        // avoid heavy read which causes write to be blocked forever
                        addRequestToQueue(fileName, jsonObject);
                    } else {
                        fileStatus.put(fileName, new Status(true, false));
                        for (ServerInfo server : targetServers) {
                            String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                            PreGetResponse preGetResponse = new PreGetResponse(fileName, localFileName, server.getIpAddress(), server.getPort());
                            this.socket.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                            break;
                        }
                        logger.info("Master: SEND PRE_GET_RESPONSE BACK TO Server " + targetIpAddress + ":" + targetPort);
                        getReqNum.put(fileName, 1);
                    }
                }
            } else {
                if (msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    // use hash to find server to store new files
                    fileStatus.put(fileName, new Status(false, true));
                    int serverIdx = (hash(fileName) % servers.size());
                    logger.info("server Idx: " + serverIdx);
                    Set<ServerInfo> serversArranged = new HashSet<>();
                    List<ServerInfo> serverInfos = new ArrayList<>(servers);
                    if (serverIdx <= serverInfos.size() - REPLICA_NUM) {
                        for (int i = serverIdx; i < serverIdx + REPLICA_NUM; i++) {
                            serversArranged.add(serverInfos.get(i));
                        }
                    } else {
                        for (int i = serverIdx; i < servers.size(); i++) {
                            serversArranged.add(serverInfos.get(i));
                        }
                        for (int i = serverIdx - 1; i >= 0; i--) {
                            serversArranged.add(serverInfos.get(i));
                            if (serversArranged.size() >= REPLICA_NUM) {
                                break;
                            }
                        }
                    }
                    for (ServerInfo serverInfo : serversArranged) {
                        logger.info("Master: ASSIGN FILE: " + fileName + " TO " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
                    }
                    fileStorageInfo.put(fileName, serversArranged);
                    String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                    PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, serversArranged);
                    this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                    logger.info("SEND PRE PUT RESPONSE TO " + targetIpAddress + ":" + targetPort);
                }
            }
        } else {
            // the target file is updating, wait for write finish
            addRequestToQueue(fileName, jsonObject);
        }
    }

    /*
    * receive all ack responses
     */
    private void receiveACK (JSONObject jsonObject){
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        if (ackResponse.get(fileName) == null) {
            Set<ServerInfo> receivedAck = new HashSet<>();
            ackResponse.put(fileName, receivedAck);
        }
        String ipAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
        int port = jsonObject.getInt(MsgKey.PORT);
        ackResponse.get(fileName).add(new ServerInfo(ipAddress, port));
        String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
        logger.info("Receive" + msgType + " ACK Response from Server " + ipAddress + ":" + port);
        int numGetReq = getReqNum.get(fileName) == null ? 0 : getReqNum.get(fileName);
        int currentAckNum = ackResponse.get(fileName).size();
        // check whether the ack number is enough
        if ((msgType.equals(MsgType.GET_ACK) && currentAckNum >= numGetReq) || (currentAckNum >= Math.min(REPLICA_NUM, servers.size()))) {
            getReqNum.remove(fileName);
            Set<ServerInfo> serversAck = ackResponse.get(fileName);
            if (msgType.equals(MsgType.PUT_ACK)) {
                // either put success or replicate success
                handlePutAck(fileName, serversAck);
            } else if (msgType.equals(MsgType.DEL_ACK)) {
                handleDelAck(fileName, serversAck);
            }
            logger.info(fileName + ": " + fileStorageInfo.get(fileName));
            ackResponse.remove(fileName);
            fileStatus.put(fileName, new Status(false, false));
            Queue<JSONObject> messageQueue = messageMap.get(fileName);
            if (messageQueue != null) {
                while (true) {
                    if (messageQueue.isEmpty()) {
                        break;
                    }
                    JSONObject json = messageQueue.peek();
                    String currentMsgType = json.getString(MsgKey.MSG_TYPE);
                    Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
                    String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
                    String sdfsFileName = json.getString(MsgKey.SDFS_FILE_NAME);
                    int targetPort = json.getInt(MsgKey.PORT);
                    if (currentMsgType.equals(MsgType.PRE_GET_REQUEST)) {
                        fileStatus.put(fileName, new Status(true, false));
                        messageQueue.poll();
                        // send first server ip and port back to querying server
                        for (ServerInfo server : targetServers) {
                            String localFileName = json.getString(MsgKey.LOCAL_FILE_NAME);
                            PreGetResponse preGetResponse = new PreGetResponse(sdfsFileName, localFileName, server.getIpAddress(), server.getPort());
                            this.socket.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                            break;
                        }
                        if (getReqNum.get(fileName) == null) {
                            getReqNum.put(fileName, 1);
                        } else {
                            getReqNum.put(fileName, getReqNum.get(fileName) + 1);
                        }
                        logger.info("RECEIVE ACK : SEND PRE GET RESPONSE TO SERVER " + targetIpAddress + ":" + targetPort);
                    } else if (currentMsgType.equals(MsgType.PRE_PUT_REQUEST)) {
                        // send all server ip and port back to the querying server
                        Status currentStatus = fileStatus.get(fileName);
                        if (currentStatus == null || !currentStatus.isReading) {
                            messageQueue.poll();
                            String localFileName = json.getString(MsgKey.LOCAL_FILE_NAME);
                            PrePutResponse prePutResponse = new PrePutResponse(sdfsFileName, localFileName, targetServers);
                            this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                            fileStatus.put(fileName, new Status(false, true));
                            for (ServerInfo serverInfo : targetServers) {
                                logger.info("RECEIVE ACK: SEND PRE PUT RESPONSE TO " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
                            }
                        }
                        break;
                    } else if (currentMsgType.equals(MsgType.PRE_DEL_REQUEST)) {
                        Status currentStatus = fileStatus.get(fileName);
                        if (currentStatus == null || !currentStatus.isReading) {
                            messageQueue.poll();
                            PreDelResponse preDelResponse = new PreDelResponse(sdfsFileName, targetServers);
                            this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                            fileStatus.put(fileName, new Status(false, true));
                            for (ServerInfo serverInfo : targetServers) {
                                logger.info("RECEIVE ACK: SEND PRE DEL RESPONSE TO Server " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
                            }
                        }
                        break;
                    } else if (currentMsgType.equals(MsgType.REPLICATE_REQUEST)) {
                        this.socket.send(json, targetIpAddress, targetPort);
                        fileStatus.put(fileName, new Status(false, true));
                        if (this.ackResponse.get(fileName) == null) {
                            this.ackResponse.put(fileName, new HashSet<>());
                        }
                        // the replicate ack response will receive ack < REPLICA NUM, thus, we need to add fake ack into the ack response
                        JSONArray newServers = json.getJSONArray(MsgKey.TARGET_SERVERS);
                        for (int i = REPLICA_NUM; i > newServers.length(); i--) {
                            this.ackResponse.get(fileName).add(new ServerInfo("", i * -1));
                        }
                    }
                }
            }
        }
    }

    private void receiveMembership (JSONObject jsonObject){
        JSONArray serverList = jsonObject.getJSONArray(MsgKey.MEMBERSHIP_LIST);
        for (int i = 0; i < serverList.length(); i++) {
            JSONObject server = serverList.getJSONObject(i);
            String ipAddress = server.getString(MsgKey.IP_ADDRESS);
            int port = server.getInt(MsgKey.PORT);
            ServerInfo serverInfo = new ServerInfo(ipAddress, port);
            logger.info("receive membershipList - loop serverList: " + ipAddress + port);
            if (!servers.contains(serverInfo)) {
                servers.add(serverInfo);
                this.serverStorageInfo.put(serverInfo, new HashSet<>());
            }
        }
    }

    private void receiveFail(JSONObject jsonObject) {
        logger.info("Receive Failure: " + jsonObject.toString());
        failServers.add(new ServerInfo(jsonObject.getString(MsgKey.IP_ADDRESS), jsonObject.getInt(MsgKey.PORT)));
    }

    /*
    * replicate all files stored on all current failed servers
     */
    private void replicateFile() {
        if (failServers == null || failServers.size() == 0) {
            return;
        }
        for (ServerInfo failServer : failServers) {
            System.out.println("REPLICATE FILE: Fail of " + failServer.getIpAddress() + ":" + failServer.getPort());
        }
        Map<String, Integer> fileReplicaNum = new HashMap<>();          // map file name to number of replica needed
        for (ServerInfo failServerInfo : failServers) {
            Set<String> fileNames = serverStorageInfo.get(failServerInfo);
            serverStorageInfo.remove(failServerInfo);
            servers.remove(failServerInfo);
            for (String fileName : fileNames) {
                if (fileReplicaNum.get(fileName) == null) {
                    fileReplicaNum.put(fileName, 1);
                } else {
                    fileReplicaNum.put(fileName, fileReplicaNum.get(fileName) + 1);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : fileReplicaNum.entrySet()) {
            String fileName = entry.getKey();
            int replicaNum = entry.getValue();
            int temp = replicaNum;
            Set<ServerInfo> serverStoreFile = fileStorageInfo.get(fileName);
            // find new servers where replicas are assigned to
            Set<ServerInfo> assignedServers = new HashSet<>();
            for (ServerInfo server : servers) {
                if (failServers.contains(server)) {
                    fileStorageInfo.get(fileName).remove(server);
                    continue;
                }
                if (!serverStoreFile.contains(fileName)) {
                    assignedServers.add(server);
                    temp--;
                }
                if (temp <= 0) {
                    break;
                }
            }
            // find the running server contains the file
            ServerInfo targetServer = null;
            for (ServerInfo serverInfo : serverStoreFile) {
                if (!failServers.contains(serverInfo)) {
                    targetServer = serverInfo;
                    break;
                }
            }
            if (targetServer == null) {
                logger.info("NO SERVER STORE THE FILE");
                return;
            }
            ReplicateRequest replicateRequest = new ReplicateRequest(fileName, servers);
            // check whether we could do replicate immediately
            if (fileStatus.get(fileName) == null || !(fileStatus.get(fileName).isWriting)) {
                // the file is currently not writing
                System.out.println("REPLICATE FILE: WRITE IS AVAILABLE");
                fileStatus.put(fileName, new Status(false, true));
                this.socket.send(replicateRequest.toJSON(), targetServer.getIpAddress(), targetServer.getPort());
                if (this.ackResponse.get(fileName) == null) {
                    this.ackResponse.put(fileName, new HashSet<>());
                }
                // add fake ack response since number of replicate request is smaller than replica num
                for (int i = REPLICA_NUM; i > replicaNum; i--) {
                    this.ackResponse.get(fileName).add(new ServerInfo("", i * -1));
                }
            } else {
                // the file is currently writing
                // add fake write ack response of those failed servers
                System.out.println("REPLICATE FILE: WRITE IS NOT AVAILABLE");
                for (int i = 0; i < replicaNum; i++) {
                    ackResponse.get(fileName).add(new ServerInfo("", i));
                }
                // add the replicate request to message queue
                addRequestToQueue(MsgType.REPLICATE_REQUEST, replicateRequest.toJSON());
            }
        }
        failServers.clear();
    }

    private int hash (String fileName){
        return Math.abs(fileName.hashCode());
    }

    /*
     * add request to message queue in order to prevent simultaneous read or write
     */
    private void addRequestToQueue(String fileName, JSONObject jsonObject) {
        // the target file is updating, wait for write finish
        if (messageMap.get(fileName) == null) {
            Queue<JSONObject> queue = new LinkedList<>();
            messageMap.put(fileName, queue);
        }
        messageMap.get(fileName).add(jsonObject);
    }

    private void handlePutAck(String fileName, Set<ServerInfo> serversAck) {
        if (fileStorageInfo.get(fileName) == null) {
            fileStorageInfo.put(fileName, new HashSet<>());
        }
        for (ServerInfo serverInfo : serversAck) {
            // check whether the ack is a fake ack
            if (!serverInfo.getIpAddress().equals("")) {
                fileStorageInfo.get(fileName).add(serverInfo);
                if (serverStorageInfo.get(serverInfo) == null) {
                    serverStorageInfo.put(serverInfo, new HashSet<>());
                }
                logger.info("ReceiveAck PUT_ACK serverInfo: " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
                serverStorageInfo.get(serverInfo).add(fileName);
            }
        }
    }

    private void handleDelAck(String fileName, Set<ServerInfo> serversAck) {
        fileStorageInfo.remove(fileName);
        for (ServerInfo serverInfo : serversAck) {
            serverStorageInfo.get(serverInfo).remove(fileName);
        }
    }

    /*
     * master receive the ls request from the query server
     */
    private void receiveLsRequest(JSONObject msgJson) {
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        System.out.println("ls filename:" + fileName);
        // check if the target server is the master
        if(this.fileStorageInfo.get(fileName) == null){
            logger.info("get filename == null");
            Message errorMsg = new ErrorResponse(fileName);
            this.socket.send(errorMsg.toJSON(), targetIpAddress, targetPort);
        }else{
            Set<ServerInfo> servers = this.fileStorageInfo.get(fileName);
            if(targetIpAddress.equals(MASTER_IP_ADDRESS) && targetPort==MASTER_PORT){
                System.out.println("List all the servers stored the file " + fileName + ":");
                for (ServerInfo server : servers) {
                    String replicaIpAddress = server.getIpAddress();
                    int replicaPort = server.getPort();
                    System.out.println(replicaIpAddress + ":" + replicaPort);
                }
                return;
            }
            Message lsResponse = new LsResponse(servers, fileName);
            this.socket.send(lsResponse.toJSON(), targetIpAddress, targetPort);
        }
    }
}