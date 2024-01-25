package io.github.Tors_0.util;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Fonts {
    public static final Font H1 = new Font(Font.SERIF, Font.PLAIN, 25);
    private static Font M3x6;
    private static Font M5x7;

    public static void initialize() {
        try {
            InputStream is = Fonts.class.getResourceAsStream("/io/github/Tors_0/resources/m3x6.ttf");
            M3x6 = Font.createFont(Font.TRUETYPE_FONT, is);
            is = Fonts.class.getResourceAsStream("/io/github/Tors_0/resources/m5x7.ttf");
            M5x7 = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(Fonts.class.getName()).log(Level.SEVERE, "Font loading error", ex);
        }
    }
    public static Font m3x6(float size) {
        return M3x6.deriveFont(size);
    }
    public static Font m5x7(float size) {
        return M5x7.deriveFont(size);
    }
}
