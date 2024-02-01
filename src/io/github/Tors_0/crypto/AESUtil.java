package io.github.Tors_0.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;

public class AESUtil {
    public static final String STANDARD_SALT = String.valueOf(AESUtil.class);
    public static final String ALG = "AES/CBC/PKCS5Padding";
    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }
    public static SecretKey getKeyFromPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");
    }
    public static SecretKey getStandardKeyFromPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return getKeyFromPassword(password, STANDARD_SALT);
    }
    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
    public static String encrypt(String input, SecretKey key, IvParameterSpec iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
                .encodeToString(cipherText);
    }
    public static String decrypt(String cipherText, SecretKey key, IvParameterSpec iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
                .decode(cipherText));
        return new String(plainText);
    }

    /**
     * prepends the iv as the first 16 bytes of the data
     * @param input string to be encrypted
     * @param key secret key for encryption
     * @param iv 16 byte seed
     * @return encrypted message as string
     * @throws InvalidKeyException if key is invalid
     */
    public static String encryptOutgoing(String input, SecretKey key, IvParameterSpec iv)
            throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        byte[] output = new byte[iv.getIV().length + cipherText.length];
        short pos = 0;
        for (byte b : iv.getIV()) {
            output[pos] = b;
            pos++;
        }
        for (byte b : cipherText) {
            output[pos] = b;
            pos++;
        }
        return Base64.getEncoder().encodeToString(output);
    }
    public static String decryptIncoming(String data, SecretKey key)
            throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        // get the data out of base 64
        byte[] dataArr = Base64.getDecoder().decode(data);
        // assume first 16 bytes of incoming data are the iv
        IvParameterSpec iv = new IvParameterSpec(dataArr, 0, 16);
        byte[] cipherText = Arrays.copyOfRange(dataArr, 16, dataArr.length);

        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText);
    }
}
