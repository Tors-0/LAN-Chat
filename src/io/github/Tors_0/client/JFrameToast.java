package io.github.Tors_0.client;

import io.github.Tors_0.util.Fonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

class JFrameToast extends JFrame {
    static int width = 200;
    static int xPos = Toolkit.getDefaultToolkit().getScreenSize().width - width;
    static int height = 65;
    static int yPos = Toolkit.getDefaultToolkit().getScreenSize().height - height;
    static JLabel hostnameLabel;
    static JLabel messageLabel;

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (WindowEvent.WINDOW_ACTIVATED == e.getID()) {
            Main.frame.setState(JFrame.NORMAL);
            Main.frame.toFront();
            this.setVisible(false);
        }

        super.processWindowEvent(e);
    }

    public JFrameToast() {
        setUndecorated(true);
        setLayout(new GridBagLayout());
        setLocationRelativeTo(null);
        setSize(width, height);
        setAutoRequestFocus(false);
        setIconImage(Main.IMAGE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        hostnameLabel = new JLabel("from: " + Main.hostname,JLabel.LEFT);
        hostnameLabel.setFont(Fonts.m3x6(25));
        panel.add(hostnameLabel);

        messageLabel = new JLabel("default message :)",JLabel.LEFT);
        messageLabel.setFont(Fonts.m5x7(20));
        panel.add(messageLabel);

        add(panel);

        setLocation(xPos,yPos);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new  RoundRectangle2D.Double(0,0,getWidth(),
                        getHeight(), 20, 20));
            }
        });
    }

    public void display(String message) {
        try {
            messageLabel.setText(message.length() > 20 ? message.substring(0,20) + "..." : message);

            setOpacity(1);
            setVisible(true);
            setAlwaysOnTop(true);
            Thread.sleep(2000);

            //hide the toast message in slow motion
            for (double d = 1.0; d > 0.2; d -= 0.1) {
                Thread.sleep(100);
                setOpacity((float)d);
            }

            // set the visibility to false
            setVisible(false);
        }catch (Exception e) {
            System.out.println(e.getMessage() + " from toast");
        }
    }
}