package io.github.Tors_0.client;

import io.github.Tors_0.server.ChatServer;
import io.github.Tors_0.util.*;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

public class Client {
    static Networking myNetCon = new Networking();
    static JFrame frame;
    static JMenuBar menuBar;
    static JMenu commandMenu;
    private static boolean useFallbackTheme = false;
    static final Image IMAGE = Toolkit.getDefaultToolkit().createImage(SysTrayToast.class.getResource("/io/github/Tors_0/resources/lanchat.png"));
    static JTextField msgField;
    static JLabel msgLabel;
    static JButton sendButton;
    static JPanel msgPane;
    static int port;
    static final boolean IS_MAC = SystemInfo.isMac();
    static final boolean IS_LINUX = SystemInfo.isLinux();
    public static int getPort() {
        return port;
    }
    public static void setHostname(String hostname) {
        Client.hostname = hostname.replaceAll(" ","");
        if (hostLabel != null) {
            hostLabel.setText("Current host: " + hostname + ":" + port);
        }
    }

    static String hostname = "";
    static JLabel hostLabel;
    static JButton disconnectButton;
    static final String DISCONNECT = "Exit";
    static JTextArea textArea;
    static JScrollPane scrollableTextArea;
    static SmartScroller smartScroller;
    static JButton joinButton;
    static JButton hostButton;
    static Action connectAction;
    public static Action getConnectAction() {
        return connectAction;
    }
    static JTextField menuPortField;

    static JPanel chatPane;
    static JPanel configPane;
    static JPanel menuPane;

    static Container contentPane;

    static boolean connected = false;
    static final int discoveryTimeout = 5000;

    // user nickname (sent to server on first connect)
    static String nickname;

