package io.github.Tors_0.util;

import java.io.IOException;
import java.util.jar.Manifest;

public class Version {
    public static final String VERSION;
    private static boolean versionPresent = false;

    static {
        try {
            Manifest manifest = new Manifest(Version.class.getResourceAsStream("/io/github/Tors_0/META-INF/MANIFEST.MF"));
            VERSION = manifest.getMainAttributes().getValue("Implementation-Version");
            versionPresent = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static boolean isVersionPresent() {
        return versionPresent;
    }
}
