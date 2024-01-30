package io.github.Tors_0.util;

import java.io.PrintWriter;

/**
 * provide utils for encoding and decoding packets to differentiate between message packets and server util packets
 * @author Rae Johnston
 */
public class NetDataUtil {
    public static final String ONLINE_REQUEST = "onlineUsersRequest";
    public static final String ONLINE_RESPONSE = "onlineUsersResponse";
    public static void sendInfoRequest(PrintWriter destination, String data) {
        destination.println(Identifier.INFO_REQUEST.getKeyString() + data);
    }
    public static void sendInfoResponse(PrintWriter destination, String data) {
        destination.println(Identifier.INFO_RESPONSE.getKeyString() + data);
    }
    public static void sendMessage(PrintWriter destination, String message) {
        destination.println(Identifier.MESSAGE.getKeyString() + message);
    }
    public enum Identifier {
        MESSAGE("CLIENT MSG"),
        INFO_REQUEST("INFO REQ"),
        INFO_RESPONSE("INFO RESP");

        private final byte[] key; // length 10
        private final String keyString;
        private Identifier(String key) {
            while (key.length() < 10) {
                key += " ";
            }
            this.keyString = key;
            this.key = key.getBytes();
        }
        public byte[] getKey() {
            return key;
        }
        public String getKeyString() {
            return keyString;
        }
    }
}
