package mp2;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.util.*;

import static mp2.constant.MasterInfo.*;

public class MasterReceiver extends Receiver {
    private Map<String, Queue<JSONObject>> messageMap;
    private Map<String, Status> fileStatus;                      // the first of boolean array isReading, the second is isWriting
    private Map<String, Set<ServerInfo>> fileStorageInfo;
    private Map<ServerInfo, Set<String>> serverStorageInfo;      // all file names a server store
    private Map<String, Set<ServerInfo>> ackResponse;
    private Map<String, Integer> getReqNum;                     // record current processing get request number for each file
    private Set<ServerInfo> servers;
    private final int REPLICA_NUM = 4;

    public MasterReceiver(String ipAddress, int port, UdpSocket socket) {
        super(ipAddress, port, socket);
        this.messageMap = messageMap;
        this.fileStatus = fileStatus;
        this.fileStorageInfo = fileStorageInfo;
        this.messageMap = new HashMap<>();
        this.fileStatus = new HashMap<>();
        this.fileStorageInfo = new HashMap<>();
        this.ackResponse = new HashMap<>();
        this.servers = new HashSet<>();
        this.serverStorageInfo = new HashMap<>();
        this.getReqNum = new HashMap<>();
    }

    public void start() {
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
        System.out.println("receive " + msgType);
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
        }
    }

    /*
     * receive request for get, put and delete
     */
    private void receiveRequest(JSONObject jsonObject){
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
        System.out.println("MASTER: RECEIVE " + msgType);
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
                        System.out.println("Master: SEND PRE_PUT_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                    }
                } else if (msgType.equals(MsgType.PRE_DEL_REQUEST)) {
                    if (currentStatus != null && currentStatus.isReading) {
                        addRequestToQueue(fileName, jsonObject);
                    } else {
                        fileStatus.put(fileName, new Status(false, true));
                        PreDelResponse preDelResponse = new PreDelResponse(fileName, targetServers);
                        this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                        System.out.println("Master: SEND PRE_DEL_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
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
                        System.out.println("Master: SEND PRE_GET_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                        getReqNum.put(fileName, 1);
                    }
                }
            } else {
                if (msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    // use hash to find server to store new files
                    fileStatus.put(fileName, new Status(false, true));
                    int serverIdx = (hash(fileName) % servers.size());
                    System.out.println(hash(fileName) + " " + servers.size());
                    System.out.println("server Idx: " + serverIdx);
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
                        System.out.println("Master: ASSIGN FILE: " + fileName + " TO " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
                    }
                    fileStorageInfo.put(fileName, serversArranged);
                    String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                    PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, serversArranged);
                    this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                    System.out.println("SEND PRE PUT RESPONSE TO " + targetIpAddress + ":" + targetPort);
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
        System.out.println(ipAddress + ":" + port);
        System.out.println(ackResponse.get(fileName));
        String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
        int numGetReq = getReqNum.get(fileName) == null ? 0 : getReqNum.get(fileName);
        int currentAckNum = ackResponse.get(fileName).size();
        if ((msgType.equals(MsgType.GET_ACK) && currentAckNum >= numGetReq) || (currentAckNum >= Math.min(REPLICA_NUM, servers.size()))) {
            System.out.println(ackResponse.get(fileName).toString());
            Set<ServerInfo> serversAck = ackResponse.get(fileName);
            if (msgType.equals(MsgType.PUT_ACK)) {
                // either put success or replicate success
                handlePutAck(fileName, serversAck);
            } else if (msgType.equals(MsgType.DEL_ACK)) {
                handleDelAck(fileName, serversAck);
            }
            System.out.println(fileName + ": " + fileStorageInfo.get(fileName));
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
                        System.out.println("RECEIVE ACK : SEND PRE GET RESPONSE TO SERVER " + targetIpAddress + ":" + targetPort);
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
                                System.out.println("RECEIVE ACK: SEND PRE PUT RESPONSE TO " + serverInfo.getIpAddress() + " : " + serverInfo.getPort());
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
                                System.out.println("RECEIVE ACK: SEND PRE DEL RESPONSE TO " + targetServers);
                            }
                        }
                        break;
                    } else if (currentMsgType.equals(MsgType.REPLICATE_REQUEST)) {
                        this.socket.send(json, targetIpAddress, targetPort);
                        fileStatus.put(fileName, new Status(false, true));
                        if (this.ackResponse.get(fileName) == null) {
                            this.ackResponse.put(fileName, new HashSet<>());
                        }
                        // the replicate ack response will only receive 1 ack, thus, we need to add 3 more fake ack into the ack response
                        this.ackResponse.get(fileName).add(new ServerInfo("", -1));
                        this.ackResponse.get(fileName).add(new ServerInfo("", -2));
                        this.ackResponse.get(fileName).add(new ServerInfo("", -3));
                    }
                }
            }
        }
    }

    private void receiveMembership (JSONObject jsonObject){
        JSONArray serverList = jsonObject.getJSONArray(MsgKey.MEMBERSHIP_LIST);
        for (int i = 0; i < serverList.length(); i++) {
            JSONObject server = serverList.getJSONObject(i);
            String ipAddress = server.getString("ipAddress");
            int port = server.getInt("port");
            ServerInfo serverInfo = new ServerInfo(ipAddress, port);
            System.out.println("receive membershipList - loop serverList: " + ipAddress + port);
            if (!servers.contains(serverInfo)) {
                servers.add(serverInfo);
                this.serverStorageInfo.put(serverInfo, new HashSet<>());
            }
        }
    }

    /*
    * receive fail message of some server, and send replicate request to servers containing data stored on the failed server
     */
    private void receiveFail(JSONObject jsonObject) {
        String ipAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
        int port = jsonObject.getInt(MsgKey.PORT);
        ServerInfo failServerInfo = new ServerInfo(ipAddress, port);
        if (servers.contains(failServerInfo)) {
            servers.remove(failServerInfo);
        } else {
            return;
        }
        System.out.println("receiveFail: " + jsonObject.toString());
        Set<String> fileNames = serverStorageInfo.get(failServerInfo);
        serverStorageInfo.remove(failServerInfo);
        if (fileNames != null) {
            System.out.println("receiveFail: " + fileNames);
            for (String fileName : fileNames) {
                fileStorageInfo.get(fileName).remove(failServerInfo);
                List<ServerInfo> serverStoreFile = new ArrayList<>(fileStorageInfo.get(fileName));

                if (serverStoreFile.size() > 0) {
                    System.out.println("receiveFail: " + serverStoreFile.size());
                    for (ServerInfo server : servers) {
                        if (!serverStoreFile.contains(server)) {
                            System.out.println("receiveFail: " + !serverStoreFile.contains(server));
//                            fileStorageInfo.get(fileName).add(server);
//                            serverStorageInfo.get(server).add(fileName);
                            ReplicateRequest replicateRequest = new ReplicateRequest(fileName, server.getIpAddress(), server.getPort());
//                            System.out.println("receiveFail: fileName" + serverStoreFile.get(0).getIpAddress() + serverStoreFile.get(0).getPort());
                            if (fileStatus.get(fileName) == null || !fileStatus.get(fileName).isWriting) {
                                System.out.println("receiveFail: not writing file" );
                                fileStatus.put(fileName, new Status(false, true));
                                System.out.println("send replicate request to :" + serverStoreFile.get(0).getIpAddress() + serverStoreFile.get(0).getPort() + replicateRequest.toJSON());
                                this.socket.send(replicateRequest.toJSON(), serverStoreFile.get(0).getIpAddress(), serverStoreFile.get(0).getPort());
                                // the replicate ack response will only receive 1 ack, thus, we need to add 3 more fake ack into the ack response
                                if (this.ackResponse.get(fileName) == null) {
                                    this.ackResponse.put(fileName, new HashSet<>());
                                }
                                this.ackResponse.get(fileName).add(new ServerInfo("", -1));
                                this.ackResponse.get(fileName).add(new ServerInfo("", -2));
                                this.ackResponse.get(fileName).add(new ServerInfo("", -3));
                            } else {
                                System.out.println("receiveFail: " + "add fail server ack response to ack response to make ack number >= replica number");
                                // add fail server ack response to ack response to make ack number >= replica number
                                ackResponse.get(fileName).add(failServerInfo);
                                addRequestToQueue(MsgType.PRE_PUT_REQUEST, replicateRequest.toJSON());
                            }
                            break;
                        }
                    }
                } else {
                    System.out.println("REPLICATE FOR FAILURE: NO SERVER STORE THE FILE");
                }
            }
        } else {
            System.out.println("REPLICATE FOR FAILURE: THERE IS NO SUCH SERVER");
        }
    }

    private int hash (String fileName){
        return Math.abs(fileName.hashCode());
    }

    /*
     * whether a server is the master
     */
    private Boolean isMaster(ServerInfo serverInfo) {
        return serverInfo.getIpAddress().equals(MASTER_IP_ADDRESS) && serverInfo.getPort() == MASTER_PORT;
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
        if (fileStorageInfo.get(fileName) == null || fileStorageInfo.get(fileName).size() == 0) {
            fileStorageInfo.put(fileName, serversAck);
        } else {
            // handle replica case
            List<ServerInfo> updatedServers = new ArrayList<>(serversAck);
            boolean isReplicaAck = false;
            for (ServerInfo updatedServer : updatedServers) {
                if (updatedServer.getPort() < 0) {
                    isReplicaAck = true;
                    break;
                }
            }
            if (isReplicaAck) {
                for (ServerInfo updatedServer : updatedServers) {
                    if (updatedServer.getPort() > 0) {
                        fileStorageInfo.get(fileName).add(updatedServer);
                        break;
                    }
                }
            }
        }
        for (ServerInfo serverInfo : serversAck) {
            if (serverInfo.getPort() < 0) {
                continue;
            }
            if (serverStorageInfo.get(serverInfo) == null) {
                serverStorageInfo.put(serverInfo, new HashSet<>());
            }
            System.out.println("ReceiveAck PUT_ACK serverInfo: " + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
            serverStorageInfo.get(serverInfo).add(fileName);
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
        // check if the target server is the master
        if(this.fileStorageInfo.get(fileName) == null){
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