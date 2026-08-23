package Vista;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tarjeta clickeable con hover, usada para las opciones del menú principal.
 * Reutilizable por cualquier pantalla que necesite el mismo patrón de navegación.
 */
public class TarjetaMenu extends JPanel {

    private static final int TAMANO_INSIGNIA = 48;

    private final Icono icono;
    private final Tema.Acento acento;
    private final String titulo;
    private final String descripcion;
    private boolean resaltada = false;

    public TarjetaMenu(Icono icono, Tema.Acento acento, String titulo, String descripcion, Runnable alHacerClic) {
        this.icono = icono;
        this.acento = acento;
        this.titulo = titulo;
        this.descripcion = descripcion;

        setBackground(Tema.SUPERFICIE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                resaltada = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                resaltada = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                alHacerClic.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.setColor(Tema.SUPERFICIE);
        g2.fillRoundRect(0, 0, w, h, 20, 20);

        if (resaltada) {
            for (int i = 3; i >= 1; i--) {
                g2.setColor(new Color(0, 0, 0, 5 * i));
                g2.drawRoundRect(i, i, w - 2 * i - 1, h - 2 * i - 1, 20, 20);
            }
            g2.setColor(acento.icono);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 20, 20);
        } else {
            g2.setColor(Tema.BORDE);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
        }

        int insigniaX = 24;
        int insigniaY = 22;
        g2.setColor(acento.insignia);
        g2.fillRoundRect(insigniaX, insigniaY, TAMANO_INSIGNIA, TAMANO_INSIGNIA, 14, 14);
        icono.dibujar(g2, insigniaX + 8, insigniaY + 8, TAMANO_INSIGNIA - 16, acento.icono);

        int textoX = 24;
        int textoY = insigniaY + TAMANO_INSIGNIA + 26;

        g2.setColor(Tema.TEXTO_PRIMARIO);
        g2.setFont(Tema.SUBTITULO);
        g2.drawString(titulo, textoX, textoY);

        g2.setColor(Tema.TEXTO_SECUNDARIO);
        g2.setFont(Tema.TEXTO);
        String[] lineas = descripcion.split("\n");
        int y = textoY + 26;
        for (String linea : lineas) {
            g2.drawString(linea, textoX, y);
            y += 22;
        }
    }
}
