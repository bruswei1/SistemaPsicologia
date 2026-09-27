package Vista;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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

    private static final int TAMANO_INSIGNIA = 84;
    private static final float ESCALA_HOVER = 1.06f;
    private static final float PASO_ESCALA = 0.015f;

    private final Icono icono;
    private final Tema.Acento acento;
    private final String titulo;
    private final String descripcion;
    private boolean resaltada = false;
    private float escala = 1f;
    private Timer timerEscala;
    private float alphaEntrada = 1f;

    public TarjetaMenu(Icono icono, Tema.Acento acento, String titulo, String descripcion, Runnable alHacerClic) {
        this(icono, acento, titulo, descripcion, alHacerClic, 0);
    }

    /**
     * @param indiceEntrada posición de esta tarjeta entre sus hermanas (0, 1, 2...): cada una
     *                      arranca su animación de entrada (fade + un leve crecimiento) un poco
     *                      después que la anterior, en vez de que las tres aparezcan de golpe al
     *                      mismo tiempo al abrir el menú.
     */
    public TarjetaMenu(Icono icono, Tema.Acento acento, String titulo, String descripcion, Runnable alHacerClic,
            int indiceEntrada) {
        this.icono = icono;
        this.acento = acento;
        this.titulo = titulo;
        this.descripcion = descripcion;

        setBackground(Tema.SUPERFICIE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);

        alphaEntrada = 0f;
        escala = 0.94f;
        Timer entrada = new Timer(15, null);
        entrada.setInitialDelay(indiceEntrada * 80);
        final long[] inicioAnim = {0L};
        entrada.addActionListener(e -> {
            if (inicioAnim[0] == 0L) {
                inicioAnim[0] = System.currentTimeMillis();
            }
            float t = Math.min(1f, (System.currentTimeMillis() - inicioAnim[0]) / 320f);
            float avance = 1f - (float) Math.pow(1f - t, 3); // ease-out cúbico
            alphaEntrada = avance;
            escala = 0.94f + 0.06f * avance;
            repaint();
            if (t >= 1f) {
                alphaEntrada = 1f;
                escala = 1f;
                ((Timer) e.getSource()).stop();
            }
        });
        entrada.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                resaltada = true;
                animarEscalaHacia(ESCALA_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                resaltada = false;
                animarEscalaHacia(1f);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                alHacerClic.run();
            }
        });
    }

    /** Anima `escala` hacia `objetivo` de a pasos chicos, repintando en cada paso — un "pop" suave en vez de un salto brusco. */
    private void animarEscalaHacia(float objetivo) {
        if (timerEscala != null) {
            timerEscala.stop();
        }
        timerEscala = new Timer(12, e -> {
            if (Math.abs(escala - objetivo) <= PASO_ESCALA) {
                escala = objetivo;
                timerEscala.stop();
            } else {
                escala += objetivo > escala ? PASO_ESCALA : -PASO_ESCALA;
            }
            repaint();
        });
        timerEscala.start();
    }

    /** Tamaño de referencia si algún layout lo consulta; MenuPrincipal usa GridLayout, que estira
     * la tarjeta a todo el espacio disponible en vez de respetar este valor. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(420, 380);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (alphaEntrada < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alphaEntrada)));
        }

        int w = getWidth();
        int h = getHeight();

        // Escala centrada en el propio panel: al pasar el cursor, toda la tarjeta "crece"
        // levemente sobre su propio centro (efecto lupa/pop), sin mover el layout de las
        // tarjetas vecinas ya que el tamaño que Swing usa para posicionarlas no cambia.
        if (escala != 1f) {
            g2.translate(w / 2.0, h / 2.0);
            g2.scale(escala, escala);
            g2.translate(-w / 2.0, -h / 2.0);
        }

        g2.setColor(Tema.SUPERFICIE);
        g2.fillRoundRect(0, 0, w, h, 20, 20);

        // Franja de color arriba: ancla la tarjeta a su categoría (mismo acento que la insignia)
        // sin pintarla entera, y le da un primer plano de color a una tarjeta que si no es toda
        // blanca. Recortada a la forma redondeada de la tarjeta para que no se salga por las
        // esquinas.
        Graphics2D gFranja = (Graphics2D) g2.create();
        gFranja.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 20, 20));
        gFranja.setPaint(new java.awt.GradientPaint(0, 0, acento.icono.brighter(), w, 0, acento.icono));
        gFranja.fillRect(0, 0, w, 6);
        gFranja.dispose();

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

        // Centrado (horizontal y vertical) en vez de coordenadas fijas desde la esquina: con
        // GridLayout estirando la tarjeta a toda la pantalla, el alto real varía según el
        // tamaño de la ventana — si el contenido quedara pegado arriba, una tarjeta enorme se
        // vería con todo el texto amontonado arriba y un espacio vacío gigante abajo.
        Font fuenteTitulo = Tema.fuente(Font.BOLD, 30);
        Font fuenteDescripcion = Tema.fuente(Font.PLAIN, 18);
        FontMetrics fmTitulo = g2.getFontMetrics(fuenteTitulo);
        FontMetrics fmDescripcion = g2.getFontMetrics(fuenteDescripcion);
        String[] lineas = descripcion.split("\n");

        int espacioIconoTitulo = 40;
        int espacioTituloDescripcion = 22;
        int altoLinea = fmDescripcion.getHeight();
        int altoContenido = TAMANO_INSIGNIA + espacioIconoTitulo + fmTitulo.getHeight()
            + espacioTituloDescripcion + altoLinea * lineas.length;

        int y = (h - altoContenido) / 2;

        int insigniaX = (w - TAMANO_INSIGNIA) / 2;

        // Sombra propia de la insignia, no solo un cuadrado plano: un óvalo difuminado corrido
        // hacia abajo simula profundidad sin necesitar un blur real (Java2D no trae uno barato).
        for (int i = 8; i >= 1; i--) {
            g2.setColor(new Color(acento.icono.getRed(), acento.icono.getGreen(), acento.icono.getBlue(), 3));
            g2.fillRoundRect(insigniaX - i, y - i + 6, TAMANO_INSIGNIA + i * 2, TAMANO_INSIGNIA + i * 2, 24, 24);
        }

        // Degradé radial (claro arriba-izquierda, el tono de acento abajo-derecha) en vez de
        // relleno plano — es lo que hace que un cuadrado se lea como un botón con volumen.
        g2.setPaint(new java.awt.RadialGradientPaint(
            new java.awt.geom.Point2D.Float(insigniaX + TAMANO_INSIGNIA * 0.3f, y + TAMANO_INSIGNIA * 0.25f),
            TAMANO_INSIGNIA * 0.9f,
            new float[]{0f, 1f},
            new Color[]{Tema.aclarar(acento.icono, 0.35f), acento.icono}
        ));
        g2.fillRoundRect(insigniaX, y, TAMANO_INSIGNIA, TAMANO_INSIGNIA, 20, 20);
        icono.dibujar(g2, insigniaX + 14, y + 14, TAMANO_INSIGNIA - 28, Color.WHITE);
        y += TAMANO_INSIGNIA + espacioIconoTitulo;

        g2.setColor(Tema.TEXTO_PRIMARIO);
        g2.setFont(fuenteTitulo);
        g2.drawString(titulo, (w - fmTitulo.stringWidth(titulo)) / 2, y + fmTitulo.getAscent());
        y += fmTitulo.getHeight() + espacioTituloDescripcion;

        g2.setColor(Tema.TEXTO_SECUNDARIO);
        g2.setFont(fuenteDescripcion);
        for (String linea : lineas) {
            g2.drawString(linea, (w - fmDescripcion.stringWidth(linea)) / 2, y + fmDescripcion.getAscent());
            y += altoLinea;
        }

        g2.dispose();
    }
}
