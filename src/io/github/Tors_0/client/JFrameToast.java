package io.github.Tors_0.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

class JFrameToast extends JFrame {
    static int width = 150;
    static int xPos = Toolkit.getDefaultToolkit().getScreenSize().width - width;
    static int height = 50;
    static int yPos = Toolkit.getDefaultToolkit().getScreenSize().height - height;
    static JLabel hostnameLabel;
    static JLabel messageLabel;
    public JFrameToast() {
        setUndecorated(true);
        setLayout(new GridBagLayout());
        setLocationRelativeTo(null);
        setSize(150, 50);
        setFocusable(false);
        setAutoRequestFocus(false);
        setIconImage(Client.imageIcon);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        hostnameLabel = new JLabel("from: " + Client.hostname,JLabel.LEFT);
        hostnameLabel.setFont(Font.getFont(Font.MONOSPACED));
        panel.add(hostnameLabel);

        messageLabel = new JLabel("default message :)",JLabel.LEFT);
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
            messageLabel.setText(message);

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