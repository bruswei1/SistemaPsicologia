package Vista;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Línea de tiempo de un estudiante: atenciones, entradas de seguimiento, citas y archivos
 * adjuntos en un solo recorrido cronológico (lo más nuevo arriba, agrupado por mes), para ver
 * de un vistazo la historia del caso sin saltar entre pestañas. Tocar un evento lleva a él.
 *
 * Solo muestra metadatos (fecha, tipo, profesional, duración, motivo corto), nunca el texto
 * completo de una atención. Para "Usuario autorizado" (secretaria) incluye solo las citas: el
 * resto es material clínico que ese rol no ve.
 */
public class LineaDeTiempo extends JPanel {

    public enum Tipo {
        ATENCION("Atenciones", Icono.SESIONES),
        SEGUIMIENTO("Seguimiento", Icono.HISTORIA),
        CITA("Citas", Icono.AGENDA),
        ADJUNTO("Adjuntos", Icono.CARPETA);

        final String etiqueta;
        final Icono icono;

        Tipo(String etiqueta, Icono icono) {
            this.etiqueta = etiqueta;
            this.icono = icono;
        }
    }

    public static final class Evento {
        public final Tipo tipo;
        public final int id;
        public final LocalDateTime fecha;
        public final String titulo;
        public final String detalle;
        public final boolean destacado;

        Evento(Tipo tipo, int id, LocalDateTime fecha, String titulo, String detalle, boolean destacado) {
            this.tipo = tipo;
            this.id = id;
            this.fecha = fecha;
            this.titulo = titulo;
            this.detalle = detalle;
            this.destacado = destacado;
        }
    }

    private static final Locale ES = new Locale("es");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final Consumer<Evento> alElegir;
    private final JPanel lista = new PanelAnchoDelScroll();
    private final JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private List<Evento> eventos = new ArrayList<>();
    private Tipo filtro;

    public LineaDeTiempo(Consumer<Evento> alElegir) {
        super(new BorderLayout(0, 10));
        this.alElegir = alElegir;
        setOpaque(false);

        filtros.setOpaque(false);
        add(filtros, BorderLayout.NORTH);

        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        mostrarMensaje("Cargando…");
    }

    public void setEventos(List<Evento> nuevos) {
        eventos = nuevos;
        armarFiltros();
        armarLista();
    }

