package server;

import client.Client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatServer implements Closeable {
    private ServerSocket serverSocket;
    private final ArrayList<EchoClientHandler> clientHandlers = new ArrayList<>();
    public static final ChatServer server = new ChatServer();
    static Thread discoveryThread;
    static int port;
    public static int getPort() {
        return port;
    }
    static boolean serverStarted = false;
    public static boolean isServerStarted() {
        return serverStarted;
    }
    public void start() throws IOException {
        ChatServer.port = Client.getPort();
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        discoveryThread = new DiscoveryThread();
        discoveryThread.start();
        System.out.println("Listening for clients on port " + port);
        int currentClient = 0;
        JOptionPane.showMessageDialog(null,"Server started on port " + port,"Success",JOptionPane.INFORMATION_MESSAGE);
        Client.setHostname("127.0.0.1");
        Client.getConnectAction().actionPerformed(null);
        serverStarted = true;
        while (true) {
            EchoClientHandler handler = new EchoClientHandler(serverSocket.accept(), currentClient);
            handler.start();
            clientHandlers.add(handler);
            currentClient++;
        }
    }
    private synchronized void removeClient(EchoClientHandler handler) {
        this.clientHandlers.remove(handler);
    }
    public synchronized void distributeMsg(String msg) {
        for (EchoClientHandler client : clientHandlers) {
            client.sendMsg(msg);
        }
    }
    public void stop() throws IOException {
        distributeMsg("Server closed...");
        discoveryThread.interrupt();
        serverStarted = false;
        serverSocket.close();
    }

    @Override
    public void close() throws IOException {
        stop();
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
                    msg = null;
                }

                if (msg != null && !msg.isEmpty()) {
                    System.out.printf("client %s: %s%n", clientID, msg);
                    msg = "client " + clientID + ": " + msg;
                    server.distributeMsg(msg);
                } else if (msg == null) {
                    msg = String.format("client %s disconnected%n", clientID);
                    System.out.print(msg);
                    server.distributeMsg(msg);
                    break;
                }
            }
            ChatServer.server.removeClient(this);
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
