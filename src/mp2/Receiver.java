package mp2;

import mp2.model.GetResponse;
import mp2.model.Message;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Receiver {
    private String ipAddress;
    private int port;
    private boolean isMaster;
    private List<File> files;
    private UdpSocket socket;
    private byte[] buffer = new byte[2048];

    public Receiver(String ipAddress, int port, boolean isMaster, UdpSocket socket) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isMaster = isMaster;
        this.socket = socket;
        this.files = new ArrayList<>();
        if (this.port == 3000) {
            File file = new File("test.txt");
            try {
                FileOutputStream o = new FileOutputStream(file);
                String s = "hello world";
                o.write(s.getBytes());
                o.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
            files.add(file);
        }
    }

    public void start() {
        while (true) {
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            this.socket.receive(receivedPacket);
            String msg = readBytes(buffer, receivedPacket.getLength());
            receive(msg);
        }
    }

    public void receive(String msg) {
        JSONObject msgJson  = new JSONObject(msg);
        String msgType = msgJson.getString(MsgKey.MSG_TYPE);
        switch(msgType) {
            case(MsgType.GET_REQUEST):
                receiveGetRequest(msgJson);
                break;
            case(MsgType.GET_RESPONSE):
                receiveGetResponse(msgJson);
                break;
        }
    }

    public void receiveGetRequest(JSONObject msgJson) {
        String fileName = msgJson.getString(MsgKey.FILE_NAME);
        String senderIpAddress = msgJson.getString(MsgKey.IP_ADDRESS);
        int senderPort = msgJson.getInt(MsgKey.PORT);
        File target = null;
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                target = file;
                break;
            }
        }
        Message response = new GetResponse(target);
        this.socket.send(response.toJSON(), senderIpAddress, senderPort);
    }

    public void receiveGetResponse(JSONObject msgJson) {
        if (msgJson.get(MsgKey.FILE) != null && msgJson.get(MsgKey.FILE).equals(MsgContent.NO_FILE_FOUND)) {
            System.out.println("No such file");
            return;
        }
        byte[] bytes = Base64.getDecoder().decode(msgJson.getString(MsgKey.FILE));
        File file = new File("test1.txt");
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(bytes);
            out.close();
            files.add(file);
            for (File f : files) {
                System.out.println(f.getName());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * turn bytes into string
     */
    private String readBytes(byte[] packet, int length) {
        if (packet == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char)(packet[i]));
        }
        return sb.toString();
    }
}
