package Vista;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Iconos vectoriales simples dibujados a mano con Java2D.
 * Se usan en vez de emojis porque Java 8 (javac.source=1.8, ver CLAUDE.md)
 * no soporta glifos de emoji a color en Graphics2D: se ven como cuadros vacíos.
 */
public enum Icono {

    PACIENTES {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            int headD = (int) (size * 0.34);
            g2.fillOval(x + (size - headD) / 2, y + (int) (size * 0.08), headD, headD);
            int bodyW = (int) (size * 0.62);
            int bodyH = (int) (size * 0.38);
            g2.fillRoundRect(x + (size - bodyW) / 2, y + (int) (size * 0.52), bodyW, bodyH, bodyH, bodyH);
        }
    },

    TURNOS {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            dibujarCalendarioBase(g2, x, y, size, color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.09f)));
            int cx = x + size / 2;
            int cy = y + (int) (size * 0.66);
            int r = (int) (size * 0.13);
            g2.drawLine(cx - r, cy, cx + r, cy);
            g2.drawLine(cx, cy - r, cx, cy + r);
        }
    },

    SESIONES {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            int w = (int) (size * 0.82);
            int h = (int) (size * 0.60);
            int px = x + (size - w) / 2;
            int py = y + (int) (size * 0.10);
            int arc = (int) (size * 0.16);
            g2.fillRoundRect(px, py, w, h, arc, arc);

            int[] colaX = {px + (int) (w * 0.22), px + (int) (w * 0.40), px + (int) (w * 0.22)};
            int[] colaY = {py + h, py + h, py + h + (int) (size * 0.18)};
            g2.fillPolygon(colaX, colaY, 3);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.045f)));
            for (int i = 0; i < 3; i++) {
                int ly = py + (int) (h * (0.30 + i * 0.24));
                int lw = (i == 2) ? (int) (w * 0.35) : (int) (w * 0.62);
                g2.drawLine(px + (int) (w * 0.16), ly, px + (int) (w * 0.16) + lw, ly);
            }
        }
    },

    HISTORIA {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            int tabW = (int) (size * 0.34);
            int tabH = (int) (size * 0.14);
            int bodyY = y + (int) (size * 0.30);
            g2.fillRoundRect(x + (int) (size * 0.10), y + (int) (size * 0.20), tabW, tabH, tabH, tabH);
            g2.fillRoundRect(x + (int) (size * 0.10), bodyY, (int) (size * 0.80), (int) (size * 0.54), (int) (size * 0.08), (int) (size * 0.08));
        }
    },

    DASHBOARD {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            int barW = (int) (size * 0.18);
            int baseY = y + (int) (size * 0.86);
            int arc = (int) (barW * 0.4);
            g2.fillRoundRect(x + (int) (size * 0.14), baseY - (int) (size * 0.34), barW, (int) (size * 0.34), arc, arc);
            g2.fillRoundRect(x + (int) (size * 0.41), baseY - (int) (size * 0.58), barW, (int) (size * 0.58), arc, arc);
            g2.fillRoundRect(x + (int) (size * 0.68), baseY - (int) (size * 0.22), barW, (int) (size * 0.22), arc, arc);
        }
    },

    AGENDA {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            dibujarCalendarioBase(g2, x, y, size, color);
            g2.setStroke(new BasicStroke(Math.max(1.6f, size * 0.07f)));
            int cx = x + (int) (size * 0.5);
            int cy = y + (int) (size * 0.66);
            int r = (int) (size * 0.15);
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            g2.drawLine(cx, cy, cx, cy - (int) (r * 0.7));
            g2.drawLine(cx, cy, cx + (int) (r * 0.6), cy);
        }
    };

    public abstract void dibujar(Graphics2D g2, int x, int y, int size, Color color);

    private static void dibujarCalendarioBase(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        int w = (int) (size * 0.80);
        int h = (int) (size * 0.72);
        int px = x + (size - w) / 2;
        int py = y + (int) (size * 0.18);
        g2.fillRoundRect(px, py, w, h, (int) (size * 0.14), (int) (size * 0.14));

        g2.setColor(Color.WHITE);
        g2.fillRect(px, py, w, (int) (size * 0.16));

        g2.setColor(color);
        int tabW = Math.max(2, (int) (size * 0.06));
        int tabH = (int) (size * 0.18);
        g2.fillRoundRect(px + (int) (w * 0.20), y + (int) (size * 0.04), tabW, tabH, tabW, tabW);
        g2.fillRoundRect(px + w - (int) (w * 0.20) - tabW, y + (int) (size * 0.04), tabW, tabH, tabW, tabW);
    }
}
