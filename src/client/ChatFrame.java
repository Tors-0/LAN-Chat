package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ChatFrame extends JFrame {
    public ChatFrame() throws HeadlessException {
        super();
    }

    public ChatFrame(GraphicsConfiguration gc) {
        super(gc);
    }

    public ChatFrame(String title) throws HeadlessException {
        super(title);
    }

    public ChatFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            try {
                Client.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        super.processWindowEvent(e);
    }
}
