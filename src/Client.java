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

        clientLoop();
    }

    private static void windowInit() {
        frame = new JFrame("ChatClient");
        frame.setSize(500,250);
        frame.setMinimumSize(new Dimension(500,250));
        textField = new JTextField(60);
        label = new JLabel();
        frame.add(label);
        frame.add(textField);
        frame.setVisible(true);
        textField.setVisible(true);
        label.setVisible(true);
        label.setSize(500,230);
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
        label.setText(label.getText() + txt);
    }
    private static void clientLoop() {
        System.out.print("Send a message: ");
        String userInput = scanner.nextLine();
        while (true) {
            if (userInput.contains("stop")) {
                System.out.println("stopping client...");
                System.exit(0);
            }
            sendToServer(userInput);
            System.out.print("Send a message: ");
            userInput = scanner.nextLine();
        }
    }

    public static void sendToServer(String msg) {
        try {
            myNetCon.sendMsg(msg);
        } catch (Exception e) {
            System.out.println("got exception: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        myNetCon.close();
    }
}