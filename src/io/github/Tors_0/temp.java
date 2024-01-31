package io.github.Tors_0;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;

public class temp {
    public static void main(String[] args) {
        send();
        receive();
    }

    private static void send() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("DES");
            kg.init(new SecureRandom());
            SecretKey key = kg.generateKey();
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            Class spec = Class.forName("javax.crypto.spec.DESKeySpec");
            DESKeySpec ks = (DESKeySpec) skf.getKeySpec(key, spec);
            ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(Paths.get("keyfile")));
            oos.writeObject(ks.getKey());

            Cipher c = Cipher.getInstance("DES/CFB8/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key);
            CipherOutputStream cos = new CipherOutputStream(
                    Files.newOutputStream(Paths.get("ciphertext")), c);
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(cos));
            pw.println("Stand and unfold yourself");
            pw.close();
            oos.writeObject(c.getIV());
            oos.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void receive() {
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    Files.newInputStream(Paths.get("keyfile")));
            DESKeySpec ks = new DESKeySpec((byte[]) ois.readObject());
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            SecretKey key = skf.generateSecret(ks);

            Cipher c = Cipher.getInstance("DES/CFB8/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key,
                    new IvParameterSpec((byte[]) ois.readObject()));
            CipherInputStream cis = new CipherInputStream(
                    Files.newInputStream(Paths.get("ciphertext")), c);
//            cis.read(new byte[8]);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(cis));
            System.out.println("Got message");
            System.out.println(br.readLine());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
