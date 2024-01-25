package io.github.Tors_0.client;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
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
    public void sendMsg(String msg) {
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
            JFrameToast toast = new JFrameToast();
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
                        if (Client.IS_LINUX) {
                            // new thread to avoid queueing toasts
                            String finalMsg = msg;
                            new Thread(() -> {
                                toast.display(finalMsg);
                            }).start();

                            // make a little noise :3
                            PlaySound.playNotifySound();
                        } else {
                            // for Windows and Mac, we use the native notifications :o
                            SysTrayToast.display(msg);
                            // don't make a little noise because Windows has one already :(
                        }
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
    public static class PlaySound {
        private static boolean muted = false;
        public static void setMuted(boolean value) {
            muted = value;
        }
        /**
         * source: <a href="https://soundcloud.com/sescini/melodic-1">Melodic 1 - SoundCloud</a>
         */
        static AudioClip clip = Applet.newAudioClip(PlaySound.class.getResource("/io/github/Tors_0/client/resources/melodic.wav"));
        public static void playNotifySound() {
            if (!muted) {
                clip.play();
            }
        }
    }
}
