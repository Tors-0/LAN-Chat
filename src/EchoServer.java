import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String msg = "";
        while (msg != null) {
            msg = in.readLine();
            out.println(msg);
            System.out.println("got msg from client: " + msg);
            if (msg.equals("stop")) {
                stop();
                break;
            }
        }
    }
    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
    public static void main(String[] args) {
        EchoServer server = new EchoServer();
        try {
            server.start(6666);
        } catch (Exception e) {
            System.out.println("internal server error: " + e.getMessage());
        }
    }
}
