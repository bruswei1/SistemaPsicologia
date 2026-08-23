package Vista;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Adapta un {@link Icono} (dibujado con Graphics2D) a javax.swing.Icon, para usarlo en JButton/JLabel. */
public class IconoSwing implements Icon {

    private final Icono icono;
    private final Color color;
    private final int size;

    public IconoSwing(Icono icono, Color color, int size) {
        this.icono = icono;
        this.color = color;
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icono.dibujar(g2, x, y, size, color);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
