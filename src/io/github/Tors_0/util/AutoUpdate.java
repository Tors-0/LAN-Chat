package io.github.Tors_0.util;

import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;

public class AutoUpdate {
    public static void tryAutoUpdate() {
        if (0 != JOptionPane.showConfirmDialog(null,"Would you like to check for updates?", "Auto Updater", JOptionPane.YES_NO_OPTION)) {
            return;
        }
        if (Version.isVersionPresent()) {
            String version = Version.getVersion();
            try {
                URLConnection connection = new URL("https://github.com/Tors-0/LAN-Chat/releases/latest").openConnection();
                connection.connect();
                connection.getInputStream();
                URL repoUrl = connection.getURL();
                String[] pathSects = repoUrl.getPath().split("/");
                if (version.compareTo(pathSects[pathSects.length-1]) != 0) {
                    System.out.println("github version newer than local version");
                    if (0 == JOptionPane.showConfirmDialog(null, "Newer release available, download?","Auto Updater", JOptionPane.YES_NO_OPTION)) {
                        // download the updated jar file
                        String fileName = Version.getTitle() + "-" + pathSects[pathSects.length-1] + ".jar";
                        BufferedInputStream repoNewFile = new BufferedInputStream(new URL(repoUrl.toString() + "/" + fileName).openStream());
                        Path path = Paths.get(Paths.get(AutoUpdate.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString() + File.separator + fileName);
//                        FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
//
//                        byte dataBuffer[] = new byte[1024];
//                        int bytesRead;
//                        while ((bytesRead = repoNewFile.read(dataBuffer, 0, 1024)) != -1) {
//                            fileOutputStream.write(dataBuffer, 0, bytesRead);
//                        }


                        System.out.println("got " + Files.copy(repoNewFile,path, StandardCopyOption.REPLACE_EXISTING) + " bytes...");

                        repoNewFile.close();
//                        fileOutputStream.close();
                        JOptionPane.showMessageDialog(null,"Downloaded to '" + path + "'","Auto Updater",JOptionPane.INFORMATION_MESSAGE);
                        startNewJar(path);
                    }
                }
            } catch (IOException | URISyntaxException e) {
                // notify user if network/github/etc not available
                JOptionPane.showMessageDialog(null,e.toString(),"Update Failed", JOptionPane.WARNING_MESSAGE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void startNewJar(Path path) throws IOException, InterruptedException {
        if (!path.toFile().setExecutable(true)) {
            return;
        }
        Process process = Runtime.getRuntime().exec("java -jar " + path);
        process.waitFor();
        // get process output
        InputStream in = process.getInputStream();
        InputStream err = process.getErrorStream();

        byte b[]=new byte[in.available()];
        in.read(b,0,b.length);
        System.out.println(new String(b));

        byte c[]=new byte[err.available()];
        err.read(c,0,c.length);
        System.err.println(new String(c));
        System.exit(0);
    }
}
