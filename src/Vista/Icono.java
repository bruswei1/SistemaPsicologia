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
    },

    GUARDAR {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int x1 = x + (int) (size * 0.16), y1 = y + (int) (size * 0.52);
            int x2 = x + (int) (size * 0.42), y2 = y + (int) (size * 0.78);
            int x3 = x + (int) (size * 0.86), y3 = y + (int) (size * 0.22);
            g2.drawLine(x1, y1, x2, y2);
            g2.drawLine(x2, y2, x3, y3);
        }
    },

    NUEVO {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = x + size / 2;
            int cy = y + size / 2;
            int r = (int) (size * 0.32);
            g2.drawLine(cx - r, cy, cx + r, cy);
            g2.drawLine(cx, cy - r, cx, cy + r);
        }
    },

    ELIMINAR {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            int w = (int) (size * 0.56);
            int h = (int) (size * 0.50);
            int px = x + (size - w) / 2;
            int py = y + (int) (size * 0.36);
            g2.fillRoundRect(px, py, w, h, (int) (size * 0.08), (int) (size * 0.08));
            g2.fillRect(x + (int) (size * 0.20), y + (int) (size * 0.24), (int) (size * 0.60), (int) (size * 0.08));
            g2.fillRoundRect(x + (int) (size * 0.36), y + (int) (size * 0.12), (int) (size * 0.28), (int) (size * 0.12), 4, 4);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1.2f, size * 0.035f)));
            for (int i = 0; i < 3; i++) {
                int lx = px + (int) (w * (0.25 + i * 0.25));
                g2.drawLine(lx, py + (int) (h * 0.2), lx, py + (int) (h * 0.8));
            }
        }
    },

    BUSCAR {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int r = (int) (size * 0.28);
            int cx = x + (int) (size * 0.40);
            int cy = y + (int) (size * 0.40);
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            int hx1 = cx + (int) (r * 0.7);
            int hy1 = cy + (int) (r * 0.7);
            g2.drawLine(hx1, hy1, x + (int) (size * 0.88), y + (int) (size * 0.88));
        }
    },

    LIMPIAR {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int d = (int) (size * 0.64);
            int px = x + (size - d) / 2;
            int py = y + (size - d) / 2;
            g2.drawArc(px, py, d, d, 30, 280);

            double anguloRad = Math.toRadians(30);
            int puntaX = px + d / 2 + (int) (d / 2 * Math.cos(anguloRad));
            int puntaY = py + d / 2 - (int) (d / 2 * Math.sin(anguloRad));
            int[] fx = {puntaX - (int) (size * 0.10), puntaX + (int) (size * 0.12), puntaX + (int) (size * 0.02)};
            int[] fy = {puntaY - (int) (size * 0.10), puntaY - (int) (size * 0.02), puntaY + (int) (size * 0.10)};
            g2.fillPolygon(fx, fy, 3);
        }
    },

    VOLVER {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = y + size / 2;
            g2.drawLine(x + (int) (size * 0.80), cy, x + (int) (size * 0.20), cy);
            g2.drawLine(x + (int) (size * 0.20), cy, x + (int) (size * 0.44), cy - (int) (size * 0.24));
            g2.drawLine(x + (int) (size * 0.20), cy, x + (int) (size * 0.44), cy + (int) (size * 0.24));
        }
    },

    EDITAR {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + (int) (size * 0.22), y + (int) (size * 0.78), x + (int) (size * 0.68), y + (int) (size * 0.32));
            int[] px = {x + (int) (size * 0.68), x + (int) (size * 0.84), x + (int) (size * 0.78), x + (int) (size * 0.62)};
            int[] py = {y + (int) (size * 0.32), y + (int) (size * 0.18), y + (int) (size * 0.12), y + (int) (size * 0.26)};
            g2.fillPolygon(px, py, 4);
            int[] tx = {x + (int) (size * 0.16), x + (int) (size * 0.28), x + (int) (size * 0.22)};
            int[] ty = {y + (int) (size * 0.88), y + (int) (size * 0.82), y + (int) (size * 0.70)};
            g2.fillPolygon(tx, ty, 3);
        }
    },

    EXPORTAR {
        public void dibujar(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            int w = (int) (size * 0.62);
            int h = (int) (size * 0.72);
            int px = x + (size - w) / 2;
            int py = y + (int) (size * 0.06);
            g2.fillRoundRect(px, py, w, h, (int) (size * 0.08), (int) (size * 0.08));

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1.2f, size * 0.035f)));
            for (int i = 0; i < 2; i++) {
                int ly = py + (int) (h * (0.30 + i * 0.22));
                g2.drawLine(px + (int) (w * 0.20), ly, px + (int) (w * 0.80), ly);
            }

            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(1.6f, size * 0.06f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = x + size / 2;
            int ay1 = py + h + (int) (size * 0.02);
            int ay2 = py + h + (int) (size * 0.18);
            g2.drawLine(cx, ay1, cx, ay2);
            g2.drawLine(cx - (int) (size * 0.08), ay2 - (int) (size * 0.08), cx, ay2);
            g2.drawLine(cx + (int) (size * 0.08), ay2 - (int) (size * 0.08), cx, ay2);
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
