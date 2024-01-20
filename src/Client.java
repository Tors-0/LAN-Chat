import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;

public class Client implements Closeable {
    static Networking myNetCon;
    static Scanner scanner = new Scanner(System.in);
    static JFrame frame;
    static JTextField msgField;
    static JLabel msgLabel;
    static int port;
    static JLabel portLabel;
    static JTextField portSelectField;
    static String hostname;
    static JLabel hostLabel;
    static JTextField hostField;
    static JButton connectButton;
    static JTextArea textArea;
    static JScrollPane scrollableTextArea;
    static JPanel chatPane;
    static JPanel configPane;

    static boolean connected = false;

    public static void main(String[] args) {
        myNetCon = new Networking();

        windowInit();
    }

    private static void windowInit() {
        frame = new JFrame("ChatClient");
        frame.setMinimumSize(new Dimension(600,465));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // begin messaging panel
        textArea = new JTextArea("",25,60);
        textArea.setSize(500,230);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
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
        portLabel = new JLabel("Current port: " + port);

        portSelectField = new JTextField(5);
        portSelectField.setSize(50,1);
        portSelectField.setCaretColor(Color.white);

        hostLabel = new JLabel("Current host: " + hostname);

        hostField = new JTextField(25);
        hostField.setCaretColor(Color.white);

        connectButton = new JButton("Connect");

        configPane = new JPanel();
        configPane.setLayout(new BoxLayout(configPane,BoxLayout.X_AXIS));
        configPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        configPane.setBackground(Color.darkGray);

        configPane.add(portLabel);
        configPane.add(Box.createRigidArea(new Dimension(5,0)));
        configPane.add(portSelectField);
        configPane.add(Box.createRigidArea(new Dimension(5,0)));
        configPane.add(hostLabel);
        configPane.add(Box.createRigidArea(new Dimension(5,0)));
        configPane.add(hostField);
        configPane.add(Box.createRigidArea(new Dimension(5,0)));
        configPane.add(connectButton);

        for (Component comp : configPane.getComponents()) {
            comp.setBackground(Color.gray);
            comp.setForeground(Color.white);
        }

        Container contentPane = frame.getContentPane();
        contentPane.add(chatPane, BorderLayout.NORTH);
        contentPane.add(configPane, BorderLayout.SOUTH);
        contentPane.setBackground(Color.darkGray);

        frame.setBackground(Color.darkGray);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

        Action msgAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = msgField.getText();
                msgField.setText("");
                if (text.contains("stop")) {
                    System.out.println("stopping client...");
                    System.exit(0);
                }
                sendToServer(text);
            }
        };
        msgField.addActionListener(msgAction);
        Action portAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (portSelectField.getText().isEmpty()) return; // cancel if fields not present
                port = Integer.parseInt(portSelectField.getText());
                portSelectField.setText("");

                portLabel.setText("Current port: " + port);
            }
        };
        portSelectField.addActionListener(portAction);
        Action hostAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hostField.getText().isEmpty()) return; // cancel if fields not present
                hostname = hostField.getText();
                hostField.setText("");

                hostLabel.setText("Current host: " + hostname);
            }
        };
        hostField.addActionListener(hostAction);
        Action connectAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hostname == null || hostname.isEmpty() || port < 1 || connected) return;
                try {
                    myNetCon.startConnection(hostname,port);
                    connectButton.setEnabled(false);
                    addText("Connected to " + hostname + " on port " + port);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame,ex.toString(),"Connect Failed",JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        connectButton.addActionListener(connectAction);
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

    @Override
    public void close() throws IOException {
        myNetCon.close();
    }
}