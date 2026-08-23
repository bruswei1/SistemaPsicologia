package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import util.Auditoria;
import util.Sesion;

public class Agenda extends JPanel {

    private static final String PATRON_FECHA_HORA = "dd/MM/yyyy HH:mm";
    private static final String[] ESTADOS = {"programado", "completado", "cancelado", "ausente"};

    private final Runnable alVolver;

    private JComboBox<PacienteItem> cmbPaciente;
    private JComboBox<PsicologoItem> cmbPsicologo;
    private JSpinner spinnerFechaHora;
    private JComboBox<String> cmbDuracion;
    private JComboBox<String> cmbEstado;
    private JTextField txtNotas;

    private JTable tablaTurnos;
    private DefaultTableModel modeloTabla;
    private CardLayout cardLayoutVista;
    private JPanel panelVista;
    private PanelCalendario panelCalendario;
    private JButton btnVerCalendario;
    private boolean vistaCalendario;
    private LocalDate filtroDia;
    private javax.swing.border.TitledBorder bordeTabla;

    private Integer turnoIdActual;

    public Agenda(Runnable alVolver) {
        this.alVolver = alVolver;
        initComponents();
        cargarPacientes();
        cargarPsicologos();
        cargarTurnos();
    }

    public void refrescar() {
        cargarPacientes();
        cargarPsicologos();
        cargarTurnos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Tema.SUPERFICIE);
        panelForm.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbPaciente = new JComboBox<>();
        cmbPsicologo = new JComboBox<>();
        spinnerFechaHora = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editorFecha = new JSpinner.DateEditor(spinnerFechaHora, PATRON_FECHA_HORA);
        spinnerFechaHora.setEditor(editorFecha);
        cmbDuracion = new JComboBox<>(new String[]{"30", "45", "60"});
        cmbDuracion.setSelectedItem("45");
        cmbEstado = new JComboBox<>(ESTADOS);
        txtNotas = new JTextField(20);

        int fila = 0;
        agregarCampo(panelForm, gbc, fila++, "Paciente*", cmbPaciente);
        agregarCampo(panelForm, gbc, fila++, "Psicólogo*", cmbPsicologo);
        agregarCampo(panelForm, gbc, fila++, "Fecha y hora*", spinnerFechaHora);
        agregarCampo(panelForm, gbc, fila++, "Duración (min)", cmbDuracion);
        agregarCampo(panelForm, gbc, fila++, "Estado", cmbEstado);
        agregarCampo(panelForm, gbc, fila++, "Notas", txtNotas);

        JButton btnGuardar = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardar.addActionListener(evt -> guardarTurno());

        JButton btnNuevo = Tema.botonPrimario("Nuevo turno", Icono.NUEVO);
        btnNuevo.addActionListener(evt -> limpiarCampos());

        JButton btnEliminar = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminar.addActionListener(evt -> eliminarTurno());

        JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
        btnVolver.addActionListener(evt -> alVolver.run());
        Tema.atajoEscape(this, btnVolver, alVolver);

