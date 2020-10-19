package mp2;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Master extends BaseServer {
    private Map<String, Queue<JSONObject>> messageQueue;
    private Map<String, Boolean> fileStatus;
    private Map<String, List<String>> fileStorageInfo;


}
