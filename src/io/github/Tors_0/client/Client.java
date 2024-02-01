package io.github.Tors_0.client;

import io.github.Tors_0.crypto.AESUtil;
import io.github.Tors_0.server.ChatServer;
import io.github.Tors_0.util.*;
import io.github.Tors_0.util.NetDataUtil.Identifier;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

public class Client {
    static ChatClient myChatClient = new ChatClient();
    // crypto vars
    static IvParameterSpec cryptoIv;
    static String cryptoPassword;
    static SecretKey cryptoKey;
    static boolean serverNeedsPass = true;
    static boolean expectingServerPasswordResponse = false;
    static boolean cryptoActive = false;
    static boolean isHost = false;
    // end crypto vars
    static JFrame frame;
    static JMenuBar menuBar;
    static JMenu commandMenu;
    static JMenu usersMenu;
    static JMenu usersSubMenu;
    static JMenu creditsMenu;
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
    static DatagramSocket discoveryPort;
    static ArrayList<String> hosts = new ArrayList<>();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());


        } catch (Exception e) {
            new Thread(() -> JOptionPane.showMessageDialog(frame, "System Theme not supported, using fallback theme")).start();
            useFallbackTheme = true;
        }

        Fonts.initialize();

        windowInit();
    }

    private static void windowInit() {
        frame = new ChatFrame("ChatClient");

        frame.setMinimumSize(new Dimension(525,452));
        frame.setPreferredSize(new Dimension(525,452));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(IMAGE);

        // menu bar init
        menuBar = new JMenuBar();

        // start command menu
        commandMenu = new JMenu("Commands");

        JMenuItem helpCommand = new JMenuItem("Help");
        helpCommand.addActionListener(e -> sendToServer(Identifier.MESSAGE, "/help"));
        commandMenu.add(helpCommand);

        JMenuItem nicknameCommand = new JMenuItem("Set Nickname");
        nicknameCommand.addActionListener(e -> {
            String nick = JOptionPane.showInputDialog(frame, "Enter new nickname (3-20 chars): ");
            if (connected) {
                sendToServer(Identifier.MESSAGE, "/nickname " + nick);
            } else {
                nickname = nick;
            }
        });
        commandMenu.add(nicknameCommand);

        JMenuItem quitCommand = new JMenuItem("Quit");
        quitCommand.addActionListener(e -> stopClient());
        commandMenu.add(quitCommand);
        // end command menu

        // start online users menu
        usersMenu = new JMenu("Users");
        usersSubMenu = new JMenu("List");

        JMenuItem refreshButton = new JMenuItem("Refresh");
        refreshButton.addActionListener(e -> {
            if (connected) {
                sendToServer(Identifier.INFO_REQUEST, NetDataUtil.ONLINE_REQUEST);
            } else if (nickname != null) {
                usersSubMenu.removeAll();
                usersSubMenu.add(nickname);
            }
        });
        usersMenu.add(refreshButton);

        usersMenu.add(usersSubMenu);
        // end online users menu
        // start credits menu
        creditsMenu = new JMenu("Credits");
        // following section written by <a href="https://stackoverflow.com/users/145574/pstanton">pstanton</a>
        // html content
        JEditorPane creditsPane = new JEditorPane("text/html", "<html>Code written by Rae Johnston, sourced in part from Michiel De May,<br>" +
                "<a href=\"https://stackoverflow.com/users/992484/madprogrammer\">MadProgrammer</a>, <a href=\"https://stackoverflow.com/users/145574/pstanton\">pstanton</a>, " +
                "Philip Danner, Rob Camick, and JavaFX PlatformUtil. <br> Fonts (m3x6 and m5x7) made by <a href=\"https://daniellinssen.games\">Daniel Linssen</a>.<br>" +
                "Sound effects from <a href=\"https://soundcloud.com/sescini/melodic-1\">Melodic 1 - SoundCloud</a>.</html>");
        creditsPane.setFont(Fonts.m5x7(20));

        // handle link events
        creditsPane.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI()); // roll your own link launcher or use Desktop if J6+
                    } catch (IOException | URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        creditsPane.setEditable(false);
        // end of section contributed by pstanton

        JMenuItem credits = new JMenuItem("Show Credits");
        credits.addActionListener(e ->
                JOptionPane.showMessageDialog(frame, creditsPane, "Credits", JOptionPane.INFORMATION_MESSAGE)
        );
        creditsMenu.add(credits);

        // end credits menu

        menuBar.add(commandMenu);
        menuBar.add(usersMenu);
        menuBar.add(creditsMenu);
        Arrays.stream(menuBar.getComponents()).forEach(menu -> menu.setFont(Fonts.m3x6(24)));

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
                    ChatClient.PlaySound.setMuted(muted);
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
                sendToServer(Identifier.MESSAGE, text);
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

                        myChatClient.close();
                        clearCrypto();
                        isHost = false;
                        connected = false;

                        setMainMenu(true); // disconnect
                        disconnectButton.requestFocusInWindow();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame,ex.toString(),"Disconnect Error",JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    if (hostname == null || hostname.replaceAll(" ","").isEmpty() || port < 1) return;
                    try {
                        myChatClient.startConnection(hostname, port);

                        setMainMenu(false); // connect

                        connected = true;
                        if (!cryptoActive) {
                            initializeCrypto(); // join remote host
                        }

                        disconnectButton.setText((ChatServer.isServerStarted() ? "Stop Server and " : "") + DISCONNECT);
                        textArea.setText("Connected to " + hostname + " on port " + port);
                        addText("Use \"/help\" to get a list of valid commands from the server");
                        if (nickname != null) {
                            sendToServer(Identifier.MESSAGE, "/nickname " + nickname);
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
                isHost = false;
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
                                isHost = true;
                                initializeCrypto(); // start local host
                                ChatServer.SERVER.start(port, cryptoPassword, cryptoKey);
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
                            isHost = false;
                            initializeCrypto(); // found conflicting host
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

    /**
     * get password by user input
     */
    private static void initializeCrypto() {
        try {
            String userPass;
            if (serverNeedsPass) {
                userPass = JOptionPane.showInputDialog(frame, "Please input a password");
                if (userPass == null || userPass.isEmpty()) {
                    userPass = AESUtil.STANDARD_PASSWORD;
                }
            } else {
                userPass = AESUtil.STANDARD_PASSWORD;
            }
            cryptoPassword = userPass;
            cryptoKey = AESUtil.getStandardKeyFromPassword(userPass);
            cryptoIv = AESUtil.generateIv();
            cryptoActive = true;
            if (serverNeedsPass && !isHost) {
                expectServerResponse(userPass);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
    private static void clearCrypto() {
        cryptoActive = false;
        cryptoPassword = null;
        cryptoKey = null;
        cryptoIv = null;
    }
    private static void expectServerResponse(String password) {
        sendToServerWithDefault(Identifier.MESSAGE, password);
        expectingServerPasswordResponse = true;
    }
    private static void setupTextArea(boolean notInit) {
        // begin messaging panel
        textArea = new JTextArea();
        textArea.setFont(Fonts.m5x7(20));
        textArea.setForeground(Color.white);
        textArea.setRows(14);
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
     * @return false if port is valid (within 1024 and 49151, inclusive), true otherwise
     */
    private static boolean validatePortSelection() {
        String txt = inputPortNumber();
        if (txt == null || txt.isEmpty() || isInvalidPort(txt)) {
            return true;
        }
        port = Integer.parseInt(txt);
        return port < 1024 || port > 49151;
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
        }
    }

    public static void sendToServer(Identifier type, String msg) {
        try {
            myChatClient.sendPacket(type, msg, cryptoKey, cryptoIv);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void sendToServerWithDefault(Identifier type, String msg) {
        try {
            myChatClient.sendPacket(type, msg, AESUtil.getStandardKey(), cryptoIv);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            discoveryPort = new DatagramSocket(port);
            discoveryPort.setBroadcast(true);

            byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
                discoveryPort.send(sendPacket);
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
                        discoveryPort.send(sendPacket);
                    } catch (Exception ignored) {}

                    System.out.println(Client.class.getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            System.out.println(Client.class.getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[256];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            discoveryPort.setSoTimeout(2500);
            String message;
            do {
                discoveryPort.receive(receivePacket);
                message = new String(receivePacket.getData()).trim();
            } while (
                    !AESUtil.NO_PASS.equals(message)
                    && !AESUtil.NEEDS_PASS.equals(message)
            );

            //We have a response
            System.out.println(Client.class.getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            message = new String(receivePacket.getData()).trim();
            if (message.equals(AESUtil.NO_PASS)) {
                //DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
                serverIPs.add(receivePacket.getAddress().getHostAddress());
                serverNeedsPass = false;
            } else if (message.equals(AESUtil.NEEDS_PASS)) {
                serverIPs.add(receivePacket.getAddress().getHostAddress());
                serverNeedsPass = true;
            }

            //Close the port!
            discoveryPort.close();
        } catch (IOException ex) {
            if (ex.getClass().equals(BindException.class)) {
                showAlertMessage("Port already in use by another application", "Port Busy", JOptionPane.INFORMATION_MESSAGE);
            }
            System.out.println("error in discovery process " + ex);
            if (discoveryPort != null) {
                discoveryPort.close();
            }
        }
        return serverIPs;
    }

    public static void close() throws IOException {
        if (ChatServer.isServerStarted()) {
            ChatServer.SERVER.stop();
        }
        if (connected) {
            myChatClient.close();
        }
    }
}