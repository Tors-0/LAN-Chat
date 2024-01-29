package io.github.Tors_0.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SysTrayToast {
    static Image icon = Toolkit.getDefaultToolkit().createImage(SysTrayToast.class.getResource("/io/github/Tors_0/client/resources/lanchat.png"));
    static SystemTray tray = SystemTray.getSystemTray();
    static TrayIcon trayIcon = new TrayIcon(icon,"LAN-Chat");
    static {
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.frame.setState(JFrame.NORMAL);
                Main.frame.toFront();
            }
        });

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
