import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class EchoServer {
    private ServerSocket serverSocket;
    private final ArrayList<EchoClientHandler> clientHandlers = new ArrayList<>();
    static EchoServer server = new EchoServer();
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        int currentClient = 0;
        while (true) {
            EchoClientHandler handler = new EchoClientHandler(serverSocket.accept(), currentClient);
            handler.start();
            clientHandlers.add(handler);
            currentClient++;
        }
    }
    private void removeClient(EchoClientHandler client) {
        clientHandlers.remove(client);
    }
    public synchronized void distributeMsg(String msg) {
        for (EchoClientHandler client : clientHandlers) {
            client.sendMsg(msg);
        }
    }
    public void stop() throws IOException {
        serverSocket.close();
    }
    public static void main(String[] args) {
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
    private static class EchoClientHandler extends Thread {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private final int clientID;
        public EchoClientHandler(Socket socket, int ID) {
            this.clientSocket = socket;
            this.clientID = ID;
        }
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 512);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String msg = "";
            while (msg != null) {
                try {
                    msg = in.readLine();
                } catch (IOException e) {
                    // exception can happen here when client disconnects
                }

                if (msg != null) {
                    System.out.printf("client %s: %s%n", clientID, msg);
                    msg = "client " + clientID + ": " + msg;
                    server.distributeMsg(msg);
                } else {
                    msg = String.format("client %s disconnected%n", clientID);
                    System.out.print(msg);
                    server.distributeMsg(msg);
                    break;
                }
            }
            EchoServer.server.removeClient(this);
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public void sendMsg(String msg) {
            out.println(msg);
        }
    }
}
