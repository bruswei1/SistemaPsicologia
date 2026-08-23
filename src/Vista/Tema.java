package Vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Paleta y tipografía centralizadas para todas las pantallas Swing.
 * Objetivo: que ninguna pantalla vuelva a declarar sus propios
 * new Color(...)/new Font(...) sueltos.
 */
public final class Tema {

    private Tema() {
    }

    // Colores
    public static final Color FONDO = new Color(244, 246, 249);
    public static final Color SUPERFICIE = new Color(255, 255, 255);
    public static final Color BORDE = new Color(226, 230, 236);
    public static final Color BORDE_SUAVE = new Color(236, 239, 243);

    public static final Color PRIMARIO = new Color(58, 91, 217);
    public static final Color EXITO = new Color(13, 148, 118);
    public static final Color PELIGRO = new Color(220, 53, 69);
    public static final Color NEUTRO = new Color(206, 212, 218);

    public static final Color TEXTO_PRIMARIO = new Color(30, 35, 45);
    public static final Color TEXTO_SECUNDARIO = new Color(108, 117, 130);

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

    public static JButton botonPrimario(String texto) {
        return boton(texto, PRIMARIO, Color.WHITE);
    }

    public static JButton botonExito(String texto) {
        return boton(texto, EXITO, Color.WHITE);
    }

    public static JButton botonPeligro(String texto) {
        return boton(texto, PELIGRO, Color.WHITE);
    }

    public static JButton botonSecundario(String texto) {
        return boton(texto, NEUTRO, Color.BLACK);
    }

    private static JButton boton(String texto, Color fondo, Color letra) {
        JButton b = new JButton(texto);
        b.setBackground(fondo);
        b.setForeground(letra);
        b.setFont(BOTON);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return b;
    }
}
