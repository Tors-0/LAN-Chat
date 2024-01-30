package io.github.Tors_0.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ChatFrame extends JFrame {

    public ChatFrame(String title) throws HeadlessException {
        super(title);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            try {
                Client.close();
            } catch (IOException ignored) {}
        }

        super.processWindowEvent(e);
    }
}
