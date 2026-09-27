package Vista;

import javax.swing.*;
import java.awt.*;

/**
 * Panel lateral que entra deslizándose desde la derecha (el mismo patrón que "Nuevo / Editar
 * estudiante"): una tarjeta de ancho fijo con título, subtítulo, X para cerrar, un cuerpo con
 * scroll y un pie fijo de botones. Se agrega en el EAST de un BorderLayout y empieza oculto.
 *
 * La animación ensancha el contenedor de 0 al ancho final (ease-out, 220 ms) sin deformar el
 * contenido, que tiene ancho fijo y se va descubriendo.
 */
public class PanelDeslizable extends JPanel {

    private final int ancho;
    private final JComponent tarjeta;
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblSubtitulo = new JLabel();
    private final JScrollPane scrollCuerpo;
    private int anchoActual;
    private boolean cerrando;
    private Timer animacion;

    /**
     * @param alTocarCerrar qué hacer con la X (normalmente el "cerrar" de la pantalla, que puede
     *                      preguntar antes si hay cambios sin guardar)
     */
    public PanelDeslizable(int ancho, JComponent cuerpo, JComponent pie, Runnable alTocarCerrar) {
        super(null);
        this.ancho = ancho;
        setOpaque(false);
        setVisible(false);

        JPanel lateral = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.SUPERFICIE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.setPaint(new GradientPaint(0, 0, Tema.SECUNDARIO, getWidth(), 0, Tema.PRIMARIO));
                g2.fillRect(0, 0, getWidth(), 5);
                g2.dispose();
            }
        };
        lateral.setOpaque(false);
        lateral.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);
        encabezado.setBorder(BorderFactory.createEmptyBorder(16, 22, 12, 12));
        lblTitulo.setFont(Tema.fuente(Font.BOLD, 19));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        lblSubtitulo.setFont(Tema.TEXTO_CHICO);
        lblSubtitulo.setForeground(Tema.TEXTO_SECUNDARIO);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblSubtitulo);
        encabezado.add(textos, BorderLayout.CENTER);
        JButton btnX = Tema.botonIcono(Icono.CERRAR, "Cerrar (Esc)");
        btnX.addActionListener(e -> alTocarCerrar.run());
        JPanel envX = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        envX.setOpaque(false);
        envX.add(btnX);
        encabezado.add(envX, BorderLayout.EAST);
        lateral.add(encabezado, BorderLayout.NORTH);

        JPanel envCuerpo = new PanelAnchoDelScroll();
        envCuerpo.setOpaque(false);
        envCuerpo.setLayout(new BorderLayout());
        envCuerpo.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 18));
        envCuerpo.add(cuerpo, BorderLayout.NORTH);
        scrollCuerpo = new JScrollPane(envCuerpo);
        scrollCuerpo.setBorder(BorderFactory.createEmptyBorder());
        scrollCuerpo.setOpaque(false);
        scrollCuerpo.getViewport().setOpaque(false);
        scrollCuerpo.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollCuerpo.getVerticalScrollBar().setUnitIncrement(16);
        lateral.add(scrollCuerpo, BorderLayout.CENTER);

        JPanel envPie = new JPanel(new BorderLayout());
        envPie.setOpaque(false);
        envPie.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
            BorderFactory.createEmptyBorder(10, 18, 14, 14)));
        envPie.add(pie, BorderLayout.CENTER);
        lateral.add(envPie, BorderLayout.SOUTH);

        tarjeta = lateral;
        add(tarjeta);
    }

    public void setTitulo(String titulo, String subtitulo) {
        lblTitulo.setText(titulo);
        lblSubtitulo.setText(subtitulo != null ? subtitulo : " ");
    }

    public boolean estaAbierto() {
        return isVisible() && !cerrando;
    }

    /** Lo muestra (animado) y vuelve el scroll del cuerpo arriba. */
    public void abrir() {
        scrollCuerpo.getVerticalScrollBar().setValue(0);
        if (estaAbierto()) {
            return;
        }
        cerrando = false;
        setVisible(true);
        animar(ancho, null);
    }

    public void cerrar() {
        if (!isVisible()) {
            return;
        }
        cerrando = true;
        animar(0, () -> {
            setVisible(false);
            cerrando = false;
        });
    }

    private void animar(int objetivo, Runnable alTerminar) {
        if (animacion != null) {
            animacion.stop();
        }
        int inicial = anchoActual;
        long inicio = System.nanoTime();
        animacion = new Timer(8, null);
        animacion.addActionListener(e -> {
            float t = Math.min(1f, (System.nanoTime() - inicio) / 220_000_000f);
            float avance = 1f - (float) Math.pow(1f - t, 3);
            anchoActual = Math.round(inicial + (objetivo - inicial) * avance);
            revalidate();
            if (getParent() != null) {
                getParent().repaint();
            }
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                if (alTerminar != null) {
                    alTerminar.run();
                }
            }
        });
        animacion.start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(anchoActual, 0);
    }

    @Override
    public void doLayout() {
        tarjeta.setBounds(0, 0, ancho, getHeight());
    }

    /** Dentro del scroll toma el ancho visible (solo scroll vertical). */
    private static class PanelAnchoDelScroll extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
