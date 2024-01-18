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
        System.out.println("Server started on port " + port);
        int currentClient = 0;
        while (true) {
            clientSocket = serverSocket.accept();
            int finalCurrentClient = currentClient;
            new Thread(() -> {
                try {
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String msg = "";
                while (msg != null) {
                    try {
                        msg = in.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    out.println(msg);
                    System.out.printf("got msg from client %s: %s%n", finalCurrentClient, msg);
                    if (msg.equals("stop")) {
                        try {
                            stop();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                }
            }).start();
            currentClient++;
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
        if (args.length < 1) {
            System.out.println("No argument given, exiting...");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        try {
            server.start(port);
        } catch (Exception e) {
            System.out.println("internal server error: " + e.getMessage());
        }
    }
}
