import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;

public class Client implements Closeable {
    static Networking myNetCon;
    static Scanner scanner = new Scanner(System.in);
    static String testMessage = "Hello World!";

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