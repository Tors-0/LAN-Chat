package io.github.Tors_0.client;

import java.awt.*;

public class SysTrayToast {
    static Image icon = Toolkit.getDefaultToolkit().createImage(SysTrayToast.class.getResource("/io/github/Tors_0/client/resources/icon.png"));
    static SystemTray tray = SystemTray.getSystemTray();
    public static void display(String message) throws AWTException {
        TrayIcon trayIcon = new TrayIcon(icon,"LAN-Chat");

        trayIcon.setImageAutoSize(true);

        tray.add(trayIcon);

        trayIcon.displayMessage("LAN-Chat",message, TrayIcon.MessageType.INFO);
    }
}