        btnVerCalendario = Tema.botonSecundario("Ver calendario", Icono.AGENDA);
        btnVerCalendario.addActionListener(evt -> {
            vistaCalendario = !vistaCalendario;
            cardLayoutVista.show(panelVista, vistaCalendario ? "calendario" : "lista");
            btnVerCalendario.setText(vistaCalendario ? "Ver lista" : "Ver calendario");
            if (vistaCalendario) {
                panelCalendario.refrescar();
            }
        });

        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(Tema.SUPERFICIE);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnNuevo);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnVerCalendario);
        panelBotones.add(btnVolver);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Paciente", "Psicólogo", "Fecha y hora", "Duración", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaTurnos = new JTable(modeloTabla);
        Tema.estilizarTabla(tablaTurnos);
        tablaTurnos.getColumnModel().getColumn(5).setCellRenderer(Tema.rendererEstado());
        tablaTurnos.getSelectionModel().addListSelectionListener(this::onSeleccionarTurno);

        JScrollPane scrollTabla = new JScrollPane(tablaTurnos);
        bordeTabla = BorderFactory.createTitledBorder("Turnos");
        scrollTabla.setBorder(bordeTabla);

        panelCalendario = new PanelCalendario(this::filtrarPorDia);

        cardLayoutVista = new CardLayout();
        panelVista = new JPanel(cardLayoutVista);
        panelVista.add(scrollTabla, "lista");
        panelVista.add(panelCalendario, "calendario");

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(Tema.SUPERFICIE);
        panelSuperior.add(panelForm, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);

        add(panelSuperior, BorderLayout.NORTH);
        add(panelVista, BorderLayout.CENTER);
    }

    private void filtrarPorDia(LocalDate dia) {
        filtroDia = dia;
        vistaCalendario = false;
        cardLayoutVista.show(panelVista, "lista");
        btnVerCalendario.setText("Ver calendario");
        cargarTurnos();
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int fila, String etiqueta, java.awt.Component campo) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(etiqueta), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
    }

    private void cargarPacientes() {

        cmbPaciente.removeAllItems();

        String sql = "SELECT id, nombre, apellido FROM pacientes ";
        if (Sesion.esPsicologo()) {
            sql += "WHERE psicologo_id = ? ";
        }
        sql += "ORDER BY nombre";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            if (Sesion.esPsicologo()) {
                ps.setInt(1, Sesion.getUsuarioId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cmbPaciente.addItem(new PacienteItem(rs.getInt("id"), rs.getString("nombre") + " " + rs.getString("apellido")));
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar pacientes: " + e.getMessage());
        }
    }

    private void cargarPsicologos() {

        cmbPsicologo.removeAllItems();

        if (Sesion.esPsicologo()) {
            cmbPsicologo.addItem(new PsicologoItem(Sesion.getUsuarioId(), Sesion.getNombre()));
            cmbPsicologo.setSelectedIndex(0);
            cmbPsicologo.setEnabled(false);
            return;
        }

        String sql = "SELECT id, nombre FROM usuarios WHERE rol='psicologo' AND activo=1 ORDER BY nombre";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cmbPsicologo.addItem(new PsicologoItem(rs.getInt("id"), rs.getString("nombre")));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar psicólogos: " + e.getMessage());
        }
    }

    private void guardarTurno() {

        PacienteItem paciente = (PacienteItem) cmbPaciente.getSelectedItem();
        PsicologoItem psicologo = (PsicologoItem) cmbPsicologo.getSelectedItem();

        if (paciente == null || psicologo == null) {
            JOptionPane.showMessageDialog(this, "Selecciona paciente y psicólogo");
            return;
        }

        Timestamp fechaHora = new Timestamp(((java.util.Date) spinnerFechaHora.getValue()).getTime());
        int duracion = Integer.parseInt((String) cmbDuracion.getSelectedItem());
        String estado = (String) cmbEstado.getSelectedItem();
        String notas = txtNotas.getText().trim();

        boolean esEdicion = turnoIdActual != null;

        if (hayTurnoSolapado(psicologo.id, fechaHora, duracion, esEdicion ? turnoIdActual : null)) {
            int opcion = JOptionPane.showConfirmDialog(this,
                "Este horario se superpone con otro turno de " + psicologo.nombre + ". ¿Guardar de todos modos?",
                "Turnos superpuestos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String sql = esEdicion
            ? "UPDATE turnos SET paciente_id=?, psicologo_id=?, fecha_hora=?, duracion_minutos=?, estado=?, notas=? WHERE id=?"
            : "INSERT INTO turnos (paciente_id, psicologo_id, fecha_hora, duracion_minutos, estado, notas) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setInt(1, paciente.id);
                ps.setInt(2, psicologo.id);
                ps.setTimestamp(3, fechaHora);
                ps.setInt(4, duracion);
                ps.setString(5, estado);

                if (notas.isEmpty()) {
                    ps.setNull(6, Types.VARCHAR);
                } else {
                    ps.setString(6, notas);
                }

                if (esEdicion) {
                    ps.setInt(7, turnoIdActual);
                }

                ps.executeUpdate();
            }

            if (esEdicion) {
                Auditoria.registrar(cn, "EDITAR_TURNO", "turnos", turnoIdActual, null);
            } else {
                try (PreparedStatement ps = cn.prepareStatement("SELECT LAST_INSERT_ID() AS id");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    Auditoria.registrar(cn, "CREAR_TURNO", "turnos", rs.getInt("id"), null);
                }
            }

            JOptionPane.showMessageDialog(this, esEdicion ? "Turno actualizado" : "Turno agendado");
            limpiarCampos();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarTurno() {
        if (turnoIdActual == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un turno de la tabla para eliminar");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(this, "¿Eliminar definitivamente este turno?",
            "Eliminar turno", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM turnos WHERE id=?")) {
                ps.setInt(1, turnoIdActual);
                ps.executeUpdate();
            }

            Auditoria.registrar(cn, "ELIMINAR_TURNO", "turnos", turnoIdActual, null);

            JOptionPane.showMessageDialog(this, "Turno eliminado");
            limpiarCampos();

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                "No se puede eliminar: este turno tiene una sesión registrada. Cambiá el estado a \"cancelado\" en vez de borrarlo.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void cargarTurnos() {

        modeloTabla.setRowCount(0);
        bordeTabla.setTitle(filtroDia != null
            ? "Turnos del " + filtroDia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (clic en \"Quitar filtro\" en el calendario para ver todos)"
            : "Turnos");

        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT t.id, CONCAT(p.nombre,' ',p.apellido) AS paciente, u.nombre AS psicologo, t.fecha_hora, t.duracion_minutos, t.estado "
            + "FROM turnos t JOIN pacientes p ON p.id = t.paciente_id JOIN usuarios u ON u.id = t.psicologo_id WHERE 1=1 ");

        if (Sesion.esPsicologo()) {
            sqlBuilder.append("AND t.psicologo_id = ? ");
        }
        if (filtroDia != null) {
            sqlBuilder.append("AND DATE(t.fecha_hora) = ? ");
        }
        sqlBuilder.append("ORDER BY t.fecha_hora ASC");

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sqlBuilder.toString())) {

                int indice = 1;
                if (Sesion.esPsicologo()) {
                    ps.setInt(indice++, Sesion.getUsuarioId());
                }
                if (filtroDia != null) {
                    ps.setDate(indice, java.sql.Date.valueOf(filtroDia));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modeloTabla.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("paciente"),
                            rs.getString("psicologo"),
                            rs.getTimestamp("fecha_hora"),
                            rs.getInt("duracion_minutos"),
                            rs.getString("estado")
                        });
                    }
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar turnos: " + e.getMessage());
        }
    }

    private void onSeleccionarTurno(ListSelectionEvent evt) {

        if (evt.getValueIsAdjusting()) {
            return;
        }

        int fila = tablaTurnos.getSelectedRow();
        if (fila < 0) {
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        cargarTurno(id);
    }

    private void cargarTurno(int id) {

        String sql = "SELECT * FROM turnos WHERE id=?";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return;
                }

                turnoIdActual = id;
                seleccionarPaciente(rs.getInt("paciente_id"));
                seleccionarPsicologo(rs.getInt("psicologo_id"));
                spinnerFechaHora.setValue(rs.getTimestamp("fecha_hora"));
                cmbDuracion.setSelectedItem(String.valueOf(rs.getInt("duracion_minutos")));
                cmbEstado.setSelectedItem(rs.getString("estado"));
                txtNotas.setText(rs.getString("notas") != null ? rs.getString("notas") : "");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar el turno: " + e.getMessage());
        }
    }

    private void seleccionarPaciente(int pacienteId) {
        for (int i = 0; i < cmbPaciente.getItemCount(); i++) {
            if (cmbPaciente.getItemAt(i).id == pacienteId) {
                cmbPaciente.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarPsicologo(int psicologoId) {
        for (int i = 0; i < cmbPsicologo.getItemCount(); i++) {
            if (cmbPsicologo.getItemAt(i).id == psicologoId) {
                cmbPsicologo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void limpiarCampos() {
        turnoIdActual = null;
        tablaTurnos.clearSelection();
        if (cmbPaciente.getItemCount() > 0) {
            cmbPaciente.setSelectedIndex(0);
        }
        if (!Sesion.esPsicologo() && cmbPsicologo.getItemCount() > 0) {
            cmbPsicologo.setSelectedIndex(0);
        }
        spinnerFechaHora.setValue(new java.util.Date());
        cmbDuracion.setSelectedItem("45");
        cmbEstado.setSelectedItem("programado");
        txtNotas.setText("");
        filtroDia = null;
        cargarTurnos();
    }

    /** true si [inicio, inicio+duracion) se superpone con otro turno activo del mismo psicólogo. */
    private boolean hayTurnoSolapado(int psicologoId, Timestamp inicio, int duracionMinutos, Integer excluirId) {
        String sql = "SELECT COUNT(*) AS total FROM turnos "
            + "WHERE psicologo_id = ? AND estado <> 'cancelado' "
            + (excluirId != null ? "AND id <> ? " : "")
            + "AND fecha_hora < ? "
            + "AND DATE_ADD(fecha_hora, INTERVAL duracion_minutos MINUTE) > ?";

        Timestamp fin = new Timestamp(inicio.getTime() + duracionMinutos * 60_000L);

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            int indice = 1;
            ps.setInt(indice++, psicologoId);
            if (excluirId != null) {
                ps.setInt(indice++, excluirId);
            }
            ps.setTimestamp(indice++, fin);
            ps.setTimestamp(indice, inicio);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }

        } catch (Exception e) {
            System.out.println("Error verificando solapamiento: " + e.getMessage());
            return false;
        }
    }

    private static class PacienteItem {
        final int id;
        final String nombre;

        PacienteItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static class PsicologoItem {
        final int id;
        final String nombre;

        PsicologoItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    /** Vista de calendario mensual: una tarjeta por día con la cantidad de turnos; clic filtra la lista. */
    private static class PanelCalendario extends JPanel {
        private final Consumer<LocalDate> alSeleccionarDia;
        private YearMonth mesActual = YearMonth.now();
        private JLabel lblMes;
        private JPanel grilla;

        PanelCalendario(Consumer<LocalDate> alSeleccionarDia) {
            this.alSeleccionarDia = alSeleccionarDia;
            setLayout(new BorderLayout(10, 10));
            setBackground(Tema.SUPERFICIE);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

            JPanel cabecera = new JPanel(new BorderLayout());
            cabecera.setBackground(Tema.SUPERFICIE);

            JButton btnAnterior = Tema.botonSecundario("◀");
            btnAnterior.addActionListener(e -> {
                mesActual = mesActual.minusMonths(1);
                refrescar();
            });
            JButton btnSiguiente = Tema.botonSecundario("▶");
            btnSiguiente.addActionListener(e -> {
                mesActual = mesActual.plusMonths(1);
                refrescar();
            });
            JButton btnQuitarFiltro = Tema.botonPrimario("Ver todos los turnos");
            btnQuitarFiltro.addActionListener(e -> alSeleccionarDia.accept(null));

            lblMes = new JLabel("");
            lblMes.setFont(Tema.SUBTITULO);
            lblMes.setForeground(Tema.TEXTO_PRIMARIO);
            lblMes.setHorizontalAlignment(JLabel.CENTER);

            JPanel navegacion = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            navegacion.setBackground(Tema.SUPERFICIE);
            navegacion.add(btnAnterior);
            navegacion.add(lblMes);
            navegacion.add(btnSiguiente);

            cabecera.add(navegacion, BorderLayout.CENTER);
            cabecera.add(btnQuitarFiltro, BorderLayout.EAST);
            add(cabecera, BorderLayout.NORTH);

            grilla = new JPanel(new GridLayout(0, 7, 4, 4));
            grilla.setBackground(Tema.SUPERFICIE);
            add(grilla, BorderLayout.CENTER);

            refrescar();
        }

        void refrescar() {
            lblMes.setText(capitalizar(mesActual.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")))
                + " " + mesActual.getYear());

            Map<Integer, Integer> turnosPorDia = cargarConteoDelMes();

            grilla.removeAll();
            String[] diasSemana = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
            for (String d : diasSemana) {
                JLabel lbl = new JLabel(d, JLabel.CENTER);
                lbl.setFont(Tema.BOTON);
                lbl.setForeground(Tema.TEXTO_SECUNDARIO);
                grilla.add(lbl);
            }

            LocalDate primerDia = mesActual.atDay(1);
            int offset = primerDia.getDayOfWeek().getValue() % 7; // getDayOfWeek: Lun=1..Dom=7 -> alinear Dom=0
            for (int i = 0; i < offset; i++) {
                JPanel vacio = new JPanel();
                vacio.setBackground(Tema.SUPERFICIE);
                grilla.add(vacio);
            }

            int totalDias = mesActual.lengthOfMonth();
            for (int dia = 1; dia <= totalDias; dia++) {
                LocalDate fecha = mesActual.atDay(dia);
                int cantidad = turnosPorDia.getOrDefault(dia, 0);
                grilla.add(crearCeldaDia(fecha, cantidad));
            }

            grilla.revalidate();
            grilla.repaint();
        }

        private String capitalizar(String s) {
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        private JPanel crearCeldaDia(LocalDate fecha, int cantidadTurnos) {
            boolean hoy = fecha.equals(LocalDate.now());
            JPanel celda = new JPanel(new BorderLayout());
            celda.setBackground(hoy ? Tema.ACENTO_AZUL.insignia : Tema.SUPERFICIE);
            celda.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            celda.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

            JLabel lblDia = new JLabel(String.valueOf(fecha.getDayOfMonth()));
            lblDia.setFont(Tema.TEXTO_CHICO);
            lblDia.setForeground(Tema.TEXTO_PRIMARIO);
            lblDia.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));
            celda.add(lblDia, BorderLayout.NORTH);

            if (cantidadTurnos > 0) {
                JLabel lblCantidad = new JLabel(cantidadTurnos + (cantidadTurnos == 1 ? " turno" : " turnos"));
                lblCantidad.setFont(Tema.TEXTO_ITALICA_CHICA);
                lblCantidad.setForeground(Tema.ACENTO_AZUL.icono);
                lblCantidad.setHorizontalAlignment(JLabel.CENTER);
                celda.add(lblCantidad, BorderLayout.SOUTH);
            }

            celda.setPreferredSize(new java.awt.Dimension(0, 55));
            celda.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    alSeleccionarDia.accept(fecha);
                }
            });

            return celda;
        }

        private Map<Integer, Integer> cargarConteoDelMes() {
            Map<Integer, Integer> resultado = new HashMap<>();
            LocalDate desde = mesActual.atDay(1);
            LocalDate hasta = mesActual.atEndOfMonth();

            String sql = "SELECT DAY(fecha_hora) AS dia, COUNT(*) AS total FROM turnos "
                + "WHERE fecha_hora >= ? AND fecha_hora < ? ";
            if (Sesion.esPsicologo()) {
                sql += "AND psicologo_id = ? ";
            }
            sql += "GROUP BY dia";

            try (Connection cn = new Conexion().conectar();
                 PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
                if (Sesion.esPsicologo()) {
                    ps.setInt(3, Sesion.getUsuarioId());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultado.put(rs.getInt("dia"), rs.getInt("total"));
                    }
                }

            } catch (Exception e) {
                System.out.println("Error cargando conteo del calendario: " + e.getMessage());
            }

            return resultado;
        }
    }
}
