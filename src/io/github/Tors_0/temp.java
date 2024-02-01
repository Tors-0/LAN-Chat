package io.github.Tors_0;

import io.github.Tors_0.crypto.AESUtil;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

public class temp {
    public static void main(String[] args) {
        try {
            givenString_whenEncrypt_thenSuccess();
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | NoSuchPaddingException |
                 NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
    static void givenString_whenEncrypt_thenSuccess()
            throws NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {

        String password = "testy";
        String input = new Random().ints(97, 122 + 1)
                .limit(64)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        System.out.println("input:\n" + input);
        SecretKey key = AESUtil.getKeyFromPassword(password, AESUtil.STANDARD_SALT);
        IvParameterSpec ivParameterSpec = AESUtil.generateIv();
        String cipherText = AESUtil.encrypt(input, key, ivParameterSpec);
        System.out.println("encrypted:\n" + cipherText);
//        key = AESUtil.getStandardKeyFromPassword(password);
        String plainText = AESUtil.decrypt(cipherText, key, ivParameterSpec);
        System.out.println("plaintext:\n" + plainText);
        System.out.println("success? >> " + input.equals(plainText));
    }
}
