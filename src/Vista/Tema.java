package Vista;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.JTable;
import javax.swing.JLayeredPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.BoxLayout;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.KeyEvent;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;

/**
 * Paleta y tipografía centralizadas para todas las pantallas Swing.
 * Objetivo: que ninguna pantalla vuelva a declarar sus propios
 * new Color(...)/new Font(...) sueltos.
 *
 * Los colores de superficie/texto no son `final`: {@link #alternarModoOscuro()}
 * los reemplaza para soportar modo oscuro. Una pantalla ya abierta no se
 * redibuja sola al alternar: hay que recrearla (como ya hace la navegación
 * del menú con dispose()+new).
 */
public final class Tema {

    private Tema() {
    }

    private static boolean oscuro = false;

    /**
     * Avisos "toast" actualmente visibles por ventana, para apilarlos en vez de superponerlos.
     * WeakHashMap a propósito: DetallePaciente abre una ventana nueva por cada estudiante que se
     * mira, y con un HashMap común cada una quedaría referenciada acá para siempre aunque se
     * cerrara — una fuga de memoria lenta en una sesión larga con muchos estudiantes abiertos.
     */
    private static final java.util.Map<Window, java.util.List<JWindow>> TOASTS_ACTIVOS = new java.util.WeakHashMap<>();

    // Colores de superficie/texto (varían con el modo oscuro)
    public static Color FONDO;
    public static Color SUPERFICIE;
    public static Color BORDE;
    public static Color BORDE_SUAVE;
    public static Color TEXTO_PRIMARIO;
    public static Color TEXTO_SECUNDARIO;

    // Colores de acción. Paleta tomada del cartel real del Departamento de Orientación y
    // Psicología del colegio: violeta institucional profundo como color primario, azul
    // institucional como secundario/de marca, y verde-azulado para éxito (funcional, no de
    // marca). A diferencia de antes, SÍ varían con el modo oscuro: la versión oscura usa tonos
    // más claros/saturados de la misma familia — el violeta/azul "profundo" de modo claro se ve
    // apagado sobre un fondo ya oscuro, así que se aclaran para mantener el mismo contraste e
    // impacto visual en los dos modos (se fijan en {@link #aplicarPaleta()}).
    public static Color PRIMARIO;
    public static Color PRIMARIO_CLARO;
    public static Color SECUNDARIO;
    public static Color EXITO;
    public static Color PELIGRO;
    public static final Color NEUTRO = new Color(206, 212, 218);

    /** Pares de color (insignia suave + ícono sólido) para diferenciar categorías en tarjetas. */
    public static Acento ACENTO_AZUL;
    public static Acento ACENTO_MORADO;
    public static final Acento ACENTO_VERDE = new Acento(new Color(209, 250, 229), new Color(5, 150, 105));
    public static final Acento ACENTO_AMBAR = new Acento(new Color(254, 240, 199), new Color(202, 113, 6));
    public static final Acento ACENTO_CELESTE = new Acento(new Color(224, 231, 250), new Color(74, 109, 201));
    public static final Acento ACENTO_ROSA = new Acento(new Color(255, 224, 235), new Color(219, 39, 119));
    public static final Acento ACENTO_GRIS = new Acento(new Color(226, 230, 236), new Color(71, 85, 105));

    public static final class Acento {
        public final Color insignia;
        public final Color icono;

        public Acento(Color insignia, Color icono) {
            this.insignia = insignia;
            this.icono = icono;
        }
    }

    // Tipografía
    private static final String FAMILIA = "Segoe UI";
    public static final Font TITULO = new Font(FAMILIA, Font.BOLD, 28);
    public static final Font SUBTITULO = new Font(FAMILIA, Font.BOLD, 20);
    public static final Font TEXTO = new Font(FAMILIA, Font.PLAIN, 14);
    public static final Font TEXTO_CHICO = new Font(FAMILIA, Font.PLAIN, 12);
    public static final Font TEXTO_ITALICA_CHICA = new Font(FAMILIA, Font.ITALIC, 10);
    public static final Font BOTON = new Font(FAMILIA, Font.BOLD, 12);

    /** Fuente de marca en un estilo/tamaño puntual no cubierto por las constantes de arriba. */
    public static Font fuente(int estilo, int tamano) {
        return new Font(FAMILIA, estilo, tamano);
    }

    // Espaciado
    public static final int ESPACIADO_CHICO = 10;
    public static final int ESPACIADO_MEDIANO = 20;
    public static final int ESPACIADO_GRANDE = 40;

    static {
        aplicarPaleta();
    }

    public static boolean esOscuro() {
        return oscuro;
    }

    private static final String ARCHIVO_PREFERENCIA = "tema.properties";

    /**
     * Instala el Look & Feel respetando la preferencia guardada en disco (si existe).
     * Hay que llamar a esto en cada main(), en lugar de FlatLightLaf.setup() a secas,
     * para que la app arranque con el tema que el usuario dejó la última vez.
     */
    public static void instalarLookAndFeelGuardado() {
        oscuro = leerPreferenciaOscuro();
        if (oscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        aplicarEsquinasRedondeadas();
        aplicarPaleta();
    }

    /**
     * Alterna modo oscuro: cambia el Look & Feel real de FlatLaf (no solo la paleta propia),
     * para que campos de texto, combos, spinners, scrollbars y diálogos —que no pintamos
     * nosotros— también se oscurezcan en vez de quedar blancos. Solo afecta a ventanas
     * creadas de ahí en más: una ya abierta hay que recrearla (dispose()+new).
     */
    public static void alternarModoOscuro() {
        oscuro = !oscuro;
        if (oscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        aplicarEsquinasRedondeadas();
        FlatLaf.updateUI();
        aplicarPaleta();
        guardarPreferenciaOscuro();
    }

    /**
     * Redondea globalmente botones, campos, combos y scrollbars vía las propiedades de FlatLaf
     * (hay que llamarla después de cada `FlatLightLaf`/`FlatDarkLaf.setup()`, que resetea los
     * defaults de UIManager). Sin esto, todos los componentes nativos de Swing/FlatLaf quedan con
     * esquina recta por default, lo que desentona con las tarjetas/paneles pintados a mano que sí
     * usamos con esquina redondeada en toda la app.
     */
    private static void aplicarEsquinasRedondeadas() {
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("CheckBox.arc", 4);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ProgressBar.arc", 999);
    }

    private static boolean leerPreferenciaOscuro() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(ARCHIVO_PREFERENCIA)) {
            props.load(in);
            return Boolean.parseBoolean(props.getProperty("oscuro", "false"));
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static void guardarPreferenciaOscuro() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("oscuro", String.valueOf(oscuro));
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(ARCHIVO_PREFERENCIA)) {
            props.store(out, "Preferencia de tema - Depto. de Psicologia, Colegio San Roque Gonzalez");
        } catch (java.io.IOException e) {
            System.out.println("No se pudo guardar la preferencia de tema: " + e.getMessage());
        }
    }

    private static void aplicarPaleta() {
        if (oscuro) {
            // Toma los colores reales del tema oscuro de FlatLaf para que nuestros
            // paneles/tarjetas pintados a mano combinen con los componentes nativos.
            FONDO = colorOMenos("Panel.background", new Color(30, 31, 34));
            SUPERFICIE = colorOMenos("TextField.background", new Color(43, 45, 48));
            BORDE = colorOMenos("Component.borderColor", new Color(82, 86, 90));
            BORDE_SUAVE = colorOMenos("Separator.foreground", new Color(60, 63, 67));
            TEXTO_PRIMARIO = colorOMenos("Label.foreground", new Color(230, 230, 230));
            TEXTO_SECUNDARIO = colorOMenos("Label.disabledForeground", new Color(150, 150, 150));

            // Mismos matices violeta/azul/verde/rojo institucionales, aclarados: sobre un fondo
            // ya oscuro, la versión "profunda" de modo claro se ve apagada y pierde el impacto
            // que sí tiene sobre blanco.
            PRIMARIO = new Color(139, 124, 216);
            PRIMARIO_CLARO = new Color(154, 140, 224);
            SECUNDARIO = new Color(109, 147, 219);
            EXITO = new Color(63, 203, 168);
            PELIGRO = new Color(227, 112, 112);
        } else {
            FONDO = new Color(246, 245, 251);
            SUPERFICIE = new Color(255, 255, 255);
            BORDE = new Color(226, 230, 236);
            BORDE_SUAVE = new Color(236, 239, 243);
            TEXTO_PRIMARIO = new Color(30, 35, 45);
            TEXTO_SECUNDARIO = new Color(108, 117, 130);

            PRIMARIO = new Color(75, 58, 140);
            PRIMARIO_CLARO = new Color(91, 72, 163);
            SECUNDARIO = new Color(47, 90, 168);
            EXITO = new Color(13, 148, 118);
            PELIGRO = new Color(196, 58, 58);
        }

        ACENTO_AZUL = new Acento(oscuro ? new Color(58, 74, 99) : new Color(221, 231, 249), SECUNDARIO);
        ACENTO_MORADO = new Acento(oscuro ? new Color(58, 50, 92) : new Color(228, 222, 247), PRIMARIO);
    }

