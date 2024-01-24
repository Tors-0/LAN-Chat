package io.github.Tors_0.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

class Toast extends JFrame {
    static int width = 150;
    static int xPos = Toolkit.getDefaultToolkit().getScreenSize().width - width;
    static int height = 50;
    static int yPos = Toolkit.getDefaultToolkit().getScreenSize().height - height;
    public Toast(final String message) {
        setUndecorated(true);
        setLayout(new GridBagLayout());
        setBackground(new Color(240,240,240,250));
        setLocationRelativeTo(null);
        setSize(150, 50);
        setFocusable(false);
        setAutoRequestFocus(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        panel.add(new JLabel(Client.hostname,JLabel.LEFT));
        JLabel label = new JLabel(message,JLabel.LEFT);
        label.setFont(Font.getFont(Font.SERIF));
        panel.add(label);

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

    public void display() {
        try {
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