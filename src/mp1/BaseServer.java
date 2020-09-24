package mp1;

import mp1.model.Member;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.List;

public abstract class BaseServer {
    protected String id;
    protected DatagramSocket socket;
    protected String ipAddress;
    protected int port;
    protected Timestamp startingTime;
    protected List<Member> membershipList;

    /*
     * bind the socket to the ip address and port
     */
    protected void bind() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            this.socket = new DatagramSocket(port, address);
        } catch (SocketException exception) {
            // TODO: Log the exception
        } catch (UnknownHostException exception) {
            // TODO: Log the exception
        }
    }


    protected String createId() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.ipAddress);
        sb.append("_");
        sb.append(this.port);
        sb.append("_");
        sb.append(this.startingTime.toString());
        return sb.toString();
    }
}
