package io.github.Tors_0.client;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Networking implements Closeable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread worker;

    public Networking() {
    }
    public void startConnection(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        worker = new PrintInputStream(in);
        worker.start();
    }
    public void sendMsg(String msg) throws IOException {
        out.println(msg);
    }
    @Override
    public void close() throws IOException {
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        socket.close();
        out.close();
        in.close();
    }
    private static class PrintInputStream extends Thread {
        BufferedReader in;
        public PrintInputStream(BufferedReader in) {
            this.in = in;
        }
        public void run() {
            String msg = "";
            while (msg != null) {
                if (interrupted()) break;
                try {
                    msg = in.readLine();
                } catch (IOException ignored) {
                    msg = null;
                }
                if (msg != null && !msg.isEmpty()) {
                    Client.addText(msg);
                    if (!Client.frame.isActive()) {
                        // send a toast message
                        if (!Client.isWindows) {
                            // new thread to avoid queueing toasts
                            String finalMsg = msg;
                            new Thread(() -> {
                                new Toast(finalMsg).display();
                            }).start();
                        } else {
                            try {
                                SysTrayToast.display(msg);
                            } catch (AWTException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        // make a little noise :3
                        PlaySound.playNotifySound();
                    }
                    if ("Server closed".equals(msg)) {
                        Client.showAlertMessage("Server stopped by host","Disconnected", JOptionPane.INFORMATION_MESSAGE);
                        Client.getConnectAction().actionPerformed(null);
                        break;
                    }
                }
            }
        }
    }
    private static class PlaySound {
        /**
         * source: <a href="https://soundcloud.com/sescini/melodic-1">Melodic 1 - SoundCloud</a>
         */
        static AudioClip clip = Applet.newAudioClip(PlaySound.class.getResource("/io/github/Tors_0/client/resources/melodic.wav"));
        public static void playNotifySound() {
            clip.play();
        }
    }
}
