package io.github.Tors_0.client;

import java.awt.*;

public class SysTrayToast {
    static Image icon = Toolkit.getDefaultToolkit().createImage(SysTrayToast.class.getResource("/io/github/Tors_0/client/resources/lanchat.png"));
    static SystemTray tray = SystemTray.getSystemTray();
    static TrayIcon trayIcon = new TrayIcon(icon,"LAN-Chat");
    static {
        trayIcon.setImageAutoSize(true);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
    public static void display(String message) {
        trayIcon.displayMessage("LAN-Chat",message, TrayIcon.MessageType.INFO);
    }
}