    /** Mezcla `color` hacia blanco en la proporción `fraccion` (0=sin cambio, 1=blanco puro). Útil
     * para el punto claro de un degradé radial sobre insignias/tarjetas de estadística. */
    public static Color aclarar(Color color, float fraccion) {
        int r = (int) (color.getRed() + (255 - color.getRed()) * fraccion);
        int g = (int) (color.getGreen() + (255 - color.getGreen()) * fraccion);
        int b = (int) (color.getBlue() + (255 - color.getBlue()) * fraccion);
        return new Color(r, g, b);
    }

    /** Mezcla `color` hacia negro en la proporción `fraccion` (0=sin cambio, 1=negro puro). Para el estado "presionado" de un botón. */
    public static Color oscurecer(Color color, float fraccion) {
        int r = (int) (color.getRed() * (1 - fraccion));
        int g = (int) (color.getGreen() * (1 - fraccion));
        int b = (int) (color.getBlue() * (1 - fraccion));
        return new Color(r, g, b);
    }

    /** Interpola entre `a` y `b` (incluido el canal alfa) en la proporción `t` (0=a, 1=b). Para animaciones de color. */
    public static Color mezclar(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }

    private static Color colorOMenos(String claveUIManager, Color porDefecto) {
        Color c = UIManager.getColor(claveUIManager);
        return c != null ? c : porDefecto;
    }

    public static JButton botonPrimario(String texto) {
        return boton(texto, PRIMARIO, Color.WHITE, null, true);
    }

    public static JButton botonPrimario(String texto, Icono icono) {
        return boton(texto, PRIMARIO, Color.WHITE, icono, true);
    }

    public static JButton botonExito(String texto) {
        return boton(texto, EXITO, Color.WHITE, null, true);
    }

    public static JButton botonExito(String texto, Icono icono) {
        return boton(texto, EXITO, Color.WHITE, icono, true);
    }

    public static JButton botonPeligro(String texto) {
        return boton(texto, PELIGRO, Color.WHITE, null, true);
    }

    public static JButton botonPeligro(String texto, Icono icono) {
        return boton(texto, PELIGRO, Color.WHITE, icono, true);
    }

    /**
     * Botón "tonal": relleno plano suave (tinte de {@link #PRIMARIO}) en vez del contorno de línea
     * que tenía antes — mismo criterio que "Cancelar" al lado de "Guardar" en apps modernas: se
     * nota que acompaña a la acción principal sin competir con ella ni quedar un bloque sólido.
     */
    public static JButton botonSecundario(String texto) {
        return boton(texto, colorTonal(), PRIMARIO, null, false);
    }

    public static JButton botonSecundario(String texto, Icono icono) {
        return boton(texto, colorTonal(), PRIMARIO, icono, false);
    }

    private static Color colorTonal() {
        return new Color(PRIMARIO.getRed(), PRIMARIO.getGreen(), PRIMARIO.getBlue(), oscuro ? 45 : 26);
    }

    /**
     * Botón con esquinas redondeadas, pintado a mano: los "sólidos" (primario/éxito/peligro)
     * llevan un leve degradé y una sombra de su propio color abajo, como una tarjeta elevada; los
     * "tonales" (secundario) son un relleno plano suave, sin sombra, para no competir con la
     * acción principal de la pantalla. El hover no es un cambio de color a los saltos: un Timer
     * anima una transición suave hacia el color de hover y de vuelta al normal.
     */
    private static JButton boton(String texto, Color fondoBase, Color letra, Icono icono, boolean solidoConSombra) {
        final float[] progresoHover = {0f};
        final Timer[] animacionHover = {null};

        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int alturaSombra = solidoConSombra ? 3 : 0;
                int h = getHeight() - alturaSombra;
                int arco = 10;

                Color colorHover = solidoConSombra ? aclarar(fondoBase, 0.10f)
                    : new Color(fondoBase.getRed(), fondoBase.getGreen(), fondoBase.getBlue(),
                        Math.min(255, fondoBase.getAlpha() + 22));

                Color base;
                if (!isEnabled()) {
                    // Tono apagado derivado del tema actual: antes era un gris claro fijo, que en
                    // modo oscuro quedaba como un bloque brillante y en modo claro dejaba el
                    // texto blanco casi ilegible.
                    base = mezclar(SUPERFICIE, TEXTO_SECUNDARIO, oscuro ? 0.14f : 0.12f);
                } else if (getModel().isPressed()) {
                    base = solidoConSombra ? oscurecer(fondoBase, 0.12f)
                        : new Color(fondoBase.getRed(), fondoBase.getGreen(), fondoBase.getBlue(),
                            Math.min(255, fondoBase.getAlpha() + 45));
                } else {
                    base = mezclar(fondoBase, colorHover, progresoHover[0]);
                }

                if (solidoConSombra && isEnabled()) {
                    g2.setColor(new Color(fondoBase.getRed(), fondoBase.getGreen(), fondoBase.getBlue(), 90));
                    g2.fillRoundRect(0, alturaSombra, w, h, arco, arco);
                    g2.setPaint(new GradientPaint(0, 0, aclarar(base, 0.15f), 0, h, base));
                } else {
                    g2.setColor(base);
                }
                g2.fillRoundRect(0, 0, w, h, arco, arco);
                g2.dispose();

                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBackground(fondoBase);
        b.setForeground(letra);
        b.setFont(BOTON);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(9, 18, solidoConSombra ? 12 : 9, 18));
        if (icono != null) {
            b.setIcon(new IconoSwing(icono, letra, 16));
            b.setDisabledIcon(new IconoSwing(icono, colorDeshabilitado(), 16));
            b.setIconTextGap(8);
        }

        // Anima progresoHover hacia 1 (mouse encima) o 0 (mouse afuera) en vez de aplicar el
        // color de hover de golpe. Un solo Timer por botón, que se detiene solo al llegar.
        b.getModel().addChangeListener(e -> {
            if (animacionHover[0] != null && animacionHover[0].isRunning()) {
                return;
            }
            animacionHover[0] = new Timer(12, null);
            animacionHover[0].addActionListener(ev -> {
                float objetivo = b.getModel().isRollover() ? 1f : 0f;
                float actual = progresoHover[0];
                float nuevo = actual + (objetivo - actual) * 0.25f;
                if (Math.abs(objetivo - nuevo) < 0.02f) {
                    nuevo = objetivo;
                    ((Timer) ev.getSource()).stop();
                }
                progresoHover[0] = nuevo;
                b.repaint();
            });
            animacionHover[0].start();
        });

