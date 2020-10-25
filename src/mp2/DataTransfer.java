package mp2;

import mp2.constant.UserInfo;
import org.json.JSONObject;

import java.net.*;

public class DataTransfer {
    private DatagramSocket socket;
    private String ipAddress;
    private int port;

    public DataTransfer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        bind();
    }

    /*
     * bind the socket to the ip address and port
     */
    public void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            this.socket = new DatagramSocket(port, address);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * send message to the target ip address and port
     */
    public void send(JSONObject msg, String targetIpAddress, int targetPort) {
        if (msg == null) {
            return;
        }
        byte[] buffer = msg.toString().getBytes();
        try {
            InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, targetPort);
            this.socket.send(packet);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    /*
     * send local file to remote server, update the sdfs file. Used for PUT
     */
    public int sendFile(String localFile, String sdfsFile, String ipAddress) {
        System.out.println("send file");
        int idx = ipAddress.indexOf(".cs.illinois.edu");
        String hostName = ipAddress;
        if (idx >= 0) {
            hostName = ipAddress.substring(0, idx);
        }
        System.out.println("HostName " + hostName);
        StringBuilder sb = new StringBuilder();
        sb.append("scp ").append(localFile).append(" ").append(UserInfo.username).append("@").append(hostName).append(":").append(sdfsFile);
        String command = sb.toString();
        return executeCommand(command);
    }

    public void receive(DatagramPacket receivedPacket) {
        try {
            this.socket.receive(receivedPacket);
        } catch(Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * download sdfs file from remote servers into the local file. Used for GET
     */
    public int receiveFile(String localFile, String sdfsFile, String ipAddress) {
        System.out.println("receive file");
        String hostName = ipAddress;
        int idx = ipAddress.indexOf(".cs.illinois.edu");
        if (idx >= 0) {
            hostName = ipAddress.substring(0, idx);
        }
        System.out.println("HOSTNAME " + hostName);
        StringBuilder sb = new StringBuilder();
        sb.append("scp ").append(UserInfo.username).append("@").append(hostName).append(":").append(sdfsFile).append(" ").append(localFile);
        String command = sb.toString();
        return executeCommand(command);
    }

    private int executeCommand(String command) {
        System.out.println(command);
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            int result = process.exitValue();
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
