package Vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;

/**
 * Campo reutilizable de fecha + hora: un cuadro de texto de sólo lectura y un botón
 * "Elegir…" que abre, dentro de la misma ventana (una hoja de {@link Tema#mostrarHoja}), un
 * calendario mensual (clic en el día) y dos ruedas hora/minutos. Reemplaza al {@code JSpinner} de texto que usaba la Agenda.
 *
 * Mismo espíritu que {@link SelectorCursoSeccion}: todo el estilo sale de {@link Tema},
 * sin {@code new Color(...)}/{@code new Font(...)} sueltos.
 */
public class SelectorFechaHora extends JPanel {

    private static final DateTimeFormatter FORMATO_CAMPO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES = new Locale("es");
    private static final int PASO_MINUTOS = 5;

    private final JTextField txtVisible = new JTextField();
    private LocalDateTime valor;

    public SelectorFechaHora() {
        super(new BorderLayout(6, 0));
        setOpaque(false);

        txtVisible.setEditable(false);
        txtVisible.setFont(Tema.TEXTO);

        JButton btnElegir = Tema.botonSecundario("Elegir…", Icono.AGENDA);
        btnElegir.addActionListener(e -> abrirDialogo());

        add(txtVisible, BorderLayout.CENTER);
        add(btnElegir, BorderLayout.EAST);

        setValor(redondear(LocalDateTime.now()));
    }

    public LocalDateTime getValor() {
        return valor;
    }

    public void setValor(LocalDateTime nuevo) {
        valor = (nuevo != null ? nuevo : LocalDateTime.now()).withSecond(0).withNano(0);
        txtVisible.setText(valor.format(FORMATO_CAMPO));
    }

    /** Nunca devuelve null: si no se eligió nada, es el valor con el que se creó el campo ("ahora"). */
    public Timestamp getTimestamp() {
        return Timestamp.valueOf(valor);
    }

