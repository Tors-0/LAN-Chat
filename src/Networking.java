import java.io.*;
import java.net.Socket;

public class Networking implements Closeable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public Networking() {
    }
    public void startConnection(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new PrintInputStream(in).start();
    }
    public void sendMsg(String msg) throws IOException {
        out.println(msg);
    }
    public BufferedReader getReader() {
        return in;
    }
    @Override
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
    private static class PrintInputStream extends Thread {
        BufferedReader in;
        public PrintInputStream(BufferedReader in) {
            this.in = in;
            System.out.println("now receiving messages from server...");
        }
        public void run() {
            String msg = "";
            while (msg != null) {
                try {
                    msg = in.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (msg != null && !msg.isEmpty()) {
                    System.out.println("\r" + msg.replace("\n","") + "     ");
                    System.out.print("Send a message: ");
                    Client.addText(msg);
                }
            }
        }
    }
}
