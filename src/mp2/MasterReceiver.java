package mp2;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.model.PreDelResponse;
import mp2.model.PreGetResponse;
import mp2.model.PrePutResponse;
import mp2.model.ServerInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class MasterReceiver extends Receiver {
    private Map<String, Queue<JSONObject>> messageMap;
    private Map<String, Boolean> fileStatus;
    private Map<String, Set<ServerInfo>> fileStorageInfo;
    private Map<String, Set<ServerInfo>> ackResponse;
    private Set<ServerInfo> servers;
    private final int REPLICA_NUM = 4;
    private final String PUT = "PUT";
    private final String DELETE = "DELETE";

    public MasterReceiver(String ipAddress, int port, UdpSocket socket, Map<String, Queue<JSONObject>> messageMap,
                          Map<String, Boolean> fileStatus, Map<String, Set<ServerInfo>> fileStorageInfo) {
        super(ipAddress, port, socket);
        this.messageMap = messageMap;
        this.fileStatus = fileStatus;
        this.fileStorageInfo = fileStorageInfo;
        this.ackResponse = new HashMap<>();
        this.servers = new HashSet<>();
    }

    /*
     * receive request for get, put and delete
     */
    public void receiveRequest(JSONObject jsonObject) {
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
        System.out.println("MASTER: RECEIVE " + msgType);
        Boolean isAccessing = fileStatus.get(fileName);
        if (isAccessing == null || !isAccessing) {
            Set<ServerInfo> targetServers = fileStorageInfo.get(fileName);
            String targetIpAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
            int targetPort = jsonObject.getInt(MsgKey.PORT);
            if (targetServers != null) {
                if (msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    // return all servers storing the file
                    fileStatus.put(fileName, true);
                    String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                    PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, targetServers);
                    this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                    System.out.println("Master: SEND PRE_PUT_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                } else if (msgType.equals(MsgType.PRE_DEL_REQUEST)) {
                    fileStatus.put(fileName, true);
                    PreDelResponse preDelResponse = new PreDelResponse(fileName,targetServers);
                    this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                    System.out.println("Master: SEND PRE_DEL_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                } else if (msgType.equals(MsgType.PRE_GET_REQUEST)) {
                    for (ServerInfo server : targetServers) {
                        String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                        PreGetResponse preGetResponse = new PreGetResponse(fileName, localFileName, server.getIpAddress(), server.getPort());
                        this.socket.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                        break;
                    }
                    System.out.println("Master: SEND PRE_GET_RESPONSE BACK TO " + targetIpAddress + ":" + targetPort);
                }
            } else {
                if (msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    // use hash to find server to store new files
                    int serverIdx = (int)(hash(fileName) % servers.size());
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
                        for (int i = serverIdx-1; i >= 0; i--) {
                            serversArranged.add(serverInfos.get(i));
                            if (serversArranged.size() >= REPLICA_NUM) {
                                break;
                            }
                        }
                    }
                    for (ServerInfo serverInfo : serversArranged) {
                        System.out.println("Master: ASSIGN FILE: " + fileName + " TO "  + serverInfo.getIpAddress() + ":" + serverInfo.getPort());
                    }
                    fileStorageInfo.put(fileName, serversArranged);
                    String localFileName = jsonObject.getString(MsgKey.LOCAL_FILE_NAME);
                    PrePutResponse prePutResponse = new PrePutResponse(fileName, localFileName, servers);
                    this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                    System.out.println("SEND PRE PUT RESPONSE TO " + targetIpAddress + ":" + targetPort);
                }
            }
        } else {
            // the target file is updating, wait for write finish
            if (messageMap.get(fileName) == null) {
                Queue<JSONObject> queue = new LinkedList<>();
                messageMap.put(fileName, queue);
            }
            messageMap.get(fileName).add(jsonObject);
        }
    }

    public void receiveACK(JSONObject jsonObject) {
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        if (ackResponse.get(fileName) == null) {
            Set<ServerInfo> receivedAck = new HashSet<>();
            ackResponse.put(fileName, receivedAck);
        }
        String ipAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
        int port = jsonObject.getInt(MsgKey.PORT);
        ackResponse.get(fileName).add(new ServerInfo(ipAddress, port));
        if (ackResponse.get(fileName).size() >= REPLICA_NUM) {
            Set<ServerInfo> serversAck = ackResponse.get(fileName);
            String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
            if (msgType.equals(MsgType.PUT_ACK)) {
                if (fileStorageInfo.get(fileName) == null) {
                    Set<ServerInfo> servers = new HashSet<>(serversAck);
                    fileStorageInfo.put(fileName, servers);
                }
            } else if (msgType.equals(MsgType.DEL_ACK)) {
                fileStorageInfo.remove(fileName);
            }
            ackResponse.remove(fileName);
            fileStatus.put(fileName, false);
            Queue<JSONObject> messageQueue = messageMap.get(fileName);
            if (messageQueue != null) {
                boolean isFirstMessage = true;
                while (true) {
                    if (messageQueue.isEmpty()) {
                        break;
                    }
                    JSONObject json = messageQueue.peek();
                    String currentMsgType = json.getString(MsgKey.MSG_TYPE);
                    Set<ServerInfo> servers = fileStorageInfo.get(fileName);
                    String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
                    String sdfsFileName = json.getString(MsgKey.SDFS_FILE_NAME);
                    int targetPort = json.getInt(MsgKey.PORT);
                    if (currentMsgType.equals(MsgType.PRE_GET_REQUEST)) {
                        isFirstMessage = false;
                        messageQueue.poll();
                        // send first server ip and port back to querying server
                        for (ServerInfo server : servers) {
                            String localFileName = json.getString(MsgKey.LOCAL_FILE_NAME);
                            PreGetResponse preGetResponse = new PreGetResponse(sdfsFileName, localFileName, server.getIpAddress(), server.getPort());
                            this.socket.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                            break;
                        }
                    } else if (currentMsgType.equals(MsgType.PRE_PUT_REQUEST)) {
                        // send all server ip and port back to the querying server
                        fileStatus.put(fileName, true);
                        if (isFirstMessage) {
                            messageQueue.poll();
                            String localFileName = json.getString(MsgKey.LOCAL_FILE_NAME);
                            PrePutResponse prePutResponse = new PrePutResponse(sdfsFileName, localFileName, servers);
                            this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                        }
                        break;
                    } else if (currentMsgType.equals(MsgType.PRE_DEL_REQUEST)) {
                        fileStatus.put(fileName, true);
                        if (isFirstMessage) {
                            messageQueue.poll();
                            PreDelResponse preDelResponse = new PreDelResponse(sdfsFileName, servers);
                            this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void receiveMembership(JSONObject jsonObject) {
        JSONArray serverList = jsonObject.getJSONArray(MsgKey.MEMBERSHIP_LIST);
        for (int i = 0; i < serverList.length(); i++) {
            JSONObject server = serverList.getJSONObject(i);
            String ipAddress = server.getString("ipAddress");
            int port = server.getInt("port");
            ServerInfo serverInfo = new ServerInfo(ipAddress, port);
            if (!servers.contains(serverInfo)) {
                servers.add(serverInfo);
            }
        }
    }


    /*public void receiveFail(JSONObject jsonObject) {
        String ipAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
        int port = jsonObject.getInt(MsgKey.PORT);
        for (Map.Entry<String, Set<ServerInfo>> storage : fileStorageInfo.entrySet()) {
            for (ServerInfo serverInfo : storage.getValue()) {
                if (serverInfo.ipAddress.equals(ipAddress) && serverInfo.port == port) {

                }
            }
        }
    }*/

    private long hash(String fileName) {
        long hash = 0;
        for (int i = 0; i < fileName.length(); i++) {
            hash = 31 * (hash + fileName.charAt(i));
        }
        return hash;
    }
}
