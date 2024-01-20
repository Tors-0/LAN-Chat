package client;

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
        worker.interrupt();
        worker = null;
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
                    if ("Server closed...".equals(msg)) {
                        Client.addText("Exiting program in 5s...");
                        long startTime = System.currentTimeMillis();
                        startTime += 5000;
                        while (startTime < System.currentTimeMillis()) {}
                        System.exit(0);
                    }
                }
            }
        }
    }
}
