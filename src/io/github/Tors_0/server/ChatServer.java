package io.github.Tors_0.server;

import io.github.Tors_0.client.Client;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer implements Closeable {
    private ServerSocket serverSocket;
    private final ArrayList<ChatClientHandler> clientHandlers = new ArrayList<>();
    private final ReentrantLock handlerListLock = new ReentrantLock();
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
    static final int maxNicknameLength = 20;
    public void start() throws IOException {
        ChatServer.port = Client.getPort();
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        discoveryThread = new DiscoveryThread();
        discoveryThread.start();
        System.out.println("Listening for clients on port " + port);
        int currentClient = 0;
        Client.setHostname("127.0.0.1");
        serverStarted = true;
        Client.getConnectAction().actionPerformed(null);
        new Thread(() -> {
            Client.showAlertMessage("Server started on port " + port,"Success",JOptionPane.INFORMATION_MESSAGE);
        }).start();
        while (true) {
            ChatClientHandler handler = new ChatClientHandler(serverSocket.accept(), currentClient);
            handler.start();
            clientHandlers.add(handler);
            currentClient++;
        }
    }
    private void removeClient(ChatClientHandler handler) {
        this.clientHandlers.remove(handler);
    }
    public void distributeMsg(String msg) {
        synchronized (handlerListLock) {
            for (ChatClientHandler clientHandler : clientHandlers) {
                clientHandler.sendMsg(msg);
            }
        }
    }
    public void stop() throws IOException {
        distributeMsg("Server closed");
        discoveryThread.interrupt();
        for (int i = clientHandlers.size()-1; i >= 0; i--) {
            clientHandlers.get(i).closeClient();
        }
        serverStarted = false;
        serverSocket.close();
        clientHandlers.clear();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private static class ChatClientHandler extends Thread {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientID;
        private boolean nicknamed = false;
        private final HashMap<String, String> commandRegistry = new HashMap<>();
        public String getIP() {
            return clientSocket.getInetAddress().getHostAddress();
        }
        public ChatClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientID = String.valueOf(id);

            this.commandRegistry.put("/help", "Displays all valid commands");
            this.commandRegistry.put("/nickname NICKNAME_HERE", "Changes your nickname (" + maxNicknameLength + " char limit)");
            this.commandRegistry.put("/stop\", \"/exit\", \"/quit", "Disconnects you, closes server if you are host, and closes the window");
        }
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 512);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            out.println("Use \"/help\" to get a list of valid commands from the server");
            server.distributeMsg("client " + clientID + " joined\n");
            String msg = "";
            while (msg != null) {
                try {
                    msg = in.readLine().trim();
                } catch (IOException e) {
                    msg = null;
                } catch (NullPointerException ex) {
                    System.out.println("client " + clientID + " gone");
                }

                if (msg != null && !msg.isEmpty()) {
                    doMessageAction(msg);
                } else if (msg == null) {
                    synchronized (server.handlerListLock) {
                        server.removeClient(this);
                    }
                    msg = String.format("%s disconnected%n", (nicknamed ? clientID : "client " + clientID));
                    System.out.print(msg);
                    server.distributeMsg(msg);
                    break;
                }
            }
        }
        private void doMessageAction(String text) {
            if (text.startsWith("/help")) {
                out.println("Valid server commands:");
                commandRegistry.forEach((com,desc) -> {
                    out.printf("\"%s\" -> %s%n", com, desc);
                });
                out.println("\n");
            } else if (text.startsWith("/nickname")) {
                String newName = text.substring(9);
                if (!newName.trim().isEmpty()) {
                    if (newName.trim().length() <= ChatServer.maxNicknameLength) {
                        server.distributeMsg((nicknamed ? clientID : "client " + clientID) + " changed nickname to " + newName.trim());
                        clientID = newName.trim();
                        nicknamed = true;
                    } else {
                        this.out.println("Nickname must not exceed length " + ChatServer.maxNicknameLength);
                    }
                } else {
                    this.out.println("Nickname must not be blank...");
                }
            } else if (text.startsWith("/")) {
                out.println("Invalid command...");
            } else {
                text = String.format("%s: %s%n", (nicknamed ? clientID : "client " + clientID), text);
                System.out.print(text);
                server.distributeMsg(text);
            }
        }
        public void closeClient() {
            try {
                clientSocket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public void sendMsg(String msg) {
            out.println(msg);
        }
    }
}
