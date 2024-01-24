package io.github.Tors_0.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Code taken from {com.sun.javafx.PlatformUtil.java}
 */
public class SystemInfo {
    private static final String os = System.getProperty("os.name");
    private static final String version = System.getProperty("os.version");
    private static final boolean embedded;
    private static final String embeddedType;
    private static final boolean useEGL;
    private static final boolean doEGLCompositing;
    // a property used to denote a non-default impl for this host
    private static String javafxPlatform;

    static {
        javafxPlatform = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("javafx.platform"));
        embedded = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("com.sun.javafx.isEmbedded"));
        embeddedType = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("embedded"));
        useEGL = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("use.egl"));
        if (useEGL) {
            doEGLCompositing = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("doNativeComposite"));
        } else
            doEGLCompositing = false;
    }

    private static final boolean ANDROID = "android".equals(javafxPlatform) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean WINDOWS = os.startsWith("Windows");
    private static final boolean WINDOWS_VISTA_OR_LATER = WINDOWS && versionNumberGreaterThanOrEqualTo(6.0f);
    private static final boolean WINDOWS_7_OR_LATER = WINDOWS && versionNumberGreaterThanOrEqualTo(6.1f);
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux") && !ANDROID;
    private static final boolean SOLARIS = os.startsWith("SunOS");
    private static final boolean IOS = os.startsWith("iOS");

    private static boolean versionNumberGreaterThanOrEqualTo(float value) {
        try {
            return Float.parseFloat(version) >= value;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the operating system is a form of Windows.
     */
    public static boolean isWindows(){
        return WINDOWS;
    }

    /**
     * Returns true if the operating system is at least Windows Vista(v6.0).
     */
    public static boolean isWinVistaOrLater(){
        return WINDOWS_VISTA_OR_LATER;
    }

    /**
     * Returns true if the operating system is at least Windows 7(v6.1).
     */
    public static boolean isWin7OrLater(){
        return WINDOWS_7_OR_LATER;
    }

    /**
     * Returns true if the operating system is a form of Mac OS.
     */
    public static boolean isMac(){
        return MAC;
    }

    /**
     * Returns true if the operating system is a form of Linux.
     */
    public static boolean isLinux(){
        return LINUX;
    }

    public static boolean useEGL() {
        return useEGL;
    }

    public static boolean useEGLWindowComposition() {
        return doEGLCompositing;
    }

    public static boolean useGLES2() {
        String useGles2 = "false";
        useGles2 =
                AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("use.gles2"));
        if ("true".equals(useGles2))
            return true;
        else
            return false;
    }

    /**
     * Returns true if the operating system is a form of Unix, including Linux.
     */
    public static boolean isSolaris(){
        return SOLARIS;
    }

    /**
     * Returns true if the operating system is a form of Linux or Solaris
     */
    public static boolean isUnix(){
        return LINUX || SOLARIS;
    }

    /**
     * Returns true if the platform is embedded.
     */
    public static boolean isEmbedded() {
        return embedded;
    }

    /**
     * Returns a string with the embedded type - ie eglx11, eglfb, dfb or null.
     */
    public static String getEmbeddedType() {
        return embeddedType;
    }

    /**
     * Returns true if the operating system is iOS
     */
    public static boolean isIOS(){
        return IOS;
    }
}
