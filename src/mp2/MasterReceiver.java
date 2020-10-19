package mp2;

import mp2.constant.MsgKey;
import mp2.constant.MsgType;
import mp2.model.PreDelResponse;
import mp2.model.PreGetResponse;
import mp2.model.PrePutResponse;
import mp2.model.ServerInfo;
import org.json.JSONObject;

import java.util.*;

public class MasterReceiver extends Receiver {
    private Map<String, Queue<JSONObject>> messageMap;
    private Map<String, Boolean> fileStatus;
    private Map<String, Set<ServerInfo>> fileStorageInfo;
    private Map<String, Set<ServerInfo>> writeAckResponse;
    private final int REPLICA_NUM = 4;
    private final String PUT = "PUT";
    private final String DELETE = "DELETE";

    public MasterReceiver(String ipAddress, int port, UdpSocket socket, Map<String, Queue<JSONObject>> messageMap,
                          Map<String, Boolean> fileStatus, Map<String, Set<ServerInfo>> fileStorageInfo) {
        super(ipAddress, port, socket);
        this.messageMap = messageMap;
        this.fileStatus = fileStatus;
        this.fileStorageInfo = fileStorageInfo;
        this.writeAckResponse = new HashMap<>();
    }

    /*
     * receive request for get, put and delete
     */
    public void receiveRequest(JSONObject jsonObject) {
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
        Boolean isWriting = fileStatus.get(fileName);
        if (isWriting == null || !isWriting) {
            Set<ServerInfo> servers = fileStorageInfo.get(fileName);
            if (servers != null) {
                String targetIpAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
                int targetPort = jsonObject.getInt(MsgKey.PORT);
                if (msgType.equals(MsgType.PRE_PUT_REQUEST)) {
                    // return all servers storing the file
                    fileStatus.put(fileName, true);
                    PrePutResponse prePutResponse = new PrePutResponse(servers);
                    this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                } else if (msgType.equals(MsgType.PRE_DEL_REQUEST)) {
                    fileStatus.put(fileName, true);
                    PreDelResponse preDelResponse = new PreDelResponse(servers);
                    this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                } else if (msgType.equals(MsgType.PRE_GET_REQUEST)) {
                    for (ServerInfo server : servers) {
                        PreGetResponse preGetResponse = new PreGetResponse(server.getIpAddress(), server.getPort());
                        this.socket.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                        break;
                    }
                }
            } else {

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

    public void receivePutDeleteACK(JSONObject jsonObject) {
        String fileName = jsonObject.getString(MsgKey.SDFS_FILE_NAME);
        if (writeAckResponse.get(fileName) == null) {
            Set<ServerInfo> receivedAck = new HashSet<>();
            writeAckResponse.put(fileName, receivedAck);
        }
        String ipAddress = jsonObject.getString(MsgKey.IP_ADDRESS);
        int port = jsonObject.getInt(MsgKey.PORT);
        writeAckResponse.get(fileName).add(new ServerInfo(ipAddress, port));
        if (writeAckResponse.get(fileName).size() >= REPLICA_NUM) {
            Set<ServerInfo> serversAck = writeAckResponse.get(fileName);
            String msgType = jsonObject.getString(MsgKey.MSG_TYPE);
            if (msgType.equals(MsgType.PUT_ACK)) {
                if (fileStorageInfo.get(fileName) == null) {
                    Set<ServerInfo> servers = new HashSet<>(serversAck);
                    fileStorageInfo.put(fileName, servers);
                }
            } else if (msgType.equals(MsgType.DEL_ACK)) {
                fileStorageInfo.remove(fileName);
            } else {
                return;
            }
            writeAckResponse.remove(fileName);
            fileStatus.put(fileName, false);
            Queue<JSONObject> messageQueue = messageMap.get(fileName);
            if (messageQueue != null) {
                while (true) {
                    if (messageQueue.isEmpty()) {
                        break;
                    }
                    JSONObject json = messageQueue.poll();
                    String currentMsgType = json.getString(MsgKey.MSG_TYPE);
                    Set<ServerInfo> servers = fileStorageInfo.get(fileName);
                    String targetIpAddress = json.getString(MsgKey.IP_ADDRESS);
                    int targetPort = json.getInt(MsgKey.PORT);
                    if (currentMsgType.equals(MsgType.PRE_GET_REQUEST)) {
                        // send first server ip and port back to querying server
                        for (ServerInfo server : servers) {
                            PreGetResponse preGetResponse = new PreGetResponse(server.getIpAddress(), server.getPort());
                            this.socket.send(preGetResponse.toJSON(), targetIpAddress, targetPort);
                            break;
                        }
                    } else if (currentMsgType.equals(MsgType.PRE_PUT_REQUEST)) {
                        // send all server ip and port back to the querying server
                        fileStatus.put(fileName, true);
                        PrePutResponse prePutResponse = new PrePutResponse(servers);
                        this.socket.send(prePutResponse.toJSON(), targetIpAddress, targetPort);
                        break;
                    } else if (currentMsgType.equals(MsgType.PRE_DEL_REQUEST)) {
                        fileStatus.put(fileName, true);
                        PreDelResponse preDelResponse = new PreDelResponse(servers);
                        this.socket.send(preDelResponse.toJSON(), targetIpAddress, targetPort);
                        break;
                    }
                }
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
}
