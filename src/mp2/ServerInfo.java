package mp2;

public record ServerInfo(String ipAddress, int port) {
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
}
