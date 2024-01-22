package io.github.Tors_0.client;

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
                } catch (IOException ignored) {}
                if (msg != null && !msg.isEmpty()) {
                    Client.addText(msg);
                    if (!Client.frame.isActive()) {
                        // send a toast message
                        // new thread to avoid queueing toasts
                        String finalMsg = msg;
                        new Thread(() -> {
                            new Toast(finalMsg).display();
                        }).start();

                        // make a little noise :3
                        PlaySound.playNotifySound();
                    }
                    if ("Server closed".equals(msg)) {
                        Client.getConnectAction().actionPerformed(null);
                        break;
                    }
                }
            }
        }
    }
    private static class PlaySound {
        static AudioClip clip = Applet.newAudioClip(PlaySound.class.getResource("/io/github/Tors_0/client/sounds/notify.wav"));
        public static void playNotifySound() {
            clip.play();
        }
    }
}