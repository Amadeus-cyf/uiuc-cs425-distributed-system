package mp2;

public class Status {
    boolean isReading;
    boolean isWriting;
    boolean isReplicating;

    public Status(
        boolean isReading,
        boolean isWriting,
        boolean isReplicating
    ) {
        this.isReading = isReading;
        this.isWriting = isWriting;
        this.isReplicating = isReplicating;
    }
}
