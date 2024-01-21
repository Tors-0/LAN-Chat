package client;

import server.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Closeable {
    static Networking myNetCon;
    static Scanner scanner = new Scanner(System.in);
    static JFrame frame;
    static JTextField msgField;
    static JLabel msgLabel;
    static int port;
    public static int getPort() {
        return port;
    }
    public static void setHostname(String hostname) {
        Client.hostname = hostname.replaceAll(" ","");
        if (hostLabel != null) {
            hostLabel.setText("Current host: " + hostname);
        }
    }

    static String hostname = "               ";
    static JLabel hostLabel;
    static JButton connectButton;
    static final String DISCONNECT = "Exit";
    static JTextArea textArea;
    static JScrollPane scrollableTextArea;
    static JButton clientButton;
    static JButton serverButton;
    static Action connectAction;
    public static Action getConnectAction() {
        return connectAction;
    }
    static JTextField menuPortField;

    static JPanel chatPane;
    static JPanel configPane;
    static JPanel menuPane;

    static boolean connected = false;
    static final int joinTimeout = 3000;
    static final int discoveryTimeout = 5000;

    // UDP socket for server discovery
    static DatagramSocket c;
    static ArrayList<String> hosts = new ArrayList<>();

    public static void main(String[] args) {
        myNetCon = new Networking();

        windowInit();
    }

    private static void windowInit() {
        frame = new JFrame("ChatClient");
        frame.setMinimumSize(new Dimension(520,450));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // begin messaging panel
        textArea = new JTextArea("",25,60);
        textArea.setSize(500,230);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setForeground(Color.white);
        textArea.setBackground(Color.gray);
        textArea.setVisible(true);

        scrollableTextArea = new ModernScrollPane(textArea);
        scrollableTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableTextArea.setSize(500,420);
        new SmartScroller(scrollableTextArea);

        msgLabel = new JLabel("Send a message:");
        msgLabel.setHorizontalAlignment(JLabel.LEFT);

        msgField = new JTextField(60);
        msgField.setCaretColor(Color.white);
        msgField.setVisible(true);

        chatPane = new JPanel();
        chatPane.setLayout(new BoxLayout(chatPane,BoxLayout.Y_AXIS));
        chatPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        chatPane.setBackground(Color.darkGray);

        chatPane.add(scrollableTextArea);
        chatPane.add(Box.createRigidArea(new Dimension(0,5)));
        chatPane.add(msgLabel);
        chatPane.add(msgField);

        for (Component comp : chatPane.getComponents()) {
            comp.setBackground(Color.gray);
            comp.setForeground(Color.white);
        }

        // begin config panel
        hostLabel = new JLabel("Current host: " + hostname);

        connectButton = new JButton(DISCONNECT);

        configPane = new JPanel();
        configPane.setLayout(new BoxLayout(configPane,BoxLayout.X_AXIS));
        configPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        configPane.setBackground(Color.darkGray);

        configPane.add(hostLabel);
        configPane.add(Box.createRigidArea(new Dimension(5,0)));
        configPane.add(connectButton);

        for (Component comp : configPane.getComponents()) {
            comp.setBackground(Color.gray);
            comp.setForeground(Color.white);
        }

        menuPortField = new JTextField(5);
        menuPortField.setMaximumSize(new Dimension(150,25));
        menuPortField.setCaretColor(Color.white);

        clientButton = new JButton("Join");

        serverButton = new JButton("Host");

        menuPane = new JPanel();
        menuPane.setLayout(new BoxLayout(menuPane,BoxLayout.X_AXIS));
        menuPane.setBackground(Color.darkGray);

        JPanel centeredPanel = new JPanel();
        centeredPanel.setLayout(new BoxLayout(centeredPanel, BoxLayout.Y_AXIS));
        centeredPanel.setBackground(Color.darkGray);

        centeredPanel.add(Box.createVerticalGlue());
        centeredPanel.add(new JLabel("LAN Chat"));
        centeredPanel.add(new JLabel("Enter port below"));
        centeredPanel.add(menuPortField);
        centeredPanel.add(Box.createRigidArea(new Dimension(0,5)));
        centeredPanel.add(clientButton);
        centeredPanel.add(Box.createRigidArea(new Dimension(0,5)));
        centeredPanel.add(serverButton);
        centeredPanel.add(Box.createVerticalGlue());

        for (Component comp : centeredPanel.getComponents()) {
            comp.setBackground(Color.gray);
            comp.setForeground(Color.white);
        }

        menuPane.add(Box.createHorizontalGlue());
        menuPane.add(centeredPanel);
        menuPane.add(Box.createHorizontalGlue());

        Container contentPane = frame.getContentPane();
        contentPane.add(chatPane, BorderLayout.NORTH);
        contentPane.add(configPane, BorderLayout.SOUTH);
        contentPane.setBackground(Color.darkGray);

        frame.setBackground(Color.darkGray);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

        chatPane.setVisible(false);
        configPane.setVisible(false);
        menuPane.setVisible(true);
        contentPane.add(menuPane, BorderLayout.CENTER);

        Action msgAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = msgField.getText();
                msgField.setText("");
                if ("stop".equals(text)) {
                    System.out.println("stopping client...");
                    System.exit(0);
                }
                sendToServer(text);
            }
        };
        msgField.addActionListener(msgAction);
        connectAction = new AbstractAction() {
            long nextAvailableTimeMillis = System.currentTimeMillis() + joinTimeout;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (connected) {
                    try {
                        if (ChatServer.isServerStarted()) {
                            ChatServer.server.stop();
                        }
                        myNetCon.close();
                        connected = false;
                        chatPane.setVisible(false);
                        configPane.setVisible(false);
                        menuPane.setVisible(true);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame,ex.toString(),"Disconnect Error",JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    if (nextAvailableTimeMillis > System.currentTimeMillis()) {
                        JOptionPane.showMessageDialog(frame,"Please wait " + (nextAvailableTimeMillis-System.currentTimeMillis()) + "ms before connecting again","Timed Out!",JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    nextAvailableTimeMillis = System.currentTimeMillis() + joinTimeout;
                    if (hostname == null || hostname.replaceAll(" ","").isEmpty() || port < 1) return;
                    try {
                        myNetCon.startConnection(hostname, port);
                        connected = true;
                        textArea.setText("Connected to " + hostname + " on port " + port);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, ex.toString(), "Connect Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        connectButton.addActionListener(connectAction);
        Action searchAction = new AbstractAction() {
            long nextAvailableTimeMillis = System.currentTimeMillis();
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nextAvailableTimeMillis > System.currentTimeMillis()) {
                    JOptionPane.showMessageDialog(frame,"Please wait " + (nextAvailableTimeMillis-System.currentTimeMillis()) + "ms before searching again","Timed Out!",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                nextAvailableTimeMillis = System.currentTimeMillis() + discoveryTimeout;
                if (port < 1) return;
                new Thread(() -> {
                    hosts = findLocalServerIPs();
                    if (!hosts.isEmpty()) {
                        System.out.println("found hosts: " + hosts);
                        setHostname(hosts.get(0));
                        connectAction.actionPerformed(e);
                    } else {
                        System.out.println("no hosts found");

                        JOptionPane.showMessageDialog(frame,"No server on port " + port, "No Host Found", JOptionPane.INFORMATION_MESSAGE);

                        chatPane.setVisible(false);
                        configPane.setVisible(false);
                        menuPane.setVisible(true);
                    }
                }).start();
            }
        };
        Action joinAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (menuPortField.getText().isEmpty()) return;
                port = Integer.parseInt(menuPortField.getText());
                searchAction.actionPerformed(e);
                connectButton.setText(DISCONNECT);
                menuPane.setVisible(false);
                chatPane.setVisible(true);
                configPane.setVisible(true);
            }
        };
        clientButton.addActionListener(joinAction);
        Action hostAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (menuPortField.getText().isEmpty()) return;
                port = Integer.parseInt(menuPortField.getText());

                new Thread(() -> {
                    try {
                        ChatServer.server.start();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, ex.toString(), "Server Error", JOptionPane.ERROR_MESSAGE);
                    }
                }).start();

                connectButton.setText("Stop server and " + DISCONNECT);
                menuPane.setVisible(false);
                chatPane.setVisible(true);
                configPane.setVisible(true);
            }
        };
        serverButton.addActionListener(hostAction);
    }

    public static void addText(String txt) {
        if (textArea != null) {
            textArea.append("\n" + txt);
        }
    }

    public static void sendToServer(String msg) {
        try {
            myNetCon.sendMsg(msg);
        } catch (Exception e) {
            System.out.println("sTS(String) got exception: " + e.getMessage());
        }
    }

    /**
     * Website: <a href="https://michieldemey.be/blog/network-discovery-using-udp-broadcast/">Code Source</a>
     * @return list of hostname strings found by server search. length may be 0
     * @author Michiel De Mey
     */
    private static ArrayList<String> findLocalServerIPs() {
        ArrayList<String> serverIPs = new ArrayList<>();

        // Find the server using UDP broadcast
        try {
            //Open a random port to send the package
            c = new DatagramSocket();
            c.setBroadcast(true);

            byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
                c.send(sendPacket);
                System.out.println(Client.class.getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception ignored) {
            }

            // Broadcast the message over all the network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, port);
                        c.send(sendPacket);
                    } catch (Exception ignored) {}

                    System.out.println(Client.class.getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            System.out.println(Client.class.getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[256];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.setSoTimeout(1000);
            c.receive(receivePacket);

            //We have a response
            System.out.println(Client.class.getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
                //DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
                serverIPs.add(receivePacket.getAddress().getHostAddress());
            }

            //Close the port!
            c.close();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return serverIPs;
    }

    @Override
    public void close() throws IOException {
        myNetCon.close();
    }
}