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
    public static final ChatServer SERVER = new ChatServer();
    private ServerSocket serverSocket;
    private final ArrayList<ChatClientHandler> CLIENT_HANDLERS = new ArrayList<>();
    private final ReentrantLock CLIENT_HANDLERS_LOCK = new ReentrantLock();
    private final HashMap<String, String> IP_NAME_MAP = new HashMap<>();
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
        Client.setHostname("localhost");
        serverStarted = true;
        Client.getConnectAction().actionPerformed(null);
        new Thread(() -> {
            Client.showAlertMessage("Server started on port " + port,"Success",JOptionPane.INFORMATION_MESSAGE);
        }).start();
        while (true) {
            Socket clientSocket = serverSocket.accept();
            String ip = clientSocket.getInetAddress().getHostAddress();
            boolean reconnect = IP_NAME_MAP.containsKey(ip);
            ChatClientHandler handler = new ChatClientHandler(
                    clientSocket,
                    reconnect ? IP_NAME_MAP.get(ip) : String.valueOf(currentClient),
                    reconnect);
            handler.start();
            CLIENT_HANDLERS.add(handler);
            currentClient++;
        }
    }
    private void removeClient(ChatClientHandler handler) {
        this.CLIENT_HANDLERS.remove(handler);
    }
    public void distributeMsg(String msg) {
        synchronized (CLIENT_HANDLERS_LOCK) {
            for (ChatClientHandler clientHandler : CLIENT_HANDLERS) {
                clientHandler.sendMsg(msg);
            }
        }
    }
    public void stop() throws IOException {
        distributeMsg("Server closed");
        discoveryThread.interrupt();
        for (int i = CLIENT_HANDLERS.size()-1; i >= 0; i--) {
            CLIENT_HANDLERS.get(i).closeClient();
        }
        serverStarted = false;
        serverSocket.close();
        CLIENT_HANDLERS.clear();
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
        private final String IP;
        private static final HashMap<String, String> commandRegistry = new HashMap<>();
        static {
            commandRegistry.put("/help", "Displays all valid commands");
            commandRegistry.put("/nickname NICKNAME_HERE", "Changes your nickname (" + maxNicknameLength + " char limit)");
            commandRegistry.put("/stop\", \"/exit\", \"/quit", "Disconnects you, closes server if you are host, and closes the window");
        }
        public ChatClientHandler(Socket socket, String id, boolean nicknamed) {
            this.clientSocket = socket;
            this.clientID = id;
            this.IP = clientSocket.getInetAddress().getHostAddress();
            this.nicknamed = nicknamed;
        }
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 512);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            out.println("Use \"/help\" to get a list of valid commands from the server");
            SERVER.distributeMsg((nicknamed ? clientID : "client " + clientID) + " joined\n");
            String msg = "";
            while (msg != null) {
                try {
                    msg = in.readLine().trim();
                } catch (IOException | NullPointerException e) {
                    msg = null;
                }

                if (msg != null && !msg.isEmpty()) {
                    doMessageAction(msg);
                } else if (msg == null) {
                    synchronized (SERVER.CLIENT_HANDLERS_LOCK) {
                        SERVER.removeClient(this);
                    }
                    msg = String.format("%s disconnected%n", (nicknamed ? clientID : "client " + clientID));
                    System.out.print(msg);
                    SERVER.distributeMsg(msg);
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
                String newName = text.substring(9).trim();
                if (!newName.isEmpty()) {
                    if (SERVER.IP_NAME_MAP.containsValue(newName.toLowerCase()) && !newName.equalsIgnoreCase(SERVER.IP_NAME_MAP.get(IP))) {
                        this.out.printf("Nickname \"%s\" already taken on this server%n", newName);
                    } else if (newName.length() <= ChatServer.maxNicknameLength) {
                        SERVER.distributeMsg((nicknamed ? clientID : "client " + clientID) + " changed nickname to " + newName);
                        clientID = newName;
                        nicknamed = true;
                        SERVER.IP_NAME_MAP.put(IP,clientID.toLowerCase());
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
                SERVER.distributeMsg(text);
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
