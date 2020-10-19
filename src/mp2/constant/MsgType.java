package mp2.constant;

public class MsgType {
    public static final String GET_REQUEST = "GET_REQUEST";
    public static final String GET_RESPONSE = "GET_RESPONSE";
    public static final String PUT_REQUEST = "PUT_REQUEST";
    public static final String PUT_RESPONSE = "PUT_RESPONSE";
    public static final String DEL_REQUEST = "DEL_REQUEST";
    public static final String ERROR_RESPONSE = "ERROR_RESPONSE";

    // PRE_REQUSET are messages sent to master for file storing info
    public static final String PRE_GET_REQUEST = "PRE_GET_REQUEST";
    public static final String PRE_PUT_REQUEST = "PRE_PUT_REQUEST";
    public static final String PRE_DEL_REQUEST = "PRE_DEL_REQUEST";
    // PRE_RESPONSE are messages master respond to the querying server.
    public static final String PRE_GET_RESPONSE = "PRE_GET_RESPONSE";
    public static final String PRE_PUT_RESPONSE = "PRE_PUT_RESPONSE";
    public static final String PRE_DEL_RESPONSE = "PRE_DEL_RESPONSE";

    // ACK messages are messages send back to master when get, put, delete completes.
    public static final String GET_ACK = "GET_ACK";
    public static final String PUT_ACK = "PUT_ACK";
    public static final String DEL_ACK = "DEL_ACK";
}
