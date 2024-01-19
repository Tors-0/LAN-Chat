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
        frame.setSize(600,450);
        frame.setMinimumSize(new Dimension(600,450));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textField = new JTextField(60);
        textField.setVisible(true);

        textArea = new JTextArea("",25,60);
        textArea.append("ChatClient window open...");
        textArea.setVisible(true);
        textArea.setSize(500,230);
        textArea.setLineWrap(true);

        Container contentPane = frame.getContentPane();
        JPanel chatPane = new JPanel();
        chatPane.setLayout(new BoxLayout(chatPane,BoxLayout.Y_AXIS));

        chatPane.add(textArea);
        chatPane.add(Box.createRigidArea(new Dimension(0,5)));
        chatPane.add(textField);

        contentPane.add(chatPane, BorderLayout.NORTH);
        frame.setVisible(true);

        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                sendToServer(text);
                textField.setText("");
                if (text.contains("stop")) {
                    System.out.println("stopping client...");
                    System.exit(0);
                }
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