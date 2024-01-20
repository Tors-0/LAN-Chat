import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;

public class Client implements Closeable {
    static Networking myNetCon;
    static Scanner scanner = new Scanner(System.in);
    static JFrame frame;
    static JTextField textField;
    static JLabel label;
    static JTextArea textArea;
    static JScrollPane scrollableTextArea;

    public static void main(String[] args) {
        myNetCon = new Networking();
        System.out.print("Enter host ip/address: ");
        String host = scanner.nextLine();
        System.out.print("Enter server port: ");
        int port = Integer.parseInt(scanner.nextLine());
        try {
            myNetCon.startConnection(host,port);
            sendToServer("connection established");
        } catch (IOException e) {
            System.out.println("got error: " + e.getMessage());
            System.exit(0);
        }

        windowInit();
    }

    private static void windowInit() {
        frame = new JFrame("ChatClient");
        frame.setMinimumSize(new Dimension(600,465));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea("",25,60);
        textArea.append("ChatClient window open...");
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

        label = new JLabel("Send a message:");
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setForeground(Color.white);

        textField = new JTextField(60);
        textField.setForeground(Color.white);
        textField.setBackground(Color.gray);
        textField.setCaretColor(Color.white);
        textField.setVisible(true);

        Container contentPane = frame.getContentPane();
        JPanel chatPane = new JPanel();
        chatPane.setLayout(new BoxLayout(chatPane,BoxLayout.Y_AXIS));
        chatPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        chatPane.setBackground(Color.darkGray);

        chatPane.add(scrollableTextArea);
        chatPane.add(Box.createRigidArea(new Dimension(0,5)));
        chatPane.add(label);
        chatPane.add(textField);

        contentPane.add(chatPane, BorderLayout.NORTH);
        contentPane.setBackground(Color.darkGray);

        frame.setBackground(Color.darkGray);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);

        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                textField.setText("");
                if (text.contains("stop")) {
                    System.out.println("stopping client...");
                    System.exit(0);
                }
                sendToServer(text);
            }
        };
        textField.addActionListener(action);
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