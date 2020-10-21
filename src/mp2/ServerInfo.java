package mp2;

public class ServerInfo {
    private String ipAddress;
    private int port;

    public ServerInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }
}
