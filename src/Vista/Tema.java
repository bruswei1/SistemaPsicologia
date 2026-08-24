package Vista;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.BoxLayout;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.KeyEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

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

    // Colores de superficie/texto (varían con el modo oscuro)
    public static Color FONDO;
    public static Color SUPERFICIE;
    public static Color BORDE;
    public static Color BORDE_SUAVE;
    public static Color TEXTO_PRIMARIO;
    public static Color TEXTO_SECUNDARIO;

    // Colores de acción (fijos, no dependen del modo). Paleta tomada del cartel real del
    // Departamento de Orientación y Psicología del colegio: violeta institucional profundo como
    // color primario, azul institucional como secundario/de marca, y verde-azulado para éxito
    // (funcional, no de marca).
    public static final Color PRIMARIO = new Color(75, 58, 140);
    public static final Color SECUNDARIO = new Color(47, 90, 168);
    public static final Color EXITO = new Color(13, 148, 118);
    public static final Color PELIGRO = new Color(196, 58, 58);
    public static final Color NEUTRO = new Color(206, 212, 218);

    /** Pares de color (insignia suave + ícono sólido) para diferenciar categorías en tarjetas. */
    public static final Acento ACENTO_AZUL = new Acento(new Color(221, 231, 249), SECUNDARIO);
    public static final Acento ACENTO_VERDE = new Acento(new Color(209, 250, 229), new Color(5, 150, 105));
    public static final Acento ACENTO_MORADO = new Acento(new Color(228, 222, 247), PRIMARIO);
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
        } else {
            FONDO = new Color(246, 245, 251);
            SUPERFICIE = new Color(255, 255, 255);
            BORDE = new Color(226, 230, 236);
            BORDE_SUAVE = new Color(236, 239, 243);
            TEXTO_PRIMARIO = new Color(30, 35, 45);
            TEXTO_SECUNDARIO = new Color(108, 117, 130);
        }
    }

    private static Color colorOMenos(String claveUIManager, Color porDefecto) {
        Color c = UIManager.getColor(claveUIManager);
        return c != null ? c : porDefecto;
    }

    public static JButton botonPrimario(String texto) {
        return boton(texto, PRIMARIO, Color.WHITE, null);
    }

    public static JButton botonPrimario(String texto, Icono icono) {
        return boton(texto, PRIMARIO, Color.WHITE, icono);
    }

    public static JButton botonExito(String texto) {
        return boton(texto, EXITO, Color.WHITE, null);
    }

    public static JButton botonExito(String texto, Icono icono) {
        return boton(texto, EXITO, Color.WHITE, icono);
    }

    public static JButton botonPeligro(String texto) {
        return boton(texto, PELIGRO, Color.WHITE, null);
    }

    public static JButton botonPeligro(String texto, Icono icono) {
        return boton(texto, PELIGRO, Color.WHITE, icono);
    }

    public static JButton botonSecundario(String texto) {
        return boton(texto, NEUTRO, Color.BLACK, null);
    }

    public static JButton botonSecundario(String texto, Icono icono) {
        return boton(texto, NEUTRO, Color.BLACK, icono);
    }

    private static JButton boton(String texto, Color fondo, Color letra, Icono icono) {
        JButton b = new JButton(texto);
        b.setBackground(fondo);
        b.setForeground(letra);
        b.setFont(BOTON);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        if (icono != null) {
            b.setIcon(new IconoSwing(icono, letra, 16));
            b.setIconTextGap(8);
        }
        return b;
    }

    /**
     * Encabezado de marca para el tope de una pantalla: franja con degradé
     * {@link #SECUNDARIO} → {@link #PRIMARIO} (mismos colores del cartel real del Departamento),
     * título en blanco y subtítulo opcional en blanco semi-transparente. Reemplaza el header
     * blanco liso que cada pantalla armaba a mano.
     */
    public static JPanel panelEncabezado(String titulo, String subtitulo) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, SECUNDARIO, getWidth(), getHeight(), PRIMARIO));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
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
        return panel;
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

    /** Aplica fuente, alto de fila y zebra striping consistentes a cualquier JTable de la app. */
    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(TEXTO_CHICO);
        tabla.getTableHeader().setFont(new Font(FAMILIA, Font.BOLD, 12));
        tabla.getTableHeader().setBackground(BORDE_SUAVE);
        tabla.getTableHeader().setForeground(TEXTO_PRIMARIO);
        tabla.setRowHeight(28);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(new Color(PRIMARIO.getRed(), PRIMARIO.getGreen(), PRIMARIO.getBlue(), 45));
        tabla.setSelectionForeground(TEXTO_PRIMARIO);
        tabla.setDefaultRenderer(Object.class, new ZebraRenderer());
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? SUPERFICIE : BORDE_SUAVE);
                c.setForeground(TEXTO_PRIMARIO);
            }
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return c;
        }
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
     * Liga la tecla Esc, en cualquier parte de la ventana que contenga a `ancla`, a `accion`
     * (típicamente "Volver"/"Cerrar"). Funciona tanto si `ancla` es un panel que todavía no se
     * agregó a ninguna ventana (WHEN_IN_FOCUSED_WINDOW se resuelve en el momento del evento, no
     * al registrar) como si es el rootPane de un JFrame. Además agrega "(Esc)" al tooltip del
     * botón correspondiente para que el atajo sea descubrible.
     */
    public static void atajoEscape(JComponent ancla, JButton botonAsociado, Runnable accion) {
        ancla.registerKeyboardAction(
            e -> accion.run(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        if (botonAsociado != null) {
            String actual = botonAsociado.getToolTipText();
            botonAsociado.setToolTipText((actual != null ? actual + " " : "") + "(Esc)");
        }
    }

    /** Ícono de aplicación (para JFrame.setIconImage): cuadrado redondeado con las iniciales "SP". */
    public static Image iconoApp() {
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
}