        return b;
    }

    /**
     * Encabezado de marca para el tope de una pantalla: franja con degradé
     * {@link #SECUNDARIO} → {@link #PRIMARIO} (mismos colores del cartel real del Departamento),
     * título en blanco y subtítulo opcional en blanco semi-transparente. Reemplaza el header
     * blanco liso que cada pantalla armaba a mano.
     */
    /**
     * `JPanel` liso con el degradé de marca {@link #SECUNDARIO} → {@link #PRIMARIO} pintado de
     * fondo (mismos colores del cartel real del Departamento), sin contenido propio. Base
     * compartida de {@link #panelEncabezado(String, String)} y de cualquier otra franja/hero con
     * degradé — evita reimplementar el `paintComponent` con `GradientPaint` en cada pantalla.
     */
    public static JPanel panelDegradado() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                int w = Math.max(1, getWidth());
                int h = Math.max(1, getHeight());
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Degradé diagonal de 3 paradas en vez de 2 puntas rectas: el tono intermedio
                // (PRIMARIO_CLARO) le da profundidad en vez de una transición lineal plana.
                g2.setPaint(new java.awt.LinearGradientPaint(
                    0, 0, w, h,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{SECUNDARIO, PRIMARIO_CLARO, PRIMARIO}
                ));
                g2.fillRect(0, 0, w, h);

                // Resplandor suave arriba a la derecha, como el reflejo de luz de una superficie
                // pulida — rompe la uniformidad del degradé sin agregar otro color a la paleta.
                float radio = w * 0.9f;
                g2.setPaint(new java.awt.RadialGradientPaint(
                    new java.awt.geom.Point2D.Float(w, 0), radio,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, 40), new Color(255, 255, 255, 0)}
                ));
                g2.fillRect(0, 0, w, h);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel panelEncabezado(String titulo, String subtitulo) {
        JPanel panel = panelDegradado();
        panel.setPreferredSize(new Dimension(0, subtitulo != null && !subtitulo.isEmpty() ? 92 : 72));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(TITULO);
        lblTitulo.setForeground(Color.WHITE);
        textos.add(lblTitulo);

        if (subtitulo != null && !subtitulo.isEmpty()) {
            JLabel lblSubtitulo = new JLabel(subtitulo);
            lblSubtitulo.setFont(TEXTO);
            lblSubtitulo.setForeground(new Color(255, 255, 255, 215));
            textos.add(lblSubtitulo);
        }

        panel.add(textos, BorderLayout.WEST);

        if (hayLogo()) {
            boolean conSubtitulo = subtitulo != null && !subtitulo.isEmpty();
            panel.add(insigniaLogo(conSubtitulo ? 44 : 36), BorderLayout.EAST);
        }

        return panel;
    }

    /**
     * "Tarjeta" con sombra suave y esquinas redondeadas — reemplazo más moderno de un panel con
     * {@link #tituloSeccion(String)} (borde de línea recta) para las pantallas ya migradas al
     * nuevo tratamiento visual. El título va como texto en versalita arriba del contenido, no
     * metido en el borde. El llamador solo agrega su contenido en BorderLayout.CENTER (o SOUTH).
     */
    public static JPanel panelTarjeta(String titulo) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arco = 16;

                // Sombra en capas cada vez más tenues hacia afuera, en vez de un solo rectángulo
                // gris — mismo criterio que ya usa TarjetaEstadistica en este mismo archivo.
                for (int i = 8; i >= 1; i--) {
                    g2.setColor(new Color(20, 20, 30, 3));
                    g2.fillRoundRect(0, i, w, h - i, arco, arco);
                }
                g2.setColor(SUPERFICIE);
                g2.fillRoundRect(0, 0, w, h - 6, arco, arco);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(18, 20, 22, 20));

        JLabel lblTitulo = new JLabel(titulo.toUpperCase());
        lblTitulo.setFont(new Font(FAMILIA, Font.BOLD, 12));
        lblTitulo.setForeground(TEXTO_SECUNDARIO);
        tarjeta.add(lblTitulo, BorderLayout.NORTH);

        return tarjeta;
    }

    /** Cambia el título de una tarjeta creada con {@link #panelTarjeta(String)} (ej. "Citas" → "Citas del 12/09/2026"). */
    public static void actualizarTituloTarjeta(JPanel tarjeta, String nuevoTitulo) {
        Component norte = ((BorderLayout) tarjeta.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (norte instanceof JLabel) {
            ((JLabel) norte).setText(nuevoTitulo.toUpperCase());
        }
    }

    /** `TitledBorder` con tipografía/color de marca, para reemplazar `BorderFactory.createTitledBorder(texto)` suelto. */
    public static TitledBorder tituloSeccion(String texto) {
        TitledBorder borde = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDE, 1), texto);
        borde.setTitleFont(new Font(FAMILIA, Font.BOLD, 13));
        borde.setTitleColor(PRIMARIO);
        return borde;
    }

    /** Borde normal para un campo de formulario. */
    public static Border bordeCampo() {
        return BorderFactory.createLineBorder(BORDE, 1);
    }

    /** Borde rojo para marcar un campo con error de validación. */
    public static Border bordeCampoError() {
        return BorderFactory.createLineBorder(PELIGRO, 2);
    }

    /** Aplica/quita el borde de error según `valido`, dejando el campo listo para revalidar. */
    public static void marcarError(JComponent campo, boolean valido) {
        campo.setBorder(valido ? bordeCampo() : bordeCampoError());
    }

    /**
     * Aplica fuente, alto de fila y zebra striping consistentes a cualquier JTable de la app.
     * El encabezado antes tenía un fondo gris parejo que pesaba casi lo mismo que las filas de
     * datos; ahora es solo texto (mayúscula chica, más silencioso) con una línea fina abajo que
     * separa encabezado de datos sin competir con ellos.
     */
    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(TEXTO_CHICO);
        javax.swing.table.JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setFont(new Font(FAMILIA, Font.BOLD, 11));
        encabezado.setBackground(SUPERFICIE);
        encabezado.setForeground(TEXTO_SECUNDARIO);
        encabezado.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDE));
        encabezado.setDefaultRenderer(new EncabezadoTablaRenderer(encabezado.getDefaultRenderer()));
        tabla.setRowHeight(34);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(new Color(PRIMARIO.getRed(), PRIMARIO.getGreen(), PRIMARIO.getBlue(), 45));
        tabla.setSelectionForeground(TEXTO_PRIMARIO);
        tabla.setDefaultRenderer(Object.class, new ZebraRenderer());
        tabla.setAutoCreateRowSorter(true);

        // Resalta la fila bajo el mouse (antes la única señal de "acá estás" era el cursor, sin
        // ningún cambio visual hasta hacer clic). El color real lo decide ZebraRenderer via el
        // client property "filaHover" que esto mantiene actualizado.
        java.awt.event.MouseAdapter resaltadorHover = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                tabla.putClientProperty("filaHover", tabla.rowAtPoint(e.getPoint()));
                tabla.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                tabla.putClientProperty("filaHover", -1);
                tabla.repaint();
            }
        };
        tabla.addMouseMotionListener(resaltadorHover);
        tabla.addMouseListener(resaltadorHover);
    }

    /**
     * Crea una tabla igual que {@code new JTable(modelo)}, pero que muestra un mensaje centrado y
     * discreto en gris cuando no tiene filas, en vez de quedar en blanco sin ninguna explicación.
     * Llamar a {@link #estilizarTabla} después, como con cualquier otra tabla.
     */
    public static JTable crearTablaConVacio(javax.swing.table.TableModel modelo, String mensajeVacio) {
        // fillsViewportHeight: sin esto, una tabla sin filas mide 0 px de alto y el mensaje
        // nunca se llega a ver (ni se pueden soltar archivos arrastrados sobre el área vacía).
        JTable tabla = new JTable(modelo) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() > 0) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXTO_SECUNDARIO);
                g2.setFont(fuente(Font.ITALIC, 13));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = Math.max(10, (getWidth() - fm.stringWidth(mensajeVacio)) / 2);
                int y = Math.max(fm.getHeight() + 10, getHeight() / 3);
                g2.drawString(mensajeVacio, x, y);
                g2.dispose();
            }
        };
        tabla.setFillsViewportHeight(true);
        return tabla;
    }

    /**
     * Notificación liviana tipo "toast": aparece cerca de la esquina inferior derecha de la
     * ventana y se cierra sola a los pocos segundos, sin bloquear la interfaz ni requerir un clic
     * en "Aceptar" — pensada para confirmaciones de éxito ("Guardado", "Cita eliminada") donde un
     * JOptionPane modal es fricción de más. Errores y advertencias siguen usando JOptionPane: ahí
     * sí conviene que el usuario confirme que los leyó.
     */
    public static void mostrarNotificacion(Component ancla, String mensaje) {
        Window ventana = ancla instanceof Window ? (Window) ancla : SwingUtilities.getWindowAncestor(ancla);
        if (ventana == null || !ventana.isShowing()) {
            return;
        }

        // Progreso de aparición (0 = invisible, 1 = visible del todo): en vez de aparecer y
        // desaparecer de golpe, entra con fade + un leve deslizamiento hacia arriba y se va de la
        // misma forma — un solo Timer que anima todo el ciclo de vida del aviso.
        final float[] progreso = {0f};

        JWindow popup = new JWindow(ventana);
        popup.setBackground(new Color(0, 0, 0, 0));

        Color fondoToast = oscuro ? new Color(60, 62, 78, 235) : new Color(35, 37, 48, 235);
        JPanel contenido = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(fondoToast.getRed(), fondoToast.getGreen(), fondoToast.getBlue(),
                    (int) (fondoToast.getAlpha() * progreso[0])));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        contenido.setOpaque(false);
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        // Un tilde dibujado a mano (no un carácter Unicode "✓"): esa fuente no tiene el glifo y
        // Java lo pinta como un rectángulo vacío ("tofu") en vez del símbolo — mismo motivo por el
        // que los íconos de Icono.java también se dibujan con Graphics2D en vez de usar emojis.
        JComponent tilde = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, (int) (255 * progreso[0])));
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int w = getWidth();
                int h = getHeight();
                g2.drawLine((int) (w * 0.18), (int) (h * 0.52), (int) (w * 0.42), (int) (h * 0.75));
                g2.drawLine((int) (w * 0.42), (int) (h * 0.75), (int) (w * 0.85), (int) (h * 0.25));
                g2.dispose();
            }
        };
        tilde.setPreferredSize(new Dimension(16, 16));
        tilde.setOpaque(false);

        JLabel lbl = new JLabel(mensaje);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(BOTON);

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila.setOpaque(false);
        fila.add(tilde);
        fila.add(lbl);
        contenido.add(fila, BorderLayout.CENTER);

        popup.getContentPane().add(contenido);
        popup.pack();

        // Se apila arriba de cualquier otro aviso que ya esté visible en esta misma ventana, en
        // vez de aparecer siempre en el mismo lugar y taparlo (pasaba con dos guardados seguidos).
        java.util.List<JWindow> activos = TOASTS_ACTIVOS.computeIfAbsent(ventana, k -> new java.util.ArrayList<>());
        int alturaAcumulada = 0;
        for (JWindow otro : activos) {
            alturaAcumulada += otro.getHeight() + 8;
        }
        activos.add(popup);

        Point ubicacion = ventana.getLocationOnScreen();
        int xFinal = ubicacion.x + ventana.getWidth() - popup.getWidth() - 30;
        int yFinal = ubicacion.y + ventana.getHeight() - popup.getHeight() - 40 - alturaAcumulada;
        int desplazamiento = 14;

        final int duracionTotalMs = 2400;
        final int duracionFadeMs = 180;
        final long inicio = System.currentTimeMillis();

        Timer animacion = new Timer(15, null);
        animacion.addActionListener(e -> {
            long transcurrido = System.currentTimeMillis() - inicio;
            float p;
            if (transcurrido < duracionFadeMs) {
                p = transcurrido / (float) duracionFadeMs;
            } else if (transcurrido > duracionTotalMs - duracionFadeMs) {
                p = Math.max(0f, (duracionTotalMs - transcurrido) / (float) duracionFadeMs);
            } else {
                p = 1f;
            }
            progreso[0] = p;
            lbl.setForeground(new Color(255, 255, 255, (int) (255 * p)));
            popup.setLocation(xFinal, yFinal + (int) (desplazamiento * (1f - p)));
            contenido.repaint();

            if (transcurrido >= duracionTotalMs) {
                ((Timer) e.getSource()).stop();
                activos.remove(popup);
                popup.dispose();
            }
        });

        popup.setLocation(xFinal, yFinal + desplazamiento);
        popup.setVisible(true);
        animacion.start();
    }

    /** Duración de la transición entre pantallas. */
    private static final int DURACION_TRANSICION_MS = 300;
    /** Cuánto se desplaza lateralmente cada pantalla durante la transición (en píxeles). */
    private static final int DESPLAZAMIENTO_TRANSICION = 36;

    /** Transición en curso por contenedor, para terminarla si llega otra antes de que acabe. */
    private static final java.util.Map<JPanel, Runnable> TRANSICIONES_EN_CURSO = new java.util.WeakHashMap<>();

    /** Cambia de tarjeta "hacia adelante" (la pantalla nueva entra desde la derecha). */
    public static void cambiarConFundido(java.awt.Window ventana, JPanel contenedor, java.awt.CardLayout layout, String nombreTarjeta) {
        cambiarConFundido(ventana, contenedor, layout, nombreTarjeta, false);
    }

    /**
     * Cambia de tarjeta en un {@link java.awt.CardLayout} con una transición suave en vez del
     * salto instantáneo: fundido cruzado entre una foto de la pantalla actual y una de la nueva,
     * con un leve deslizamiento lateral (la nueva entra desde la derecha al avanzar y desde la
     * izquierda al volver, como en las apps de escritorio modernas).
     *
     * Es por tiempo, no por cuadros: si el equipo se atrasa en un cuadro, el siguiente se dibuja
     * donde corresponde en vez de trabarse, y la duración es siempre la misma. Usa imágenes
     * compatibles con la pantalla (aceleradas por la placa de video) y una curva ease-in-out.
     * La tarjeta real ya queda cambiada debajo desde el primer instante; al terminar, se quita
     * la capa animada.
     *
     * @param haciaAtras true al volver (al menú, a la lista): el sentido del deslizamiento se invierte.
     */
    public static void cambiarConFundido(java.awt.Window ventana, JPanel contenedor, java.awt.CardLayout layout,
                                         String nombreTarjeta, boolean haciaAtras) {
        // Si había otra transición en este contenedor, se termina ya (clics rápidos seguidos).
        Runnable anterior = TRANSICIONES_EN_CURSO.remove(contenedor);
        if (anterior != null) {
            anterior.run();
        }

        int w = contenedor.getWidth();
        int h = contenedor.getHeight();
        JLayeredPane capas = ventana instanceof JFrame ? ((JFrame) ventana).getLayeredPane()
            : ventana instanceof JDialog ? ((JDialog) ventana).getLayeredPane() : null;

        if (w <= 0 || h <= 0 || capas == null || !contenedor.isShowing()) {
            // Ventana todavía no visible (primera vez) o sin JLayeredPane: cambio directo, sin animar.
            layout.show(contenedor, nombreTarjeta);
            return;
        }

        java.awt.GraphicsConfiguration gc = contenedor.getGraphicsConfiguration();
        BufferedImage fotoVieja = fotoDe(contenedor, gc, w, h);

        layout.show(contenedor, nombreTarjeta);
        // Se fuerza el layout de la tarjeta nueva ahora (no en el próximo ciclo) para poder
        // fotografiarla ya armada; si no, la foto saldría vacía o a medio acomodar.
        contenedor.validate();
        BufferedImage fotoNueva = fotoDe(contenedor, gc, w, h);

        Point ubicacion = SwingUtilities.convertPoint(contenedor, 0, 0, capas);
        final float[] progreso = {0f};
        int sentido = haciaAtras ? -1 : 1;
        JComponent capa = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                float t = progreso[0];
                int dx = Math.round(DESPLAZAMIENTO_TRANSICION * t);
                // Vieja: opaca, yéndose hacia el costado. La franja que deja libre se pinta con el
                // fondo. Dibujarla opaca (y solo la nueva con transparencia) da el mismo fundido
                // cruzado con la mitad de mezcla por cuadro.
                int xVieja = -sentido * dx;
                g2.setColor(FONDO);
                if (xVieja < 0) {
                    g2.fillRect(getWidth() + xVieja, 0, -xVieja, getHeight());
                } else if (xVieja > 0) {
                    g2.fillRect(0, 0, xVieja, getHeight());
                }
                g2.drawImage(fotoVieja, xVieja, 0, null);
                // Nueva: llega desde el otro costado mientras aparece encima.
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, t));
                g2.drawImage(fotoNueva, sentido * (DESPLAZAMIENTO_TRANSICION - dx), 0, null);
                g2.dispose();
            }
        };
        capa.setOpaque(true);
        capa.setBounds(ubicacion.x, ubicacion.y, w, h);
        // La capa también frena clics durante los 300 ms (evita tocar dos veces un botón que se
        // está yendo).
        capa.addMouseListener(new java.awt.event.MouseAdapter() { });
        capas.add(capa, JLayeredPane.DRAG_LAYER);
        capa.paintImmediately(0, 0, w, h);

        long inicio = System.nanoTime();
        Timer animacion = new Timer(8, null);
        Runnable terminar = () -> {
            animacion.stop();
            if (capa.getParent() != null) {
                capas.remove(capa);
                capas.repaint(ubicacion.x, ubicacion.y, w, h);
            }
            TRANSICIONES_EN_CURSO.remove(contenedor);
        };
        TRANSICIONES_EN_CURSO.put(contenedor, terminar);
        animacion.addActionListener(e -> {
            float lineal = Math.min(1f, (System.nanoTime() - inicio) / (DURACION_TRANSICION_MS * 1_000_000f));
            // ease-in-out cúbico: arranca y termina suave, sin frenazos.
            progreso[0] = lineal < 0.5f
                ? 4f * lineal * lineal * lineal
                : 1f - (float) Math.pow(-2f * lineal + 2f, 3) / 2f;
            if (lineal >= 1f) {
                terminar.run();
                return;
            }
            capa.paintImmediately(0, 0, w, h);
            java.awt.Toolkit.getDefaultToolkit().sync();
        });
        animacion.start();
    }

    /** Foto del contenedor en una imagen compatible con la pantalla (se dibuja más rápido). */
    private static BufferedImage fotoDe(JComponent c, java.awt.GraphicsConfiguration gc, int w, int h) {
        BufferedImage foto = gc != null
            ? gc.createCompatibleImage(w, h, java.awt.Transparency.OPAQUE)
            : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = foto.createGraphics();
        g.setColor(FONDO);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        return foto;
    }

    /** Envuelve el renderer de encabezado real de Swing solo para pasar el texto a mayúscula. */
    private static class EncabezadoTablaRenderer implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer base;

        EncabezadoTablaRenderer(javax.swing.table.TableCellRenderer base) {
            this.base = base;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            String texto = value != null ? String.valueOf(value).toUpperCase() : "";
            return base.getTableCellRendererComponent(table, texto, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Marca en rojo (con {@link #marcarError}) un campo de texto mientras el usuario escribe,
     * si su contenido no matchea `patron`. A propósito NO bloquea ni filtra caracteres: filtrar
     * también actuaría sobre {@code setText} (por ej. al cargar un estudiante existente para
     * editar) y borraría en silencio cualquier dato legado que no matchee el patrón nuevo, en
     * vez de solo avisar. El campo vacío siempre se considera válido (la obligatoriedad se
     * valida aparte, al guardar).
     */
    public static void validarMientrasEscribe(javax.swing.JTextField campo, java.util.regex.Pattern patron) {
        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void validar() {
                marcarError(campo, patron.matcher(campo.getText()).matches());
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                validar();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                validar();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                validar();
            }
        });
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        private static final java.time.format.DateTimeFormatter FECHA_HORA =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private static final java.time.format.DateTimeFormatter FECHA =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        /**
         * Fechas legibles en todas las tablas ("12/09/2026 18:00" en vez de
         * "2026-09-12 18:00:00.0"). Solo cambia lo que se muestra: el valor de la celda sigue
         * siendo la fecha, así que ordenar por esa columna sigue siendo cronológico.
         */
        @Override
        protected void setValue(Object value) {
            if (value instanceof java.sql.Timestamp) {
                value = ((java.sql.Timestamp) value).toLocalDateTime().format(FECHA_HORA);
            } else if (value instanceof java.time.LocalDateTime) {
                value = ((java.time.LocalDateTime) value).format(FECHA_HORA);
            } else if (value instanceof java.sql.Date) {
                value = ((java.sql.Date) value).toLocalDate().format(FECHA);
            } else if (value instanceof java.time.LocalDate) {
                value = ((java.time.LocalDate) value).format(FECHA);
            }
            super.setValue(value);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                Object filaHoverProp = table.getClientProperty("filaHover");
                int filaHover = filaHoverProp instanceof Integer ? (Integer) filaHoverProp : -1;
                Color base = row % 2 == 0 ? SUPERFICIE : BORDE_SUAVE;
                c.setBackground(row == filaHover ? mezclar(base, PRIMARIO, 0.10f) : base);
                c.setForeground(TEXTO_PRIMARIO);
            }
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return c;
        }
    }

    /** Color de texto/ícono de un botón deshabilitado, legible sobre su fondo apagado en ambos temas. */
    private static Color colorDeshabilitado() {
        return new Color(TEXTO_SECUNDARIO.getRed(), TEXTO_SECUNDARIO.getGreen(), TEXTO_SECUNDARIO.getBlue(), 170);
    }

    /** Traduce el rol crudo guardado en BD (admin/psicologo/secretaria) a la etiqueta que ve el usuario. */
    public static String etiquetaRol(String rol) {
        if (rol == null) {
            return "";
        }
        switch (rol) {
            case "admin": return "Administrador/a";
            case "psicologo": return "Profesional de Psicología";
            case "secretaria": return "Usuario autorizado";
            default: return rol;
        }
    }

    /** Traduce el estado crudo de una cita (programado/completado/cancelado/ausente) a su etiqueta. */
    public static String etiquetaEstadoCita(String estado) {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case "programado": return "Programada";
            case "completado": return "Realizada";
            case "cancelado": return "Cancelada";
            case "ausente": return "No asistió";
            default: return estado;
        }
    }

    /** Renderer de tabla que muestra la etiqueta traducida de un rol, sin alterar el valor real. */
    public static DefaultTableCellRenderer rendererRol() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                    table, etiquetaRol(String.valueOf(value)), isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? SUPERFICIE : BORDE_SUAVE);
                    c.setForeground(TEXTO_PRIMARIO);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
    }

    /** Renderer de combo que muestra la etiqueta traducida de un rol, sin alterar el valor seleccionado. */
    /**
     * Muestra en un combo la etiqueta del estado de una cita ("Programada", "No asistió"...) en
     * vez del valor crudo de la base; el ítem seleccionado sigue siendo el valor crudo.
     */
    public static ListCellRenderer<Object> rendererEstadoCombo() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(
                    list, etiquetaEstadoCita(String.valueOf(value)), index, isSelected, cellHasFocus);
            }
        };
    }

    public static ListCellRenderer<Object> rendererRolCombo() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(
                    list, etiquetaRol(String.valueOf(value)), index, isSelected, cellHasFocus);
            }
        };
    }

    /** Renderer de "pill" de color para columnas de estado (ej. estado de una cita). */
    public static DefaultTableCellRenderer rendererEstado() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                JLabel pill = new JLabel(etiquetaEstadoCita(String.valueOf(value)));
                pill.setOpaque(true);
                pill.setFont(TEXTO_CHICO);
                pill.setHorizontalAlignment(SwingConstants.CENTER);
                pill.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

                Color color;
                String estado = String.valueOf(value).toLowerCase();
                switch (estado) {
                    case "completado": color = EXITO; break;
                    case "cancelado":
                    case "ausente": color = PELIGRO; break;
                    default: color = PRIMARIO;
                }
                pill.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                pill.setForeground(color);

                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? SUPERFICIE : BORDE_SUAVE);
                wrapper.add(pill);
                return wrapper;
            }
        };
    }

    /**
     * Ejecuta `accion` mostrando el cursor de espera mientras dura (consultas a la BD en el hilo
     * de UI). Acepta cualquier componente (panel o frame): busca la ventana real que lo contiene
     * en ese momento, ya que las pantallas ahora son paneles dentro de un único JFrame.
     */
    public static void conCursorEspera(java.awt.Component ancla, Runnable accion) {
        java.awt.Window ventana = ancla instanceof java.awt.Window
            ? (java.awt.Window) ancla
            : javax.swing.SwingUtilities.getWindowAncestor(ancla);

        if (ventana == null) {
            accion.run();
            return;
        }

        Cursor anterior = ventana.getCursor();
        ventana.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            accion.run();
        } finally {
            ventana.setCursor(anterior);
        }
    }

    /**
     * Clic derecho sobre una fila la selecciona antes de mostrar el menú contextual (por defecto
     * Swing muestra el menú pero deja seleccionada la fila anterior, y la acción caía sobre otra).
     */
    public static void seleccionarFilaConClicDerecho(JTable tabla) {
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int fila = tabla.rowAtPoint(e.getPoint());
                    if (fila >= 0 && !tabla.isRowSelected(fila)) {
                        tabla.setRowSelectionInterval(fila, fila);
                    }
                }
            }
        });
    }

    /**
     * Ejecuta {@code accion} un momento después de que el usuario deja de escribir en el campo
     * (búsqueda "en vivo"), sin tener que apretar Enter ni un botón "Buscar". La espera evita
     * lanzar una consulta a la base por cada tecla.
     */
    public static void alEscribir(javax.swing.JTextField campo, int esperaMs, Runnable accion) {
        Timer espera = new Timer(esperaMs, e -> accion.run());
        espera.setRepeats(false);
        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { espera.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { espera.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { espera.restart(); }
        });
    }

    /**
     * Confirmación tras exportar un archivo: muestra dónde quedó (ruta completa, no la relativa)
     * y ofrece abrirlo o mostrarlo en su carpeta, en vez de dejar al usuario buscándolo a mano.
     */
    public static void mostrarArchivoGuardado(Component ancla, String titulo, java.io.File archivo) {
        JPanel contenido = new JPanel(new BorderLayout(14, 0));
        contenido.setOpaque(false);

        JComponent insignia = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(oscuro ? oscurecer(ACENTO_VERDE.icono, 0.45f) : ACENTO_VERDE.insignia);
                g2.fillOval(0, 0, 48, 48);
                Icono.EXPORTAR.dibujar(g2, 12, 12, 24, ACENTO_VERDE.icono);
                g2.dispose();
            }
        };
        insignia.setPreferredSize(new Dimension(48, 48));
        JPanel envInsignia = new JPanel(new BorderLayout());
        envInsignia.setOpaque(false);
        envInsignia.add(insignia, BorderLayout.NORTH);
        contenido.add(envInsignia, BorderLayout.WEST);

        JLabel lblNombre = new JLabel(archivo.getName());
        lblNombre.setFont(fuente(Font.BOLD, 14));
        lblNombre.setForeground(TEXTO_PRIMARIO);
        JLabel lblUbicacion = new JLabel("<html>Guardado en: " + archivo.getAbsoluteFile().getParent() + "</html>");
        lblUbicacion.setFont(TEXTO_CHICO);
        lblUbicacion.setForeground(TEXTO_SECUNDARIO);
        JPanel textos = new JPanel(new BorderLayout(0, 6));
        textos.setOpaque(false);
        textos.add(lblNombre, BorderLayout.NORTH);
        textos.add(lblUbicacion, BorderLayout.CENTER);
        contenido.add(textos, BorderLayout.CENTER);

        JButton btnAbrir = botonPrimario("Abrir archivo", Icono.EXPORTAR);
        JButton btnCarpeta = botonSecundario("Mostrar en carpeta", Icono.CARPETA);
        Runnable cerrar = mostrarHoja(ancla, titulo, "El archivo quedó listo", contenido, new Dimension(560, 250),
            btnAbrir, btnCarpeta);
        btnAbrir.addActionListener(e -> {
            cerrar.run();
            try {
                util.Carpetas.abrir(archivo);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ancla, "No se pudo abrir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnCarpeta.addActionListener(e -> {
            cerrar.run();
            try {
                util.Carpetas.mostrarEnCarpeta(archivo);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ancla, "No se pudo abrir la carpeta: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Abre una carpeta en el Explorador (creándola si hiciera falta), avisando si no se puede. */
    public static void abrirCarpeta(Component ancla, java.io.File carpeta) {
        try {
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            util.Carpetas.abrir(carpeta);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ancla, "No se pudo abrir la carpeta:\n" + carpeta.getAbsolutePath()
                + "\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Corre `tarea` (una consulta/actualización JDBC, típicamente) en un hilo de fondo en vez del
     * hilo de UI, mostrando el cursor de espera en la ventana de `ancla` mientras dura. El
     * resultado (o el error) vuelve al hilo de UI a través de `alTerminar`/`alError`. A diferencia
     * de {@link #conCursorEspera}, acá la ventana sigue respondiendo (se puede mover, redimensionar,
     * cancelar un diálogo) mientras se espera a la base de datos, en vez de quedar congelada.
     */
    public static <T> void enSegundoPlano(
            java.awt.Component ancla,
            java.util.concurrent.Callable<T> tarea,
            java.util.function.Consumer<T> alTerminar,
            java.util.function.Consumer<Exception> alError) {

        java.awt.Window ventana = ancla instanceof java.awt.Window
            ? (java.awt.Window) ancla
            : javax.swing.SwingUtilities.getWindowAncestor(ancla);
        Cursor anterior = ventana != null ? ventana.getCursor() : null;
        if (ventana != null) {
            ventana.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        new javax.swing.SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                if (ventana != null) {
                    ventana.setCursor(anterior);
                }
                try {
                    alTerminar.accept(get());
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    alError.accept(causa instanceof Exception ? (Exception) causa : e);
                } catch (Exception e) {
                    alError.accept(e);
                }
            }
        }.execute();
    }

    /**
     * Liga la tecla Esc, en cualquier parte de la ventana que contenga a `ancla`, a `accion`
     * (típicamente "Volver"/"Cerrar"). Funciona tanto si `ancla` es un panel que todavía no se
     * agregó a ninguna ventana (WHEN_IN_FOCUSED_WINDOW se resuelve en el momento del evento, no
     * al registrar) como si es el rootPane de un JFrame. Además agrega "(Esc)" al tooltip del
     * botón correspondiente para que el atajo sea descubrible.
     */
    public static void atajoEscape(JComponent ancla, JButton botonAsociado, Runnable accion) {
        atajo(ancla, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), accion);
        if (botonAsociado != null) {
            String actual = botonAsociado.getToolTipText();
            botonAsociado.setToolTipText((actual != null ? actual + " " : "") + "(Esc)");
        }
    }

    // ==================== Atajos y "hojas" dentro de la misma ventana ====================

    private static final String PROP_HOJAS = "tema.hojasAbiertas";

    /**
     * Atajo de teclado para toda la ventana, que solo actúa si {@code ancla} está a la vista y no
     * hay una hoja ({@link #mostrarHoja}) abierta encima. Como todas las secciones viven en la
     * misma ventana (CardLayout), sin esa condición un Esc registrado por una pantalla oculta
     * podía "ganarle" al de la pantalla visible. Si el atajo no aplica, la tecla sigue su camino
     * hacia el siguiente componente que la tenga registrada.
     */
    public static void atajo(JComponent ancla, KeyStroke tecla, Runnable accion) {
        String clave = "atajo:" + tecla;
        ancla.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tecla, clave);
        ancla.getActionMap().put(clave, new javax.swing.AbstractAction() {
            @Override
            public boolean isEnabled() {
                return ancla.isShowing() && !hayHojaAbierta(ancla);
            }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                accion.run();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<JComponent> hojasAbiertas(JComponent raiz) {
        Object lista = raiz.getClientProperty(PROP_HOJAS);
        if (lista == null) {
            lista = new java.util.ArrayList<JComponent>();
            raiz.putClientProperty(PROP_HOJAS, lista);
        }
        return (java.util.List<JComponent>) lista;
    }

    private static boolean hayHojaAbierta(Component c) {
        javax.swing.JRootPane raiz = SwingUtilities.getRootPane(c);
        return raiz != null && !hojasAbiertas(raiz).isEmpty();
    }

    /**
     * Muestra {@code contenido} como una "hoja" dentro de la misma ventana — fondo oscurecido y una
     * tarjeta centrada con título, botón para cerrar y pie de botones — en lugar de abrir un
     * JDialog aparte. Bloquea el clic sobre lo de atrás mientras está abierta; Esc o "Cerrar" la
     * cierran.
     *
     * @param tamano      tamaño deseado de la tarjeta (se achica si la ventana es más chica)
     * @param botonesPie  botones extra, a la izquierda de "Cerrar"
     * @return una acción que cierra la hoja (para usar desde los botones del pie)
     */
    public static Runnable mostrarHoja(Component ancla, String titulo, String subtitulo, JComponent contenido,
                                       Dimension tamano, JButton... botonesPie) {
        return mostrarHoja(ancla, titulo, subtitulo, contenido, tamano, "Cerrar", botonesPie);
    }

    /** Igual que {@link #mostrarHoja(Component, String, String, JComponent, Dimension, JButton...)}, eligiendo el texto del botón que cierra. */
    public static Runnable mostrarHoja(Component ancla, String titulo, String subtitulo, JComponent contenido,
                                       Dimension tamano, String textoCerrar, JButton... botonesPie) {
        javax.swing.JRootPane raiz = ancla instanceof javax.swing.RootPaneContainer
            ? ((javax.swing.RootPaneContainer) ancla).getRootPane()
            : SwingUtilities.getRootPane(ancla);
        JLayeredPane capas = raiz.getLayeredPane();
        Component focoAnterior = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        final float[] alfa = {0f};
        JPanel overlay = new JPanel(new java.awt.GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(15, 18, 30, (int) (120 * alfa[0])));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlay.setOpaque(false);
        // Consume los clics para que no lleguen a los componentes de atrás.
        overlay.addMouseListener(new java.awt.event.MouseAdapter() { });
        overlay.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });

        JPanel tarjeta = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SUPERFICIE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.setColor(BORDE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.setFocusCycleRoot(true);

        final Runnable[] cerrar = new Runnable[1];

        // Encabezado: título + subtítulo + botón cerrar (X).
        JPanel encabezado = new JPanel(new BorderLayout(10, 0));
        encabezado.setOpaque(false);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
            BorderFactory.createEmptyBorder(16, 22, 14, 14)));
        JPanel textos = new JPanel(new java.awt.GridLayout(subtitulo != null ? 2 : 1, 1, 0, 2));
        textos.setOpaque(false);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(fuente(Font.BOLD, 17));
        lblTitulo.setForeground(TEXTO_PRIMARIO);
        textos.add(lblTitulo);
        if (subtitulo != null) {
            JLabel lblSub = new JLabel(subtitulo);
            lblSub.setFont(TEXTO_CHICO);
            lblSub.setForeground(TEXTO_SECUNDARIO);
            textos.add(lblSub);
        }
        encabezado.add(textos, BorderLayout.CENTER);
        JButton btnX = botonIcono(Icono.CERRAR, "Cerrar (Esc)");
        btnX.addActionListener(e -> cerrar[0].run());
        JPanel envX = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        envX.setOpaque(false);
        envX.add(btnX);
        encabezado.add(envX, BorderLayout.EAST);
        tarjeta.add(encabezado, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 22, 6, 22));
        cuerpo.add(contenido, BorderLayout.CENTER);
        tarjeta.add(cuerpo, BorderLayout.CENTER);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
        for (JButton b : botonesPie) {
            pie.add(b);
        }
        JButton btnCerrar = botonSecundario(textoCerrar);
        btnCerrar.setToolTipText("Esc");
        btnCerrar.addActionListener(e -> cerrar[0].run());
        pie.add(btnCerrar);
        tarjeta.add(pie, BorderLayout.SOUTH);

        // La tarjeta entra subiendo unos píxeles (se anima el margen superior de su celda).
        java.awt.GridBagConstraints celda = new java.awt.GridBagConstraints();
        celda.insets = new java.awt.Insets(28, 0, 0, 0);
        overlay.add(tarjeta, celda);

        Runnable ajustarTamano = () -> {
            overlay.setBounds(0, 0, capas.getWidth(), capas.getHeight());
            tarjeta.setPreferredSize(new Dimension(
                Math.max(320, Math.min(tamano.width, capas.getWidth() - 60)),
                Math.max(200, Math.min(tamano.height, capas.getHeight() - 60))));
            overlay.revalidate();
        };
        java.awt.event.ComponentAdapter alRedimensionar = new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ajustarTamano.run();
            }
        };
        ajustarTamano.run();
        capas.addComponentListener(alRedimensionar);
        capas.add(overlay, JLayeredPane.MODAL_LAYER);
        java.util.List<JComponent> abiertas = hojasAbiertas(raiz);
        abiertas.add(overlay);

        cerrar[0] = () -> {
            if (overlay.getParent() == null) {
                return;
            }
            abiertas.remove(overlay);
            capas.removeComponentListener(alRedimensionar);
            capas.remove(overlay);
            capas.revalidate();
            capas.repaint();
            if (focoAnterior != null && focoAnterior.isShowing()) {
                focoAnterior.requestFocusInWindow();
            }
        };

        // Esc: solo la hoja de más arriba.
        overlay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrarHoja");
        overlay.getActionMap().put("cerrarHoja", new javax.swing.AbstractAction() {
            @Override
            public boolean isEnabled() {
                return !abiertas.isEmpty() && abiertas.get(abiertas.size() - 1) == overlay;
            }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cerrar[0].run();
            }
        });

        // Entrada por tiempo (no por cuadros): el fondo se oscurece y la tarjeta sube a su lugar
        // con una curva ease-out, en ~220 ms.
        long inicioEntrada = System.nanoTime();
        java.awt.GridBagLayout layoutOverlay = (java.awt.GridBagLayout) overlay.getLayout();
        Timer fundido = new Timer(8, null);
        fundido.addActionListener(e -> {
            float t = Math.min(1f, (System.nanoTime() - inicioEntrada) / 220_000_000f);
            float avance = 1f - (float) Math.pow(1f - t, 3);
            alfa[0] = avance;
            celda.insets = new java.awt.Insets(Math.round(28 * (1f - avance)), 0, 0, 0);
            layoutOverlay.setConstraints(tarjeta, celda);
            overlay.revalidate();
            overlay.repaint();
            if (t >= 1f) {
                fundido.stop();
            }
        });
        fundido.start();

        overlay.revalidate();
        capas.repaint();
        SwingUtilities.invokeLater(() -> (botonesPie.length > 0 ? botonesPie[0] : btnCerrar).requestFocusInWindow());
        return cerrar[0];
    }

    /**
     * Pregunta de confirmación dentro de la misma ventana (una hoja, no un JOptionPane aparte).
     * {@code siConfirma} corre solo si se toca el botón de confirmar; cancelar o Esc no hacen nada.
     */
    public static void confirmar(Component ancla, String titulo, String mensaje, String textoConfirmar,
                                 String textoCancelar, Runnable siConfirma) {
        JLabel lbl = new JLabel("<html>" + mensaje + "</html>");
        lbl.setFont(TEXTO);
        lbl.setForeground(TEXTO_PRIMARIO);
        lbl.setVerticalAlignment(SwingConstants.TOP);
        JButton btnConfirmar = botonPeligro(textoConfirmar);
        Runnable cerrar = mostrarHoja(ancla, titulo, null, lbl, new Dimension(480, 210), textoCancelar, btnConfirmar);
        btnConfirmar.addActionListener(e -> {
            cerrar.run();
            siConfirma.run();
        });
    }

    /**
     * Barra de botones "responsive": si la barra queda más angosta de lo que necesita con los
     * textos completos (ventana chica, panel lateral abierto), los {@code botones} indicados pasan
     * a mostrar solo su ícono y el nombre se mueve al tooltip, en vez de superponerse o cortarse.
     * Vuelven a mostrar el texto apenas hay lugar.
     */
    public static void compactarAlAchicar(JComponent barra, JButton... botones) {
        for (JButton b : botones) {
            b.putClientProperty("tema.textoCompleto", b.getText());
            b.putClientProperty("tema.tooltipCompleto", b.getToolTipText());
        }
        barra.addComponentListener(new java.awt.event.ComponentAdapter() {
            private int anchoNecesario = -1;
            private Boolean compactoActual;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (anchoNecesario < 0) {
                    anchoNecesario = barra.getPreferredSize().width;
                }
                boolean compacto = barra.getWidth() < anchoNecesario;
                if (compactoActual != null && compactoActual == compacto) {
                    return;
                }
                compactoActual = compacto;
                for (JButton b : botones) {
                    String texto = (String) b.getClientProperty("tema.textoCompleto");
                    String tooltip = (String) b.getClientProperty("tema.tooltipCompleto");
                    b.setText(compacto ? "" : texto);
                    b.setToolTipText(compacto ? texto + (tooltip != null ? " — " + tooltip : "") : tooltip);
                }
                barra.revalidate();
                barra.repaint();
            }
        });
    }

    /** Botón chico, solo con ícono (p. ej. la X para cerrar un panel). */
    public static JButton botonIcono(Icono icono, String tooltip) {
        JButton b = new JButton(new IconoSwing(icono, TEXTO_SECUNDARIO, 16));
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setContentAreaFilled(false);
            }
        });
        return b;
    }

    // --- Logo institucional del colegio (escudo S.R.G.S.) ---

    private static final String RUTA_LOGO = "/Vista/recursos/logo_colegio.jpeg";
    private static BufferedImage logoImg;
    private static boolean logoCargado;

    /**
     * El escudo con el fondo blanco EXTERIOR ya recortado a transparente, para que sobre el
     * degradé de marca no quede un recuadro blanco alrededor. Se carga y procesa una sola vez.
     * Devuelve {@code null} si el archivo no está empaquetado.
     */
    private static BufferedImage logo() {
        if (!logoCargado) {
            logoCargado = true;
            try {
                java.net.URL url = Tema.class.getResource(RUTA_LOGO);
                BufferedImage src = (url != null) ? javax.imageio.ImageIO.read(url) : null;
                logoImg = (src != null) ? recortarFondoBlanco(src) : null;
            } catch (java.io.IOException e) {
                logoImg = null;
            }
        }
        return logoImg;
    }

    /**
     * Vuelve transparente el blanco que rodea al escudo, con un relleno por inundación desde los
     * bordes de la imagen. El trazo negro del contorno frena la inundación, así el blanco
     * INTERIOR (la hoja que sostiene la figura, el cuello, etc.) queda intacto. Un pase final
     * suaviza la aureola clara que deja el JPEG en el borde.
     */
    private static BufferedImage recortarFondoBlanco(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        final int umbral = 210; // r,g,b >= umbral  => "casi blanco"
        boolean[] visto = new boolean[w * h];
        java.util.ArrayDeque<int[]> cola = new java.util.ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            cola.add(new int[]{x, 0});
            cola.add(new int[]{x, h - 1});
        }
        for (int y = 0; y < h; y++) {
            cola.add(new int[]{0, y});
            cola.add(new int[]{w - 1, y});
        }

        while (!cola.isEmpty()) {
            int[] p = cola.poll();
            int x = p[0];
            int y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h) {
                continue;
            }
            int i = y * w + x;
            if (visto[i]) {
                continue;
            }
            visto[i] = true;
            int argb = img.getRGB(x, y);
            if (((argb >> 16) & 255) < umbral || ((argb >> 8) & 255) < umbral || (argb & 255) < umbral) {
                continue;
            }
            img.setRGB(x, y, argb & 0x00FFFFFF);
            cola.add(new int[]{x + 1, y});
            cola.add(new int[]{x - 1, y});
            cola.add(new int[]{x, y + 1});
            cola.add(new int[]{x, y - 1});
        }

        // Feather de 1 px: los píxeles claros (aureola) que tocan zona transparente pasan a
        // semitransparentes según su luminosidad, para que el contorno no quede con halo.
        int[] copia = img.getRGB(0, 0, w, h, null, 0, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = copia[y * w + x];
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int min = Math.min((argb >> 16) & 255, Math.min((argb >> 8) & 255, argb & 255));
                if (min < 150) {
                    continue;
                }
                boolean tocaTransparente = false;
                for (int dy = -1; dy <= 1 && !tocaTransparente; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && (copia[ny * w + nx] >>> 24) == 0) {
                            tocaTransparente = true;
                            break;
                        }
                    }
                }
                if (tocaTransparente) {
                    int alpha = Math.max(0, Math.min(255, 255 - (min - 150) * 255 / 105));
                    img.setRGB(x, y, (alpha << 24) | (argb & 0x00FFFFFF));
                }
            }
        }
        return img;
    }

    /** Devuelve {@code true} si el archivo del logo está empaquetado y se pudo cargar. */
    public static boolean hayLogo() {
        return logo() != null;
    }

    /** El logo del colegio (fondo exterior transparente) escalado a {@code alto} px de altura. */
    public static ImageIcon logoColegio(int alto) {
        BufferedImage src = logo();
        if (src == null) {
            return new ImageIcon();
        }
        int ancho = Math.round(src.getWidth() * (alto / (float) src.getHeight()));
        return new ImageIcon(src.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
    }

    /** {@code JLabel} transparente con el logo del colegio (sirve igual sobre fondo claro o sobre el degradé). */
    public static JLabel etiquetaLogo(int alto) {
        JLabel l = new JLabel();
        l.setOpaque(false);
        if (hayLogo()) {
            l.setIcon(logoColegio(alto));
        }
        return l;
    }

    /**
     * El logo para colocar sobre el degradé de marca: como el fondo exterior del escudo ya es
     * transparente, alrededor se ve el color del encabezado, sin recuadro blanco.
     */
    public static JComponent insigniaLogo(int alto) {
        return etiquetaLogo(alto);
    }

    /** Ícono de aplicación (para JFrame.setIconImage): el escudo del colegio; si falta, un cuadrado con "DP". */
    public static Image iconoApp() {
        if (hayLogo()) {
            return logo();
        }
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setPaint(new GradientPaint(0, 0, SECUNDARIO, s, s, PRIMARIO));
        g2.fillRoundRect(0, 0, s, s, 16, 16);

        g2.setColor(Color.WHITE);
        Font f = new Font(FAMILIA, Font.BOLD, 28);
        g2.setFont(f);
        String texto = "DP";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tx = (s - fm.stringWidth(texto)) / 2;
        int ty = (s - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(texto, tx, ty);

        g2.dispose();
        return img;
    }

    /**
     * Vista previa de impresión reutilizable: en vez de mandar directo al diálogo nativo de
     * impresión, primero muestra el documento (texto plano, tal como va a salir) en una ventana
     * modal tipo "hoja", con botones "Imprimir" (recién ahí abre el diálogo nativo) y "Cerrar".
     * Pensada para cualquier botón "Imprimir" de la app, no solo el de Seguimiento.
     */
    public static void mostrarVistaPreviaImpresion(Component ancla, String titulo, String contenido) {
        JTextArea areaPrevia = new JTextArea(contenido);
        areaPrevia.setEditable(false);
        areaPrevia.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaPrevia.setBackground(Color.WHITE);
        areaPrevia.setForeground(Color.BLACK);
        areaPrevia.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JPanel hoja = new JPanel(new BorderLayout());
        hoja.setBackground(Color.WHITE);
        hoja.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        hoja.add(areaPrevia, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(hoja);
        scroll.getViewport().setBackground(new Color(210, 214, 220));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // Hoja dentro de la misma ventana (antes era un JDialog aparte).
        JButton btnImprimir = botonExito("Imprimir");
        Runnable cerrar = mostrarHoja(ancla, "Vista previa de impresión", titulo, scroll, new Dimension(760, 900), btnImprimir);
        btnImprimir.addActionListener(e -> {
            try {
                // El diálogo de la impresora sí es del sistema operativo (elige impresora/copias).
                if (areaPrevia.print()) {
                    cerrar.run();
                    mostrarNotificacion(ancla, "Enviado a la impresora");
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(ancla, "Error al imprimir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
