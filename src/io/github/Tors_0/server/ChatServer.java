package io.github.Tors_0.server;

import io.github.Tors_0.client.Client;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ChatServer implements Closeable {
    private ServerSocket serverSocket;
    private final ArrayList<EchoClientHandler> clientHandlers = new ArrayList<>();
    private final Object handlerListLock = new Object();
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
        Client.showAlertMessage("Server started on port " + port,"Success",JOptionPane.INFORMATION_MESSAGE);
        while (true) {
            EchoClientHandler handler = new EchoClientHandler(serverSocket.accept(), currentClient);
            handler.start();
            clientHandlers.add(handler);
            currentClient++;
        }
    }
    private void removeClient(EchoClientHandler handler) {
        synchronized (handlerListLock) {
            this.clientHandlers.remove(handler);
        }
    }
    public void distributeMsg(String msg) {
        synchronized (handlerListLock) {
            for (EchoClientHandler clientHandler : clientHandlers) {
                clientHandler.sendMsg(msg);
            }
        }
    }
    public void stop() throws IOException {
        distributeMsg("Server closed");
        discoveryThread.interrupt();
        serverStarted = false;
        for (EchoClientHandler handler : clientHandlers) {
            handler.closeClient();
        }
        serverSocket.close();
        clientHandlers.clear();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private static class EchoClientHandler extends Thread {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientID;
        private boolean nicknamed = false;
        private HashMap<String, String> commandRegistry = new HashMap<>();
        public EchoClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientID = String.valueOf(id);

            this.commandRegistry.put("/help", "Displays all valid commands");
            this.commandRegistry.put("/nickname YOURNAME", "Changes your nickname on the io.github.Tors_0.server");
            this.commandRegistry.put("/stop\", \"/exit\", \"/quit", "Disconnects you, closes io.github.Tors_0.server if you are host, and closes the window");
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
                    doMessageAction(msg);
                } else if (msg == null) {
                    msg = String.format("%s disconnected%n", (nicknamed ? clientID : "client " + clientID));
                    System.out.print(msg);
                    server.distributeMsg(msg);
                    server.removeClient(this);
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
                System.out.println(text);
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
