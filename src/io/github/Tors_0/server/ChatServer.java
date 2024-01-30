package io.github.Tors_0.server;

import io.github.Tors_0.client.Client;
import io.github.Tors_0.util.NetDataUtil;

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
    static final int minNicknameLength = 3;
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
        while (true) { // start the server loop, grab all incoming clients and give them a handler
            Socket clientSocket = serverSocket.accept();
            String ip = clientSocket.getInetAddress().getHostAddress();
            boolean reconnect = IP_NAME_MAP.containsKey(ip);
            ChatClientHandler handler = new ChatClientHandler(
                    clientSocket,
                    reconnect ? IP_NAME_MAP.get(ip) : "client " + currentClient
            );
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
        private PrintWriter toClientWriter;
        private BufferedReader fromClientStream;
        private String clientID;
        private boolean sentJoinMsg = false;
        private final String CLIENT_IP;
        private static final HashMap<String, String> commandRegistry = new HashMap<>();
        static {
            commandRegistry.put("/help", "Displays all valid commands");
            commandRegistry.put("/nickname NICKNAME_HERE", "Changes your nickname (" + maxNicknameLength + " char limit)");
            commandRegistry.put("/stop\", \"/exit\", \"/quit", "Disconnects you, closes server if you are host, and closes the window");
        }
        public ChatClientHandler(Socket socket, String id) {
            this.clientSocket = socket;
            this.clientID = id;
            this.CLIENT_IP = clientSocket.getInetAddress().getHostAddress();
        }
        public String getClientID() {
            return clientID;
        }
        public void run() {
            try {
                fromClientStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 512);
                toClientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            toClientWriter.println("Use \"/help\" to get a list of valid commands from the server");
            String msg = "";
            while (msg != null) {
                try {
                    msg = fromClientStream.readLine().trim();
                } catch (IOException | NullPointerException e) {
                    msg = null;
                }

                if (msg != null && !msg.substring(10).isEmpty()) {
                    if (msg.startsWith(NetDataUtil.Identifier.MESSAGE.getKeyString())) {
                        doMessageAction(msg);
                    } else if (msg.startsWith(NetDataUtil.Identifier.INFO_REQUEST.getKeyString())) {
                        answerInfoRequest(msg);
                    }
                } else if (msg == null) {
                    synchronized (SERVER.CLIENT_HANDLERS_LOCK) {
                        SERVER.removeClient(this);
                    }
                    msg = String.format("%s disconnected%n", clientID);
                    System.out.print(msg);
                    SERVER.distributeMsg(msg);
                    break;
                }
            }
        }
        private void doMessageAction(String msg) {
            String text = msg.substring(10);
            if (text.startsWith("/help")) {
                toClientWriter.println("Valid server commands:");
                commandRegistry.forEach((com,desc) -> {
                    NetDataUtil.sendMessage(toClientWriter, String.format("\"%s\" -> %s%n", com, desc));
                });
                NetDataUtil.sendMessage(toClientWriter, "\n");
            } else if (text.startsWith("/nickname")) {
                String newName = text.substring(9).trim().replaceAll(",","");
                if (!newName.isEmpty()) {
                    if (SERVER.IP_NAME_MAP.containsValue(newName.toLowerCase()) && !newName.equalsIgnoreCase(SERVER.IP_NAME_MAP.get(CLIENT_IP))) {
                        NetDataUtil.sendMessage(toClientWriter, String.format("Nickname \"%s\" already taken on this server%n", newName));
                    } else if (3 <= newName.length() && newName.length() <= ChatServer.maxNicknameLength) {
                        if (sentJoinMsg) {
                            SERVER.distributeMsg(clientID + " changed nickname to " + newName);
                        } else {
                            SERVER.distributeMsg(newName + " joined\n");
                            sentJoinMsg = true;
                        }
                        clientID = newName;
                        SERVER.IP_NAME_MAP.put(CLIENT_IP,clientID.toLowerCase());
                    } else {
                        NetDataUtil.sendMessage(toClientWriter, "Nickname wrong length. Min: " + ChatServer.minNicknameLength + " Max: " + ChatServer.maxNicknameLength);
                    }
                } else {
                    NetDataUtil.sendMessage(toClientWriter, "Nickname must not be blank...");
                }
            } else if (text.startsWith("/")) {
                NetDataUtil.sendMessage(toClientWriter, "Invalid command...");
            } else {
                if (!sentJoinMsg) {
                    sentJoinMsg = true;
                    SERVER.distributeMsg(clientID + " joined\n");
                }
                text = String.format("%s: %s%n", clientID, text);
                System.out.print(text);
                SERVER.distributeMsg(text);
            }
        }
        private void answerInfoRequest(String msg) {
            String request = msg.substring(10);
            if (request.startsWith(NetDataUtil.ONLINE_REQUEST)) {
                StringBuilder data = new StringBuilder();
                data.append(NetDataUtil.ONLINE_RESPONSE);
                SERVER.CLIENT_HANDLERS.forEach(handler -> {
                    data.append(handler.getClientID() + ",");
                });
                NetDataUtil.sendInfoResponse(toClientWriter, data.toString());
            }
        }
        public void closeClient() {
            try {
                clientSocket.close();
                fromClientStream.close();
                toClientWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public void sendMsg(String msg) {
            NetDataUtil.sendMessage(toClientWriter, msg);
        }
    }
}
