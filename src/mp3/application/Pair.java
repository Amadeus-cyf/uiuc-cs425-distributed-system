package mp3.application;

public class Pair<K, V> {
    public K key;
    public V val;

    public Pair(
        K key,
        V val
    ) {
        this.key = key;
        this.val = val;
    }
}
