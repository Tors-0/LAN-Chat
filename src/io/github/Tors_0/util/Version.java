package io.github.Tors_0.util;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Version {
    private static String version;
    private static String title;
    private static boolean versionPresent = false;

    static {
        try {
            Attributes manifest = new Manifest(Version.class.getResourceAsStream("/io/github/Tors_0/META-INF/MANIFEST.MF")).getMainAttributes();
            version = manifest.getValue("Implementation-Version");
            title = manifest.getValue("Implementation-Title");
            versionPresent = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getVersion() {
        return version;
    }
    public static String getTitle() {
        return title;
    }
    public static boolean isVersionPresent() {
        return versionPresent;
    }
}
