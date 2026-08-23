package Vista;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

    // Colores de acción (fijos, no dependen del modo)
    public static final Color PRIMARIO = new Color(58, 91, 217);
    public static final Color EXITO = new Color(13, 148, 118);
    public static final Color PELIGRO = new Color(220, 53, 69);
    public static final Color NEUTRO = new Color(206, 212, 218);

    /** Pares de color (insignia suave + ícono sólido) para diferenciar categorías en tarjetas. */
    public static final Acento ACENTO_AZUL = new Acento(new Color(224, 231, 255), new Color(58, 91, 217));
    public static final Acento ACENTO_VERDE = new Acento(new Color(209, 250, 229), new Color(5, 150, 105));
    public static final Acento ACENTO_MORADO = new Acento(new Color(237, 224, 255), new Color(124, 58, 237));
    public static final Acento ACENTO_AMBAR = new Acento(new Color(254, 240, 199), new Color(202, 113, 6));
    public static final Acento ACENTO_CELESTE = new Acento(new Color(207, 241, 255), new Color(2, 132, 199));
    public static final Acento ACENTO_ROSA = new Acento(new Color(255, 224, 235), new Color(219, 39, 119));

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

    /**
     * Alterna modo oscuro: cambia el Look & Feel real de FlatLaf (no solo la paleta propia),
     * para que campos de texto, combos, spinners, scrollbars y diálogos —que no pintamos
     * nosotros— también se oscurezcan en vez de quedar blancos. Solo afecta a ventanas
     * creadas de ahí en más: una ya abierta hay que recrearla (dispose()+new).
     */
    public static void alternarModoOscuro() {
        oscuro = !oscuro;
        try {
            UIManager.setLookAndFeel(oscuro ? new FlatDarkLaf() : new FlatLightLaf());
            FlatLaf.updateUI();
        } catch (Exception e) {
            System.out.println("No se pudo cambiar el Look & Feel: " + e.getMessage());
        }
        aplicarPaleta();
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
            FONDO = new Color(244, 246, 249);
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

    /** Renderer de "pill" de color para columnas de estado (ej. estado de un turno). */
    public static DefaultTableCellRenderer rendererEstado() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                JLabel pill = new JLabel(String.valueOf(value));
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

    /** Ícono de aplicación (para JFrame.setIconImage): cuadrado redondeado con las iniciales "SP". */
    public static Image iconoApp() {
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(PRIMARIO);
        g2.fillRoundRect(0, 0, s, s, 16, 16);

        g2.setColor(Color.WHITE);
        Font f = new Font(FAMILIA, Font.BOLD, 28);
        g2.setFont(f);
        String texto = "SP";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tx = (s - fm.stringWidth(texto)) / 2;
        int ty = (s - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(texto, tx, ty);

        g2.dispose();
        return img;
    }
}
