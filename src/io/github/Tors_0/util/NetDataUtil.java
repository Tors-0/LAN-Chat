package io.github.Tors_0.util;

import io.github.Tors_0.client.Client;
import io.github.Tors_0.crypto.AESUtil;
import io.github.Tors_0.crypto.CryptoInactiveException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.PrintWriter;

/**
 * provide utils for encoding and decoding packets to differentiate between message packets and server util packets
 * @author Rae Johnston
 */
public class NetDataUtil {
    public static final String ONLINE_REQUEST = "onlineUsersRequest";
    public static final String ONLINE_RESPONSE = "onlineUsersResponse";
    public static final String PASSWORD_WRONG = "passwordFail";
    public static final String PASSWORD_RIGHT = "passwordGood";
    public static void sendInfoRequest(PrintWriter destination, String data, SecretKey key, IvParameterSpec iv, boolean cryptoActive) {
        data = Identifier.INFO_REQUEST.getKeyString() + data;
        if (cryptoActive) {
            try {
                data = AESUtil.encryptOutgoing(data, key, iv);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(new CryptoInactiveException());
        }
        destination.println(data);
    }
    public static void sendInfoResponse(PrintWriter destination, String data, SecretKey key, IvParameterSpec iv, boolean cryptoActive) {
        data = Identifier.INFO_RESPONSE.getKeyString() + data;
        if (cryptoActive) {
            try {
                data = AESUtil.encryptOutgoing(data, key, iv);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(new CryptoInactiveException());
        }
        destination.println(data);
    }
    public static void sendMessage(PrintWriter destination, String message, SecretKey key, IvParameterSpec iv, boolean cryptoActive) {
        message = Identifier.MESSAGE.getKeyString() + message;
        if (cryptoActive) {
            try {
                message = AESUtil.encryptOutgoing(message, key, iv);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(new CryptoInactiveException());
        }
        destination.println(message);
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
