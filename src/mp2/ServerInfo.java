package mp2;

public class ServerInfo {
    private final String ipAddress;
    private final int port;

    public ServerInfo(
        String ipAddress,
        int port
    ) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ServerInfo)) {
            return false;
        }
        return this.ipAddress.equals(((ServerInfo) obj).ipAddress) && (this.port == ((ServerInfo) obj).port);
    }

    @Override
    public int hashCode() {
        return this.ipAddress.hashCode() + 31 * this.port;
    }
}
