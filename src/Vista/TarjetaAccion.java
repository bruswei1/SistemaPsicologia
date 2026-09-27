package Vista;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Acción rápida grande y autoexplicativa: insignia con ícono + título + una línea que dice qué
 * hace. Pensada para las acciones que antes eran un botón más entre muchos (exportar, abrir una
 * carpeta, respaldar) y costaba encontrarlas. Más compacta que {@link TarjetaMenu} (que ocupa
 * media pantalla): varias entran en una fila.
 *
 * Se puede usar con teclado: es enfocable (Tab) y se activa con Enter o Espacio.
 */
public class TarjetaAccion extends JPanel {

    private final Tema.Acento acento;
    private final Runnable accion;
    private boolean resaltada;
    private boolean habilitada = true;

    public TarjetaAccion(Icono icono, Tema.Acento acento, String titulo, String descripcion, Runnable accion) {
        super(new BorderLayout(12, 0));
        this.acento = acento;
        this.accion = accion;

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        setToolTipText(descripcion);

        JComponent insignia = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight());
                g2.setColor(Tema.esOscuro() ? Tema.oscurecer(acento.icono, 0.45f) : acento.insignia);
                g2.fillOval(0, (getHeight() - d) / 2, d, d);
                int tam = (int) (d * 0.52);
                icono.dibujar(g2, (d - tam) / 2, (getHeight() - tam) / 2, tam,
                    Tema.esOscuro() ? Tema.aclarar(acento.icono, 0.35f) : acento.icono);
                g2.dispose();
            }
        };
        insignia.setPreferredSize(new Dimension(44, 44));
        add(insignia, BorderLayout.WEST);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Tema.fuente(Font.BOLD, 14));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);

        JLabel lblDescripcion = new JLabel("<html>" + descripcion + "</html>");
        lblDescripcion.setFont(Tema.TEXTO_CHICO);
        lblDescripcion.setForeground(Tema.TEXTO_SECUNDARIO);

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblDescripcion);
        add(textos, BorderLayout.CENTER);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                resaltada = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Al pasar del panel a una etiqueta hija también llega un "exited": solo se
                // apaga el resaltado si el mouse salió de verdad de la tarjeta.
                if (!contains(javax.swing.SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), TarjetaAccion.this))) {
                    resaltada = false;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                ejecutar();
            }
        };
        addMouseListener(mouse);
        insignia.addMouseListener(mouse);
        lblTitulo.addMouseListener(mouse);
        lblDescripcion.addMouseListener(mouse);
        textos.addMouseListener(mouse);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
        registerKeyboardAction(e -> ejecutar(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_FOCUSED);
        registerKeyboardAction(e -> ejecutar(), KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), WHEN_FOCUSED);
    }

    /** Deshabilitada (p. ej. mientras corre la acción): se ve atenuada y no responde al clic. */
    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
        setCursor(Cursor.getPredefinedCursor(habilitada ? Cursor.HAND_CURSOR : Cursor.WAIT_CURSOR));
        repaint();
    }

    private void ejecutar() {
        if (habilitada) {
            accion.run();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        Color fondo = Tema.SUPERFICIE;
        if (resaltada && habilitada) {
            fondo = Tema.mezclar(Tema.SUPERFICIE, acento.icono, Tema.esOscuro() ? 0.18f : 0.08f);
        }
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);

        Color borde = isFocusOwner() || (resaltada && habilitada) ? acento.icono : Tema.BORDE;
        g2.setColor(borde);
        g2.setStroke(new java.awt.BasicStroke(isFocusOwner() ? 2f : 1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (habilitada) {
            super.paintChildren(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.45f));
        super.paintChildren(g2);
        g2.dispose();
    }
}
