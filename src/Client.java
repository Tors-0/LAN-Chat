import java.io.IOException;
import java.util.Scanner;

public class Client {
    static Networking myNetCon;
    static Scanner scanner = new Scanner(System.in);
    static String testMessage = "Hello World!";

    public static void main(String[] args) {
        myNetCon = new Networking();
        try {
            myNetCon.startConnection("127.0.0.1",6666);
        } catch (IOException e) {
            System.out.println("got error: " + e.getMessage());
        }
        System.out.print("Send a message: ");
        String userInput = scanner.nextLine();
        while (true) {
            sendToServer(userInput);
            if (userInput.contains("stop")) {
                System.out.println("send stop to server and stopping client...");
                try{
                    myNetCon.close();
                }catch (IOException e) {
                    System.out.println("got error while closing client: " + e.getMessage());
                }
                break;
            }
            System.out.print("Send a message: ");
            userInput = scanner.nextLine();
        }
    }
    public static void sendToServer(String msg) {
        try {
            String resp = myNetCon.sendMsg(msg);
            System.out.println("got response: " + resp);
        } catch (Exception e) {
            System.out.println("got exception: " + e.getMessage());
        }
    }
}