    // UDP socket for server discovery
    static DatagramSocket c;
    static ArrayList<String> hosts = new ArrayList<>();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());


        } catch (Exception e) {
            new Thread(() -> {
                JOptionPane.showMessageDialog(frame, "System Theme not supported, using fallback theme");
            }).start();
            useFallbackTheme = true;
        }

        Fonts.initialize();

        windowInit();
    }

    private static void windowInit() {
        frame = new ChatFrame("ChatClient");

        frame.setMinimumSize(new Dimension(525,300));
        frame.setPreferredSize(new Dimension(525,450));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(IMAGE);

        // menu bar init
        menuBar = new JMenuBar();

        commandMenu = new JMenu("Commands");

        JMenuItem helpCommand = new JMenuItem("Help");
        helpCommand.addActionListener(e -> {
            sendToServer("/help");
        });
        commandMenu.add(helpCommand);

        JMenuItem nicknameCommand = new JMenuItem("Set Nickname");
        nicknameCommand.addActionListener(e -> {
            String nick = JOptionPane.showInputDialog(frame, "Enter new nickname (3-20 chars): ");
            if (connected) {
                sendToServer("/nickname " + nick);
            } else {
                nickname = nick;
            }
        });
        commandMenu.add(nicknameCommand);

        JMenuItem quitCommand = new JMenuItem("Quit");
        quitCommand.addActionListener(e -> {
            stopClient();
        });
        commandMenu.add(quitCommand);

        menuBar.add(commandMenu);

        frame.setJMenuBar(menuBar);

        // end menu bar init

        msgLabel = new JLabel("Send a message: ", SwingConstants.LEFT);
        msgLabel.setFont(Fonts.m5x7(20));

        msgField = new JTextField();
        msgField.setFont(Fonts.m5x7(20));
        ((AbstractDocument) msgField.getDocument()).setDocumentFilter(new LimitDocumentFilter(128));
        msgField.setVisible(true);

        sendButton = new JButton("Send");
        sendButton.setFont(Fonts.m5x7(20));

        msgPane = new JPanel();
        msgPane.setLayout(new BoxLayout(msgPane,BoxLayout.X_AXIS));

        msgPane.add(msgLabel);
        msgPane.add(msgField);
        msgPane.add(Box.createRigidArea(new Dimension(5,0)));
        msgPane.add(sendButton);
        msgPane.setMaximumSize(new Dimension(10_000,50)); // horizontal doesn't matter as long as it is too large to feasibly be reached

        chatPane = new JPanel();
        chatPane.setLayout(new BoxLayout(chatPane,BoxLayout.Y_AXIS));
        chatPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        setupTextArea(false); // get the text area set up for initial maths

        // begin config panel
        hostLabel = new JLabel("Current host: " + hostname);
        hostLabel.setFont(Fonts.m5x7(20));

        disconnectButton = new JButton(DISCONNECT);
        disconnectButton.setFont(Fonts.m5x7(20));

        configPane = new JPanel();
        configPane.setLayout(new BoxLayout(configPane,BoxLayout.X_AXIS));
        configPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        configPane.add(hostLabel);
        configPane.add(Box.createRigidArea(new Dimension(5,0)));
        configPane.add(disconnectButton);
        if (IS_LINUX) { // add notification sound mute button for linux computers
            JToggleButton soundToggle = new JToggleButton("Sound: ON");
            soundToggle.setFont(Fonts.m5x7(20));
            soundToggle.addActionListener(new AbstractAction() {
                private boolean muted = false;
                @Override
                public void actionPerformed(ActionEvent e) {
                    muted = !muted;
                    soundToggle.setText("Sound: " + (muted ? "OFF" : "ON"));
                    Networking.PlaySound.setMuted(muted);
                }
            });
            configPane.add(Box.createRigidArea(new Dimension(5, 0)));
            configPane.add(soundToggle);
        }

//        scrollableTextArea added via setup method
        chatPane.add(Box.createRigidArea(new Dimension(0,5)));
        chatPane.add(msgPane);
        chatPane.add(Box.createRigidArea(new Dimension(0,5)));
        chatPane.add(configPane);


        // begin main menu

        joinButton = new JButton("Join");
        joinButton.setFont(Fonts.m3x6(40));

        hostButton = new JButton("Host");
        hostButton.setFont(Fonts.m3x6(40));

        menuPane = new JPanel();
        menuPane.setLayout(new BoxLayout(menuPane,BoxLayout.X_AXIS));

        JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane,BoxLayout.X_AXIS));
        inputPane.setMaximumSize(new Dimension(400, joinButton.getHeight()));

        inputPane.add(joinButton);
        inputPane.add(Box.createRigidArea(new Dimension(5,0)));
        inputPane.add(hostButton);


        JPanel centeredPanel = new JPanel();
        centeredPanel.setLayout(new BoxLayout(centeredPanel, BoxLayout.Y_AXIS));

        centeredPanel.add(Box.createVerticalGlue());
        centeredPanel.add(inputPane);
        centeredPanel.add(Box.createVerticalGlue());

        menuPane.add(Box.createHorizontalGlue());
        menuPane.add(centeredPanel);
        menuPane.add(Box.createHorizontalGlue());


        contentPane = frame.getContentPane();

        if (useFallbackTheme) {
            textArea.setForeground(Color.white);
            textArea.setBackground(Color.gray);
            msgField.setCaretColor(Color.white);
            chatPane.setBackground(Color.darkGray);
            colorComponents(chatPane);
            configPane.setBackground(Color.darkGray);
            colorComponents(configPane);
            menuPortField.setCaretColor(Color.white);
            menuPane.setBackground(Color.darkGray);
            centeredPanel.setBackground(Color.darkGray);
            colorComponents(centeredPanel);
            contentPane.setBackground(Color.darkGray);
            frame.setBackground(Color.darkGray);
        }


        frame.pack();
        frame.setVisible(true);

        contentPane.add(menuPane, BorderLayout.CENTER);
        menuPane.setVisible(true);
        contentPane.setVisible(true);

        Action msgAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = msgField.getText().trim();
                msgField.setText("");
                if ("/stop".equals(text) || "/exit".equals(text) || "/quit".equals(text)) {
                    stopClient();
                }
                sendToServer(text);
            }
        };
        msgField.addActionListener(msgAction);
        sendButton.addActionListener(e -> {
            msgAction.actionPerformed(e);
            msgField.requestFocusInWindow();
        });
        connectAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (connected) {
                    try {
                        if (ChatServer.isServerStarted()) {
                            ChatServer.SERVER.stop();
                        }

                        myNetCon.close();
                        connected = false;

                        setMainMenu(true); // disconnect
                        disconnectButton.requestFocusInWindow();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame,ex.toString(),"Disconnect Error",JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    if (hostname == null || hostname.replaceAll(" ","").isEmpty() || port < 1) return;
                    try {
                        myNetCon.startConnection(hostname, port);

                        setMainMenu(false); // connect

                        connected = true;
                        disconnectButton.setText((ChatServer.isServerStarted() ? "Stop Server and " : "") + DISCONNECT);
                        textArea.setText("Connected to " + hostname + " on port " + port);
                        if (nickname != null) {
                            sendToServer("/nickname " + nickname);
                        }
                        msgField.requestFocusInWindow(); // focus the message box
                    } catch (IOException ex) {
                        if (e != null) {
                            JOptionPane.showMessageDialog(frame, ex.toString(), "Connect Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        };
        disconnectButton.addActionListener(connectAction);
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

                        setMainMenu(true); // if we fail to find a server
                    }
                }).start();
            }
        };
        Action joinAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validatePortSelection()) return; // cancel on invalid port numbers
                joinButton.setEnabled(false);

                searchAction.actionPerformed(e);
                disconnectButton.setText(DISCONNECT);

                joinButton.setEnabled(true);
            }
        };
        joinButton.addActionListener(joinAction);
        Action hostAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validatePortSelection()) return; // cancel on invalid port numbers
                hostButton.setEnabled(false);

                new Thread(() -> {
                    hosts = findLocalServerIPs();
                    if (hosts.isEmpty()) {
                        new Thread(() -> {
                            try {
                                ChatServer.SERVER.start();
                            } catch (IOException ex) {
                                if (!ex.getClass().equals(SocketException.class)) {
                                    JOptionPane.showMessageDialog(frame, ex.toString(), "Server Error", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }).start();

                        setMainMenu(false); // show chat screen after starting server
                    } else {
                        if (0 == JOptionPane.showConfirmDialog(frame,"Server already exists on this port, would you like to join it?", "Cannot Host", JOptionPane.YES_NO_OPTION)) {
                            setHostname(hosts.get(0));
                            connectAction.actionPerformed(e);
                        }
                    }
                    hostButton.setEnabled(true);
                }).start();
            }
        };
        hostButton.addActionListener(hostAction);
    }

    private static void stopClient() {
        System.out.println("stopping client...");
        try {
            close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame,ex.toString(),"Stop Error",JOptionPane.ERROR_MESSAGE);
        }
        System.exit(0);
    }

    private static void setupTextArea(boolean notInit) {
        // begin messaging panel
        textArea = new JTextArea();
        textArea.setFont(Fonts.m5x7(20));
        textArea.setForeground(Color.white);
        textArea.setRows(15);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setVisible(true);

        scrollableTextArea = useFallbackTheme ? new ModernScrollPane(textArea) : new JScrollPane(textArea);
        scrollableTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableTextArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        smartScroller = new SmartScroller(scrollableTextArea);

        if (notInit) {
            chatPane.remove(0);
        }
        chatPane.add(scrollableTextArea,0);
        // TODO 2024.01.28: fix this janky mess and figure out a better way to refresh the frame
        frame.setSize(frame.getWidth()+1,frame.getHeight());
        frame.setSize(frame.getWidth()-1,frame.getHeight());
    }

    /**
     * Prompt the user for a port number
     * @return true if port is valid (within 1024 and 49151, inclusive), false otherwise
     */
    private static boolean validatePortSelection() {
        String txt = inputPortNumber();
        if (txt == null || txt.isEmpty() || isInvalidPort(txt)) return true;
        port = Integer.parseInt(txt);
        if (port < 1024 || port > 49151) return true;

        return false;
    }

    private static void setMainMenu(boolean mainMenu) {
        if (mainMenu) {
            contentPane.remove(chatPane);
            contentPane.add(menuPane, BorderLayout.CENTER);
        } else {
            contentPane.remove(menuPane);
            setupTextArea(true);
            contentPane.add(chatPane,BorderLayout.CENTER);
        }
        frame.repaint();
    }

    private static String inputPortNumber() {
        return JOptionPane.showInputDialog(frame,"Please input a port number from 1024 to 49151","11209");
    }

    public static boolean isInvalidPort(String text) {
        return (text.length() != 4 && text.length() != 5) || !text.matches("[0-9]+");
    }
    private static void colorComponents(JComponent component) {
        for (Component comp : component.getComponents()) {
            comp.setBackground(Color.gray);
            comp.setForeground(Color.white);
            if (comp instanceof JButton && IS_MAC) {
                comp.setForeground(Color.lightGray);
                ((JButton) comp).setOpaque(true);
                ((JButton) comp).setBorderPainted(false);
            }
        }
    }
    public static void showAlertMessage(String msg, String name, int messageType) {
        JOptionPane.showMessageDialog(frame,msg,name,messageType);
    }
    public static void addText(String txt) {
        if (textArea != null) {
            textArea.append("\n" + txt);
//            textArea.setRows(textArea.getRows() + 1);
//            textArea.repaint();
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
            //Open a specific port to send the package
            c = new DatagramSocket(port);
            c.setBroadcast(true);

            byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
                c.send(sendPacket);
                System.out.println(Client.class.getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception ignored) {}

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
            c.setSoTimeout(2500);
            do {
                c.receive(receivePacket);
            } while (!"DISCOVER_FUIFSERVER_RESPONSE".equals(new String(receivePacket.getData()).trim()));

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
            if (ex.getClass().equals(BindException.class)) {
                showAlertMessage("Port already in use by another application", "Port Busy", JOptionPane.INFORMATION_MESSAGE);
            }
            System.out.println(ex.toString());
            if (c != null) {
                c.close();
            }
        }
        return serverIPs;
    }

    public static void close() throws IOException {
        if (ChatServer.isServerStarted()) {
            ChatServer.SERVER.stop();
        }
        if (connected) {
            myNetCon.close();
        }
    }
}