    public void setTimestamp(Timestamp ts) {
        setValor(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
    }

    private static LocalDateTime redondear(LocalDateTime dt) {
        int r = (dt.getMinute() / PASO_MINUTOS) * PASO_MINUTOS;
        return dt.withMinute(r).withSecond(0).withNano(0);
    }

    private void abrirDialogo() {
        Contenido contenido = new Contenido(redondear(valor));
        JButton btnAceptar = Tema.botonExito("Aceptar", Icono.GUARDAR);
        // Hoja dentro de la misma ventana (antes era un JDialog modal aparte).
        Runnable cerrar = Tema.mostrarHoja(this, "Elegir fecha y hora", "Tocá un día del calendario y elegí la hora",
            contenido, new Dimension(660, 500), "Cancelar", btnAceptar);
        btnAceptar.addActionListener(e -> {
            LocalDateTime v = contenido.armarValor();
            if (v != null) {
                setValor(v);
                cerrar.run();
            }
        });
    }

    /** Cuerpo del diálogo: calendario a la izquierda, ruedas hora/minutos a la derecha. */
    private static class Contenido extends JPanel {

        private final MiniCalendario calendario;
        private final JList<String> listaHoras;
        private final JList<String> listaMinutos;
        private final JLabel lblPreview = new JLabel("", SwingConstants.CENTER);

        /** Solo el cuerpo (calendario + hora + resumen); el título y los botones los pone la hoja. */
        Contenido(LocalDateTime inicial) {
            super(new BorderLayout());
            setOpaque(false);

            calendario = new MiniCalendario(inicial.toLocalDate(), d -> actualizarPreview());

            listaHoras = ruedaNumeros(0, 23, 1, inicial.getHour());
            listaMinutos = ruedaNumeros(0, 55, PASO_MINUTOS, (inicial.getMinute() / PASO_MINUTOS) * PASO_MINUTOS);
            ListSelectionListener l = e -> actualizarPreview();
            listaHoras.addListSelectionListener(l);
            listaMinutos.addListSelectionListener(l);

            JLabel dosPuntos = new JLabel(":");
            dosPuntos.setFont(Tema.TITULO);
            dosPuntos.setForeground(Tema.TEXTO_PRIMARIO);

            JPanel ruedas = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 18));
            ruedas.setBackground(Tema.SUPERFICIE);
            ruedas.add(envolver(listaHoras));
            ruedas.add(dosPuntos);
            ruedas.add(envolver(listaMinutos));
            ruedas.setBorder(Tema.tituloSeccion("Hora"));

            JPanel centro = new JPanel(new BorderLayout(12, 0));
            centro.setBackground(Tema.SUPERFICIE);
            centro.setBorder(BorderFactory.createEmptyBorder(12, 15, 6, 15));
            centro.add(calendario, BorderLayout.CENTER);
            centro.add(ruedas, BorderLayout.EAST);
            add(centro, BorderLayout.CENTER);

            lblPreview.setFont(Tema.TEXTO);
            lblPreview.setForeground(Tema.TEXTO_SECUNDARIO);
            lblPreview.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));

            lblPreview.setFont(Tema.fuente(java.awt.Font.BOLD, 14));
            lblPreview.setForeground(Tema.PRIMARIO);
            add(lblPreview, BorderLayout.SOUTH);

            actualizarPreview();
        }

        private static JScrollPane envolver(JList<String> lista) {
            JScrollPane sp = new JScrollPane(lista,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setPreferredSize(new Dimension(70, 170));
            return sp;
        }

        private static JList<String> ruedaNumeros(int desde, int hasta, int paso, int seleccion) {
            DefaultListModel<String> modelo = new DefaultListModel<>();
            int idxSel = 0;
            int i = 0;
            for (int n = desde; n <= hasta; n += paso, i++) {
                modelo.addElement(String.format("%02d", n));
                if (n == seleccion) {
                    idxSel = i;
                }
            }
            JList<String> lista = new JList<>(modelo);
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lista.setVisibleRowCount(6);
            lista.setFont(Tema.SUBTITULO);
            lista.setFixedCellHeight(28);
            DefaultListCellRenderer r = new DefaultListCellRenderer();
            r.setHorizontalAlignment(SwingConstants.CENTER);
            lista.setCellRenderer(r);
            lista.setSelectedIndex(idxSel);
            lista.ensureIndexIsVisible(idxSel);
            return lista;
        }

        private LocalDateTime armarValor() {
            String h = listaHoras.getSelectedValue();
            String m = listaMinutos.getSelectedValue();
            if (h == null || m == null || calendario.getSeleccionado() == null) {
                return null;
            }
            return LocalDateTime.of(calendario.getSeleccionado(),
                LocalTime.of(Integer.parseInt(h), Integer.parseInt(m)));
        }

        private void actualizarPreview() {
            LocalDateTime v = armarValor();
            if (v == null) {
                lblPreview.setText("");
                return;
            }
            String texto = v.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", ES));
            lblPreview.setText(texto.isEmpty() ? "" : Character.toUpperCase(texto.charAt(0)) + texto.substring(1));
        }
    }

    /** Calendario mensual sólo para elegir una fecha (sin consultar la BD). */
    private static class MiniCalendario extends JPanel {

        private final Consumer<LocalDate> alSeleccionar;
        private YearMonth mes;
        private LocalDate seleccionado;
        private final JLabel lblMes = new JLabel("", SwingConstants.CENTER);
        private final JPanel grilla = new JPanel(new GridLayout(0, 7, 3, 3));

        MiniCalendario(LocalDate inicial, Consumer<LocalDate> alSeleccionar) {
            super(new BorderLayout(0, 6));
            this.alSeleccionar = alSeleccionar;
            this.seleccionado = inicial;
            this.mes = YearMonth.from(inicial);
            setBackground(Tema.SUPERFICIE);
            setBorder(Tema.tituloSeccion("Fecha"));

            JButton btnAnterior = Tema.botonSecundario("", Icono.ANTERIOR);
            btnAnterior.setToolTipText("Mes anterior");
            btnAnterior.addActionListener(e -> { mes = mes.minusMonths(1); refrescar(); });
            JButton btnSiguiente = Tema.botonSecundario("", Icono.SIGUIENTE);
            btnSiguiente.setToolTipText("Mes siguiente");
            btnSiguiente.addActionListener(e -> { mes = mes.plusMonths(1); refrescar(); });
            JButton btnHoy = Tema.botonSecundario("Hoy");
            btnHoy.setToolTipText("Ir a la fecha de hoy");
            btnHoy.addActionListener(e -> {
                seleccionado = LocalDate.now();
                mes = YearMonth.from(seleccionado);
                refrescar();
                alSeleccionar.accept(seleccionado);
            });

            lblMes.setFont(Tema.SUBTITULO);
            lblMes.setForeground(Tema.TEXTO_PRIMARIO);

            JPanel nav = new JPanel(new BorderLayout(6, 0));
            nav.setBackground(Tema.SUPERFICIE);
            nav.add(btnAnterior, BorderLayout.WEST);
            nav.add(lblMes, BorderLayout.CENTER);
            nav.add(btnSiguiente, BorderLayout.EAST);

            JPanel cabecera = new JPanel(new BorderLayout(6, 4));
            cabecera.setBackground(Tema.SUPERFICIE);
            cabecera.add(nav, BorderLayout.CENTER);
            cabecera.add(btnHoy, BorderLayout.EAST);

            grilla.setBackground(Tema.SUPERFICIE);

            add(cabecera, BorderLayout.NORTH);
            add(grilla, BorderLayout.CENTER);
            refrescar();
        }

        LocalDate getSeleccionado() {
            return seleccionado;
        }

        private void refrescar() {
            lblMes.setText(capitalizar(mes.getMonth().getDisplayName(TextStyle.FULL, ES)) + " " + mes.getYear());
            grilla.removeAll();

            for (String d : new String[]{"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"}) {
                JLabel l = new JLabel(d, SwingConstants.CENTER);
                l.setFont(Tema.BOTON);
                l.setForeground(Tema.TEXTO_SECUNDARIO);
                grilla.add(l);
            }

            LocalDate primero = mes.atDay(1);
            int offset = primero.getDayOfWeek().getValue() % 7; // Lun=1..Dom=7 -> alinear Dom=0
            for (int i = 0; i < offset; i++) {
                JPanel vacio = new JPanel();
                vacio.setBackground(Tema.SUPERFICIE);
                grilla.add(vacio);
            }

            for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
                grilla.add(celda(mes.atDay(dia)));
            }

            grilla.revalidate();
            grilla.repaint();
        }

        private JButton celda(LocalDate fecha) {
            JButton b = new JButton(String.valueOf(fecha.getDayOfMonth()));
            b.setFont(Tema.TEXTO_CHICO);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(38, 32));

            if (fecha.equals(seleccionado)) {
                b.setBackground(Tema.PRIMARIO);
                b.setForeground(Color.WHITE);
            } else if (fecha.equals(LocalDate.now())) {
                b.setBackground(Tema.ACENTO_AZUL.insignia);
                b.setForeground(Tema.TEXTO_PRIMARIO);
            } else {
                b.setBackground(Tema.SUPERFICIE);
                b.setForeground(Tema.TEXTO_PRIMARIO);
            }

            b.addActionListener(e -> {
                seleccionado = fecha;
                refrescar();
                alSeleccionar.accept(fecha);
            });
            return b;
        }

        private static String capitalizar(String s) {
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
