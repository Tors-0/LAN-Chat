package io.github.Tors_0.util;

import java.io.IOException;
import java.util.jar.Manifest;

public class Version {
    private static String version;
    private static boolean versionPresent = false;

    static {
        try {
            Manifest manifest = new Manifest(Version.class.getResourceAsStream("/io/github/Tors_0/META-INF/MANIFEST.MF"));
            version = manifest.getMainAttributes().getValue("Implementation-Version");
            versionPresent = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getVersion() {
        return version;
    }
    public static boolean isVersionPresent() {
        return versionPresent;
    }
}
