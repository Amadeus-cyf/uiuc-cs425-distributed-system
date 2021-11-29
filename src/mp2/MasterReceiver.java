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

import static mp2.constant.MasterSdfsInfo.*;

public class MasterReceiver extends Receiver {
    private Map<String, Queue<JSONObject>> messageMap;
    private Map<String, Status> fileStatus;                      // the first of boolean array isReading, the second is isWriting
    private Map<String, Set<ServerInfo>> fileStorageInfo;
    private Map<ServerInfo, Set<String>> serverStorageInfo;      // all file names a server store
    private Map<String, Set<ServerInfo>> ackResponse;
    private Map<String, Integer> getReqNum;                     // record current processing get request number for each file
    public Set<ServerInfo> servers;
    private final int REPLICA_NUM = 4;
    private Set<ServerInfo> failServers;

    public MasterReceiver(String ipAddress, int port, DataTransfer socket) {
        super(ipAddress, port, socket);
        this.messageMap = new HashMap<>();
        this.fileStatus = new HashMap<>();
        this.fileStorageInfo = new HashMap<>();
        this.ackResponse = new HashMap<>();
        this.servers = new HashSet<>();
        this.serverStorageInfo = new HashMap<>();
        this.getReqNum = new HashMap<>();
        this.failServers = new HashSet<>();
        ExecutorService replicaThread = Executors.newSingleThreadExecutor();
        replicaThread.execute(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(6000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    replicateFile();
                }
            }
        });
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
        JSONObject msgJson = new JSONObject(msg);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        System.out.println("Master Receiver: Receive " + msgType);
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
            case(MsgType.PUT_NOTIFY):
                receivePutNotify(msgJson);
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
            case(MsgType.REPLICATE_NOTIFY):
                receiveReplicateNotify(msgJson);
                break;
            case(MsgType.LS_REQUEST):
                receiveLsRequest(msgJson);
                break;
            case(MsgType.STORE_REQUEST):
                receiveStoreRequest();
                break;
            case(MsgType.ERROR_RESPONSE):
                receiveErrorResponse(msgJson);
                break;
            case(MsgType.FP_REJOIN_MSG):
                receiveFPRejoinMsg(msgJson);
                break;
        }
    }

    /*
     * receive request for get, put and delete
     */
    private void receiveRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        System.out.println("MASTER RECEIVE: RECEIVE REQUEST " + msgType);
        Status currentStatus = fileStatus.get(fileName);
        if(currentStatus == null || !currentStatus.isWriting) {
            Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
            if(targetServers != null) {
                switch(msgType) {
                    case MsgType.PRE_PUT_REQUEST:
                        handlePrePutRequestWithTargetServers(msgJson, currentStatus, targetServers);
                        break;
                    case MsgType.PRE_DEL_REQUEST:
                        handlePreDelRequest(msgJson, currentStatus, targetServers);
                        break;
                    case MsgType.PRE_GET_REQUEST:
                        handlePreGetRequest(msgJson, currentStatus, targetServers);
                        break;
                    default:
                        System.out.println("invalid msg type");
                }
            } else {
                if(msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    handlePrePutRequestWithNoTargetServers(msgJson);
                } else {
                    handleFileNotFound(msgJson);
                }
            }
        } else {
            // the target file is updating, wait for write finish
            addRequestToQueue(fileName, msgJson);
        }
    }

    private void handlePrePutRequestWithTargetServers(JSONObject msgJson, Status currentStatus, Set<ServerInfo> targetServers) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        if(currentStatus != null && (currentStatus.isReading || currentStatus.isReplicating)) {
            addRequestToQueue(fileName, msgJson);
        } else {
            // return all servers storing the file
            fileStatus.put(fileName, new Status(false, true, false));
            String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
            PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, targetServers);
            this.dataTransfer.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
            System.out.println("Master: SEND PRE_PUT_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
        }
    }

    private void handlePreDelRequest(JSONObject msgJson, Status currentStatus, Set<ServerInfo> targetServers) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        if(currentStatus != null && currentStatus.isReading) {
            addRequestToQueue(fileName, msgJson);
        } else {
            fileStatus.put(fileName, new Status(false, true, false));
            PreDelResponse preDelResponse = new PreDelResponse(fileName, targetServers);
            this.dataTransfer.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
            System.out.println("Master: SEND PRE_DEL_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
        }
    }

    private void handlePreGetRequest(JSONObject msgJson, Status currentStatus, Set<ServerInfo> targetServers) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        if(currentStatus != null && currentStatus.isReading) {
            // avoid heavy read which causes write to be blocked forever
            addRequestToQueue(fileName, msgJson);
        } else {
            fileStatus.put(fileName, new Status(true, false, false));
            for(ServerInfo server : targetServers) {
                String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
                PreGetResponse preGetResponse = new PreGetResponse(fileName, localFileName, server.getIpAddress(), server.getPort());
                this.dataTransfer.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                break;
            }
            System.out.print("Master: SEND PRE_GET_RESPONSE BACK TO Server " + targetIpAddress + ":" + targetPort);
            getReqNum.put(fileName, 1);
        }
    }

    private void handlePrePutRequestWithNoTargetServers(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        // use hash to find server to store new files
        fileStatus.put(fileName, new Status(false, true, false));
        int serverIdx = (hash(fileName) % servers.size());
        System.out.println("server Idx: " + serverIdx);
        Set<ServerInfo> serversArranged = new HashSet<>();
        List<ServerInfo> serverInfos = new ArrayList<>(servers);
        if(serverIdx <= serverInfos.size() - REPLICA_NUM) {
            for(int i = serverIdx; i < serverIdx + REPLICA_NUM; i++) {
                serversArranged.add(serverInfos.get(i));
            }
        } else {
            for(int i = serverIdx; i < servers.size(); i++) {
                serversArranged.add(serverInfos.get(i));
            }
            for(int i = serverIdx - 1; i >= 0; i--) {
                serversArranged.add(serverInfos.get(i));
                if(serversArranged.size() >= REPLICA_NUM) {
                    break;
                }
            }
        }
        for(ServerInfo serverInfo : serversArranged) {
            System.out.println("Master: ASSIGN FILE: " + fileName + " TO " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
        }
        String localFileName = msgJson.getString(MsgKey.LOCAL_FILE_NAME);
        PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, serversArranged);
        this.dataTransfer.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
        System.out.println("SEND PRE PUT RESPONSE TO " + targetIpAddress + ":" + targetPort);
    }

    private void handleFileNotFound(JSONObject msgJson) {
        // for GET and DELETE, FILE NOT FOUND
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        String targetIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int targetPort = msgJson.getInt(MsgKey.PORT);
        ErrorResponse errorResponse = new ErrorResponse(fileName);
        this.dataTransfer.send(errorResponse.toJSON(), targetIpAddress, targetPort);
    }

    /*
    * receive all ack responses
     */
    private void receiveACK (JSONObject msgJson){
        String fileName = msgJson.getString(MsgKey.SDFS_FILE_NAME);
        if(ackResponse.get(fileName) == null) {
            Set<ServerInfo> receivedAck = new HashSet<>();
            ackResponse.put(fileName, receivedAck);
        }
        String ipAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int port = msgJson.getInt(MsgKey.PORT);
        ackResponse.get(fileName).add(new ServerInfo(ipAddress, port));
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        System.out.println("Receive " + msgType + " ACK Response from Server " + ipAddress + ":" + port);
        int numGetReq = getReqNum.get(fileName) == null ? 0 : getReqNum.get(fileName);
        int currentAckNum = ackResponse.get(fileName).size();
        // check whether the ack number is enough
        System.out.println("CURRENT ACK NUM " + currentAckNum);
        if((!msgType.equals(MsgType.GET_ACK) || currentAckNum < numGetReq) && currentAckNum < Math.min(REPLICA_NUM, servers.size())) {
            return;
        }

        // receive enough GET ACK
        getReqNum.remove(fileName);
        Set<ServerInfo> serversAck = ackResponse.get(fileName);
        if(msgType.equals(MsgType.PUT_ACK)) {
            // either put success or replicate success
            handlePutAck(fileName, serversAck);
        } else if(msgType.equals(MsgType.DEL_ACK)) {
            handleDelAck(fileName, serversAck);
        }
        System.out.println(fileName + ": " + fileStorageInfo.get(fileName));
        ackResponse.remove(fileName);
        fileStatus.put(fileName, new Status(false, false, false));
        Queue<JSONObject> messageQueue = messageMap.get(fileName);
        if(messageQueue == null) {
            return;
        }
        label:
        while(true) {
            if(messageQueue.isEmpty()) {
                break;
            }
            JSONObject json = messageQueue.peek();
            String currentMsgType = json.getString(MsgKey.MSG_TYPE);
            System.out.println("receiveACK: " + json.toString());
            switch (currentMsgType) {
                case MsgType.PRE_GET_REQUEST:
                    handlePreGetAck(fileName);
                    break;
                case MsgType.PRE_PUT_REQUEST:
                    handlePrePutAck(fileName);
                    break label;
                case MsgType.PRE_DEL_REQUEST:
                    handlePreDelAck(fileName);
                    break label;
                case MsgType.REPLICATE_REQUEST:
                    handleReplicateAck(fileName);
                    break;
            }
        }
    }

    private void handlePreGetAck(String fileName) {
        Queue<JSONObject> messageQueue = messageMap.get(fileName);
        JSONObject json = messageQueue.peek();
        if(json == null) {
            return;
        }
        Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
        String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
        int targetPort = json.getInt(MsgKey.PORT);
        String sdfsFileName = json.getString(MsgKey.SDFS_FILE_NAME);
        fileStatus.put(fileName, new Status(true, false, false));
        messageQueue.poll();
        // send first server ip and port back to querying server
        for(ServerInfo server : targetServers) {
            String localFileName = json.getString(MsgKey.LOCAL_FILE_NAME);
            PreGetResponse preGetResponse = new PreGetResponse(sdfsFileName, localFileName, server.getIpAddress(), server.getPort());
            this.dataTransfer.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
            break;
        }
        if(getReqNum.get(fileName) == null) {
            getReqNum.put(fileName, 1);
        } else {
            getReqNum.put(fileName, getReqNum.get(fileName) + 1);
        }
        System.out.println("RECEIVE ACK : SEND PRE GET RESPONSE TO SERVER " + targetIpAddress + ":" + targetPort);
    }

    private void handlePrePutAck(String fileName) {
        Queue<JSONObject> messageQueue = messageMap.get(fileName);
        JSONObject json = messageQueue.peek();
        if(json == null) {
            return;
        }
        Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
        String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
        int targetPort = json.getInt(MsgKey.PORT);
        String sdfsFileName = json.getString(MsgKey.SDFS_FILE_NAME);
        // send all server ip and port back to the querying server
        Status currentStatus = fileStatus.get(fileName);
        if(currentStatus == null || !currentStatus.isReading) {
            messageQueue.poll();
            String localFileName = json.getString(MsgKey.LOCAL_FILE_NAME);
            PrePutResponse prePutResponse = new PrePutResponse(sdfsFileName, localFileName, targetServers);
            this.dataTransfer.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
            fileStatus.put(fileName, new Status(false, true, false));
            for(ServerInfo serverInfo : targetServers) {
                System.out.println("RECEIVE ACK: SEND PRE PUT RESPONSE TO " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
            }
        }
    }

    private void handlePreDelAck(String fileName) {
        Queue<JSONObject> messageQueue = messageMap.get(fileName);
        JSONObject json = messageQueue.peek();
        if(json == null) {
            return;
        }
        Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
        String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
        int targetPort = json.getInt(MsgKey.PORT);
        String sdfsFileName = json.getString(MsgKey.SDFS_FILE_NAME);
        Status currentStatus = fileStatus.get(fileName);
        if(currentStatus == null || !currentStatus.isReading) {
            messageQueue.poll();
            PreDelResponse preDelResponse = new PreDelResponse(sdfsFileName, targetServers);
            this.dataTransfer.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
            fileStatus.put(fileName, new Status(false, true, false));
            for(ServerInfo serverInfo : targetServers) {
                System.out.println("RECEIVE ACK: SEND PRE DEL RESPONSE TO Server " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
            }
        }
    }

    private void handleReplicateAck(String fileName) {
        Queue<JSONObject> messageQueue = messageMap.get(fileName);
        JSONObject json = messageQueue.peek();
        if(json == null) {
            return;
        }
        String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
        int targetPort = json.getInt(MsgKey.PORT);
        System.out.println("SEND REPLICATE REQUEST");
        messageQueue.poll();
        this.dataTransfer.send(json, targetIpAddress, targetPort);
        fileStatus.put(fileName, new Status(false, false, true));
        if (this.ackResponse.get(fileName) == null) {
            this.ackResponse.put(fileName, new HashSet<>());
        }
        // the replicate ack response will receive ack < REPLICA NUM, thus, we need to add fake ack into the ack response
        JSONArray newServers = json.getJSONArray(MsgKey.TARGET_SERVERS);
        for (int i = REPLICA_NUM; i > newServers.length(); i--) {
            this.ackResponse.get(fileName).add(new ServerInfo("", i * -1));
        }
    }

    private void receiveMembership (JSONObject msgJson){
        JSONArray serverList = msgJson.getJSONArray(MsgKey.MEMBERSHIP_LIST);
        for(int i = 0; i < serverList.length(); i++) {
            JSONObject server = serverList.getJSONObject(i);
            String ipAddress = server.getString(MsgKey.IP_ADDRESS);
            int port = server.getInt(MsgKey.PORT);
            ServerInfo serverInfo = new ServerInfo(ipAddress, port);
            if(!servers.contains(serverInfo)) {
                servers.add(serverInfo);
                this.serverStorageInfo.put(serverInfo, new HashSet<>());
            }
        }
    }

    private void receiveFail(JSONObject msgJson) {
        System.out.println("Receive Failure: " + msgJson.toString());
        String failIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int failPort = msgJson.getInt(MsgKey.PORT);
        ServerInfo failServerInfo = new ServerInfo(failIpAddress, failPort);
        if(servers.contains(failServerInfo) && !failServers.contains(failServerInfo)) {
            System.out.println("receiveFail: fail server " + failServerInfo.getIpAddress() + ":" + failServerInfo.getPort());
            servers.remove(failServerInfo);
            failServers.add(failServerInfo);
        }
    }

    /*
     * replicate all files stored on all current failed servers
     */
    private void replicateFile() {
        if(failServers == null || failServers.size() == 0) {
            return;
        }
        for(ServerInfo failServer : failServers) {
            System.out.println("REPLICATE FILE: Fail of " + failServer.getIpAddress() + ":" + failServer.getPort());
        }
        System.out.println("CURRENT SERVERS: " + servers);
        Map<String, Integer> fileReplicaNum = new HashMap<>();          // map file name to number of replica needed
        for(ServerInfo failServerInfo : failServers) {
            Set<String> fileNames = serverStorageInfo.get(failServerInfo);
            if(fileNames == null) {
                continue;
            }
            serverStorageInfo.remove(failServerInfo);
            // get number of replica need for each file stored on fail servers
            for(String fileName : fileNames) {
                if(fileReplicaNum.get(fileName) == null) {
                    fileReplicaNum.put(fileName, 1);
                } else {
                    fileReplicaNum.put(fileName, fileReplicaNum.get(fileName) + 1);
                }
            }
        }
        for(Map.Entry<String, Integer> entry : fileReplicaNum.entrySet()) {
            String fileName = entry.getKey();
            int replicaNum = entry.getValue();
            // remove fail server from file storage
            for(ServerInfo serverInfo : failServers) {
                fileStorageInfo.get(fileName).remove(serverInfo);
            }
            List<ServerInfo> serverStoreFile = new ArrayList<>(fileStorageInfo.get(fileName));
            // find new servers where replicas are assigned to
            Set<ServerInfo> assignedServers = new HashSet<>();
            while(assignedServers.size() < replicaNum) {
                assignedServers.add(getReplicaTarget(fileName));
            }
            // find the running server contains the file
            ServerInfo targetServer = null;
            if(serverStoreFile.size() > 0) {
                Random r = new Random();
                int randomIdx = r.nextInt(serverStoreFile.size());
                targetServer = serverStoreFile.get(randomIdx);
            }
            if(targetServer == null) {
                System.out.println("NO SERVER STORE THE FILE");
                return;
            }
            ReplicateRequest replicateRequest = new ReplicateRequest(fileName, assignedServers, targetServer.getIpAddress(), targetServer.getPort());
            System.out.println("replicate Request: " + replicateRequest.toJSON().toString());
            // check whether we could do replicate immediately
            if(fileStatus.get(fileName) == null || !(fileStatus.get(fileName).isWriting)) {
                // the file is currently not writing
                System.out.println("REPLICATE FILE: WRITE IS AVAILABLE " + fileName + " " + targetServer.getIpAddress() + ":" + targetServer.getPort());
                fileStatus.put(fileName, new Status(false,  false, true));
                this.dataTransfer.send(replicateRequest.toJSON(), targetServer.getIpAddress(), targetServer.getPort());
                this.ackResponse.computeIfAbsent(fileName, k -> new HashSet<>());
                // add fake ack response since number of replicate request is smaller than replica num
                for(int i = REPLICA_NUM; i > replicaNum; i--) {
                    this.ackResponse.get(fileName).add(new ServerInfo("", i * -1));
                }
            } else {
                // the file is currently writing
                // add fake write ack response of those failed servers
                System.out.println("REPLICATE FILE: WRITE IS NOT AVAILABLE " + fileName);
                for(int i = 0; i < replicaNum; i++) {
                    ackResponse.get(fileName).add(new ServerInfo("", i));
                }
                // add the replicate request to message queue
                addRequestToQueue(fileName, replicateRequest.toJSON());
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
    private void addRequestToQueue(String fileName, JSONObject msgJson) {
        // the target file is updating, wait for write finish
        if(messageMap.get(fileName) == null) {
            Queue<JSONObject> queue = new LinkedList<>();
            messageMap.put(fileName, queue);
        }
        messageMap.get(fileName).add(msgJson);
    }

    private void handlePutAck(String fileName, Set<ServerInfo> serversAck) {
        fileStorageInfo.computeIfAbsent(fileName, k -> new HashSet<>());
        for(ServerInfo serverInfo : serversAck) {
            // check whether the ack is a fake ack
            if(serverInfo.getIpAddress().equals("")) {
                continue;
            }
            fileStorageInfo.get(fileName).add(serverInfo);
            serverStorageInfo.computeIfAbsent(serverInfo, k -> new HashSet<>());
            System.out.println("ReceiveAck PUT_ACK serverInfo: " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
            serverStorageInfo.get(serverInfo).add(fileName);
        }
    }

    private void handleDelAck(String fileName, Set<ServerInfo> serversAck) {
        fileStorageInfo.remove(fileName);
        for(ServerInfo serverInfo : serversAck) {
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
            System.out.println("get filename == null");
            Message errorMsg = new ErrorResponse(fileName);
            this.dataTransfer.send(errorMsg.toJSON(), targetIpAddress, targetPort);
        } else{
            Set<ServerInfo> servers = this.fileStorageInfo.get(fileName);
            if(targetIpAddress.equals(MASTER_SDFS_IP_ADDRESS) && targetPort== MASTER_SDFS_PORT){
                System.out.println("List all the servers stored the file " + fileName + ":");
                for(ServerInfo server : servers) {
                    String replicaIpAddress = server.getIpAddress();
                    int replicaPort = server.getPort();
                    System.out.println(replicaIpAddress + ":" + replicaPort);
                }
                return;
            }
            Message lsResponse = new LsResponse(servers, fileName);
            this.dataTransfer.send(lsResponse.toJSON(), targetIpAddress, targetPort);
        }
    }

    private ServerInfo getReplicaTarget(String fileName){
        Random r = new Random();
        int randomIdx = r.nextInt(servers.size());
        Set<ServerInfo> serversHasFile = fileStorageInfo.get(fileName);
        List<ServerInfo> serverList = new ArrayList<>(servers);
        while(serversHasFile.contains(serverList.get(randomIdx))) {
            randomIdx = r.nextInt(servers.size());
        }
        fileStorageInfo.get(fileName).add(serverList.get(randomIdx));
        return serverList.get(randomIdx);
    }

    private void receiveFPRejoinMsg(JSONObject msgJson) {
        String ipAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int port = msgJson.getInt(MsgKey.PORT);
        System.out.println("Receive rejoin request / false positive from server " + ipAddress + ":" + port);
        servers.add(new ServerInfo(ipAddress, port));
    }
}