    public void mostrarMensaje(String texto) {
        lista.removeAll();
        JLabel lbl = new JLabel(texto);
        lbl.setFont(Tema.fuente(Font.ITALIC, 13));
        lbl.setForeground(Tema.TEXTO_SECUNDARIO);
        lbl.setBorder(BorderFactory.createEmptyBorder(20, 6, 0, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lista.add(lbl);
        lista.revalidate();
        lista.repaint();
    }

    // ==================== Filtros (chips con cantidad) ====================

    private void armarFiltros() {
        filtros.removeAll();
        Map<Tipo, Integer> cantidades = new EnumMap<>(Tipo.class);
        for (Evento e : eventos) {
            cantidades.merge(e.tipo, 1, Integer::sum);
        }
        filtros.add(chip("Todo · " + eventos.size(), null));
        for (Tipo t : Tipo.values()) {
            Integer n = cantidades.get(t);
            if (n != null) {
                filtros.add(chip(t.etiqueta + " · " + n, t));
            }
        }
        filtros.revalidate();
        filtros.repaint();
    }

    private JComponent chip(String texto, Tipo tipo) {
        JToggleButton b = new JToggleButton(texto, tipo == filtro);
        b.setFont(Tema.fuente(Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addActionListener(e -> {
            filtro = tipo;
            armarFiltros();
            armarLista();
        });
        return b;
    }

    // ==================== Lista agrupada por mes ====================

    private void armarLista() {
        lista.removeAll();
        String mesActual = null;
        int mostrados = 0;
        for (Evento e : eventos) {
            if (filtro != null && e.tipo != filtro) {
                continue;
            }
            String mes = capitalizar(e.fecha.getMonth().getDisplayName(TextStyle.FULL, ES)) + " " + e.fecha.getYear();
            if (!mes.equals(mesActual)) {
                mesActual = mes;
                JLabel lblMes = new JLabel(mes.toUpperCase());
                lblMes.setFont(Tema.fuente(Font.BOLD, 11));
                lblMes.setForeground(Tema.TEXTO_SECUNDARIO);
                lblMes.setBorder(BorderFactory.createEmptyBorder(mostrados == 0 ? 2 : 14, 4, 6, 0));
                lblMes.setAlignmentX(Component.LEFT_ALIGNMENT);
                lista.add(lblMes);
            }
            FilaEvento fila = new FilaEvento(e);
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);
            lista.add(fila);
            mostrados++;
        }
        if (mostrados == 0) {
            mostrarMensaje(eventos.isEmpty()
                ? "Todavía no hay nada registrado para este estudiante."
                : "No hay eventos de este tipo.");
            return;
        }
        lista.add(Box.createVerticalGlue());
        lista.revalidate();
        lista.repaint();
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Color colorDe(Tipo t) {
        switch (t) {
            case ATENCION: return Tema.ACENTO_MORADO.icono;
            case SEGUIMIENTO: return Tema.ACENTO_AZUL.icono;
            case CITA: return Tema.ACENTO_VERDE.icono;
            default: return Tema.ACENTO_AMBAR.icono;
        }
    }

    /** Un evento: fecha a la izquierda, punto de color sobre la línea vertical, título y detalle. */
    private final class FilaEvento extends JComponent {
        private final Evento evento;
        private boolean hover;

        FilaEvento(Evento evento) {
            this.evento = evento;
            boolean navegable = evento.tipo != Tipo.CITA;
            setCursor(Cursor.getPredefinedCursor(navegable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            setToolTipText(navegable ? "Clic para abrirlo" : "Las citas se gestionan en la Agenda");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (navegable) {
                        alElegir.accept(evento);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 54);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 54);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color color = colorDe(evento.tipo);

            if (hover && evento.tipo != Tipo.CITA) {
                g2.setColor(Tema.mezclar(Tema.SUPERFICIE, color, Tema.esOscuro() ? 0.16f : 0.07f));
                g2.fillRoundRect(0, 1, w - 1, h - 2, 12, 12);
            }

            // Fecha: día + mes corto arriba, hora abajo.
            String dia = evento.fecha.getDayOfMonth() + " " + evento.fecha.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
            g2.setFont(Tema.fuente(Font.BOLD, 12));
            g2.setColor(Tema.TEXTO_PRIMARIO);
            g2.drawString(dia, 8, 23);
            g2.setFont(Tema.TEXTO_CHICO);
            g2.setColor(Tema.TEXTO_SECUNDARIO);
            g2.drawString(evento.fecha.format(HORA), 8, 40);

            // Línea vertical + insignia con el ícono del tipo.
            int xLinea = 86;
            g2.setColor(Tema.BORDE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(xLinea, 0, xLinea, h);
            int d = 26;
            g2.setColor(Tema.esOscuro() ? Tema.oscurecer(color, 0.35f) : Tema.mezclar(Color.WHITE, color, 0.18f));
            g2.fillOval(xLinea - d / 2, (h - d) / 2, d, d);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(xLinea - d / 2, (h - d) / 2, d, d);
            evento.tipo.icono.dibujar(g2, xLinea - 7, (h - 14) / 2, 14, color);

            // Título (+ etiqueta destacada) y detalle.
            int xTexto = xLinea + 24;
            g2.setFont(Tema.fuente(Font.BOLD, 13));
            g2.setColor(Tema.TEXTO_PRIMARIO);
            FontMetrics fmTitulo = g2.getFontMetrics();
            g2.drawString(evento.titulo, xTexto, 23);
            if (evento.destacado) {
                String marca = evento.tipo == Tipo.CITA ? "PRÓXIMA" : "REITERADO";
                Color colorMarca = evento.tipo == Tipo.CITA ? Tema.PRIMARIO : Tema.PELIGRO;
                g2.setFont(Tema.fuente(Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int xm = xTexto + fmTitulo.stringWidth(evento.titulo) + 8;
                g2.setColor(Tema.mezclar(Tema.SUPERFICIE, colorMarca, 0.15f));
                g2.fillRoundRect(xm, 11, fm.stringWidth(marca) + 10, 16, 8, 8);
                g2.setColor(colorMarca);
                g2.drawString(marca, xm + 5, 23);
            }
            g2.setFont(Tema.TEXTO_CHICO);
            g2.setColor(Tema.TEXTO_SECUNDARIO);
            String detalle = recortar(evento.detalle, g2.getFontMetrics(), w - xTexto - 12);
            g2.drawString(detalle, xTexto, 41);
            g2.dispose();
        }

        private String recortar(String texto, FontMetrics fm, int ancho) {
            if (texto == null) {
                return "";
            }
            if (fm.stringWidth(texto) <= ancho) {
                return texto;
            }
            String t = texto;
            while (t.length() > 1 && fm.stringWidth(t + "…") > ancho) {
                t = t.substring(0, t.length() - 1);
            }
            return t + "…";
        }
    }

    /** Toma el ancho visible del scroll (solo scroll vertical). */
    private static class PanelAnchoDelScroll extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ==================== Carga (en segundo plano) ====================

    /**
     * Arma los eventos del estudiante, más nuevos primero. Pensado para el hilo de fondo de
     * {@link Tema#enSegundoPlano}.
     *
     * @param incluirClinico false para "Usuario autorizado": solo citas.
     */
    public static List<Evento> cargar(int pacienteId, boolean incluirClinico) throws SQLException {
        List<Evento> eventos = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        try (Connection cn = new conexion.Conexion().conectar()) {
            if (cn == null) {
                throw new SQLException("No se pudo conectar a la base de datos");
            }

            // Citas (todas menos canceladas). La próxima pendiente se destaca.
            String sqlCitas = "SELECT t.id, t.fecha_hora, t.estado, t.duracion_minutos, u.nombre AS profesional "
                + "FROM turnos t LEFT JOIN usuarios u ON u.id = t.psicologo_id "
                + "WHERE t.paciente_id = ? AND t.estado <> 'cancelado'";
            Evento proximaCita = null;
            try (PreparedStatement ps = cn.prepareStatement(sqlCitas)) {
                ps.setInt(1, pacienteId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime f = rs.getTimestamp("fecha_hora").toLocalDateTime();
                        String estado = rs.getString("estado");
                        boolean futura = "programado".equals(estado) && f.isAfter(ahora);
                        Evento e = new Evento(Tipo.CITA, rs.getInt("id"), f,
                            futura ? "Cita programada" : "Cita · " + Tema.etiquetaEstadoCita(estado),
                            rs.getInt("duracion_minutos") + " min" + conProfesional(rs.getString("profesional")), false);
                        if (futura && (proximaCita == null || f.isBefore(proximaCita.fecha))) {
                            proximaCita = e;
                        }
                        eventos.add(e);
                    }
                }
            }
            if (proximaCita != null) {
                eventos.set(eventos.indexOf(proximaCita), new Evento(Tipo.CITA, proximaCita.id, proximaCita.fecha,
                    proximaCita.titulo, proximaCita.detalle, true));
            }

            if (incluirClinico) {
                String sqlAtenciones = "SELECT s.id, s.fecha, s.duracion_minutos, s.duracion_segundos, s.turno_id, u.nombre AS profesional "
                    + "FROM sesiones s LEFT JOIN usuarios u ON u.id = s.psicologo_id WHERE s.paciente_id = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlAtenciones)) {
                    ps.setInt(1, pacienteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Timestamp f = rs.getTimestamp("fecha");
                            if (f == null) {
                                continue;
                            }
                            Object min = rs.getObject("duracion_minutos");
                            Object seg = rs.getObject("duracion_segundos");
                            String duracion;
                            if (min == null) {
                                duracion = "sin duración registrada";
                            } else if (((Number) min).intValue() == 0) {
                                duracion = (seg != null ? ((Number) seg).intValue() : 0) + " s";
                            } else {
                                duracion = min + " min";
                            }
                            eventos.add(new Evento(Tipo.ATENCION, rs.getInt("id"), f.toLocalDateTime(),
                                "Atención registrada",
                                duracion + (rs.getObject("turno_id") != null ? " · con cita" : "") + conProfesional(rs.getString("profesional")),
                                false));
                        }
                    }
                }

                String sqlSeguimiento = "SELECT h.id, h.fecha_creacion, h.motivo_consulta, h.es_reiterado, u.nombre AS profesional "
                    + "FROM historia_psicologica h LEFT JOIN usuarios u ON u.id = h.psicologo_id WHERE h.paciente_id = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlSeguimiento)) {
                    ps.setInt(1, pacienteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Timestamp f = rs.getTimestamp("fecha_creacion");
                            if (f == null) {
                                continue;
                            }
                            String motivo = rs.getString("motivo_consulta");
                            motivo = motivo != null && !motivo.trim().isEmpty()
                                ? motivo.trim().replaceAll("\\s+", " ") : "Sin motivo cargado";
                            eventos.add(new Evento(Tipo.SEGUIMIENTO, rs.getInt("id"), f.toLocalDateTime(),
                                "Entrada de seguimiento", motivo + conProfesional(rs.getString("profesional")),
                                rs.getBoolean("es_reiterado")));
                        }
                    }
                }

                String sqlAdjuntos = "SELECT d.id, d.subido_en, d.nombre_archivo, u.nombre AS profesional "
                    + "FROM documentos_paciente d LEFT JOIN usuarios u ON u.id = d.subido_por WHERE d.paciente_id = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlAdjuntos)) {
                    ps.setInt(1, pacienteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Timestamp f = rs.getTimestamp("subido_en");
                            if (f == null) {
                                continue;
                            }
                            eventos.add(new Evento(Tipo.ADJUNTO, rs.getInt("id"), f.toLocalDateTime(),
                                "Archivo adjuntado", rs.getString("nombre_archivo") + conProfesional(rs.getString("profesional")), false));
                        }
                    }
                }
            }
        }
        eventos.sort((a, b) -> b.fecha.compareTo(a.fecha));
        return eventos;
    }

    private static String conProfesional(String nombre) {
        return nombre != null && !nombre.isEmpty() ? " · " + nombre : "";
    }
}
