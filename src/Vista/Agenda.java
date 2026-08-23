package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import util.Auditoria;
import util.Sesion;

public class Agenda extends JFrame {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] ESTADOS = {"programado", "completado", "cancelado", "ausente"};

    private JComboBox<PacienteItem> cmbPaciente;
    private JComboBox<PsicologoItem> cmbPsicologo;
    private JTextField txtFechaHora;
    private JComboBox<String> cmbDuracion;
    private JComboBox<String> cmbEstado;
    private JTextField txtNotas;

    private JTable tablaTurnos;
    private DefaultTableModel modeloTabla;

    private Integer turnoIdActual;

    public Agenda() {
        setTitle("Agenda de Turnos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(Tema.iconoApp());
        initComponents();
        cargarPacientes();
        cargarPsicologos();
        cargarTurnos();
        setSize(760, 600);
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Tema.SUPERFICIE);
        panelForm.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbPaciente = new JComboBox<>();
        cmbPsicologo = new JComboBox<>();
        txtFechaHora = new JTextField(20);
        cmbDuracion = new JComboBox<>(new String[]{"30", "45", "60"});
        cmbDuracion.setSelectedItem("45");
        cmbEstado = new JComboBox<>(ESTADOS);
        txtNotas = new JTextField(20);

        int fila = 0;
        agregarCampo(panelForm, gbc, fila++, "Paciente*", cmbPaciente);
        agregarCampo(panelForm, gbc, fila++, "Psicólogo*", cmbPsicologo);
        agregarCampo(panelForm, gbc, fila++, "Fecha y hora (yyyy-mm-dd HH:mm)*", txtFechaHora);
        agregarCampo(panelForm, gbc, fila++, "Duración (min)", cmbDuracion);
        agregarCampo(panelForm, gbc, fila++, "Estado", cmbEstado);
        agregarCampo(panelForm, gbc, fila++, "Notas", txtNotas);

        JButton btnGuardar = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardar.addActionListener(evt -> guardarTurno());

        JButton btnNuevo = Tema.botonPrimario("Nuevo turno", Icono.NUEVO);
        btnNuevo.addActionListener(evt -> limpiarCampos());

        JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
        btnVolver.addActionListener(evt -> volverAlMenu());
        Tema.atajoEscape(this, btnVolver, this::volverAlMenu);

        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(Tema.SUPERFICIE);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnNuevo);
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
        scrollTabla.setBorder(javax.swing.BorderFactory.createTitledBorder("Turnos"));

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(Tema.SUPERFICIE);
        panelSuperior.add(panelForm, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(panelSuperior, BorderLayout.NORTH);
        getContentPane().add(scrollTabla, BorderLayout.CENTER);
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

        String fechaTexto = txtFechaHora.getText().trim();
        Timestamp fechaHora;
        try {
            fechaHora = Timestamp.valueOf(LocalDateTime.parse(fechaTexto, FORMATO_FECHA));
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "La fecha y hora deben tener el formato yyyy-mm-dd HH:mm");
            return;
        }

        int duracion = Integer.parseInt((String) cmbDuracion.getSelectedItem());
        String estado = (String) cmbEstado.getSelectedItem();
        String notas = txtNotas.getText().trim();

        boolean esEdicion = turnoIdActual != null;

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
            cargarTurnos();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void cargarTurnos() {

        modeloTabla.setRowCount(0);

        String sql =
            "SELECT t.id, CONCAT(p.nombre,' ',p.apellido) AS paciente, u.nombre AS psicologo, t.fecha_hora, t.duracion_minutos, t.estado "
            + "FROM turnos t JOIN pacientes p ON p.id = t.paciente_id JOIN usuarios u ON u.id = t.psicologo_id ";

        if (Sesion.esPsicologo()) {
            sql += "WHERE t.psicologo_id = ? ";
        }

        sql += "ORDER BY t.fecha_hora ASC";

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {

                if (Sesion.esPsicologo()) {
                    ps.setInt(1, Sesion.getUsuarioId());
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
                txtFechaHora.setText(FORMATO_FECHA.format(rs.getTimestamp("fecha_hora").toLocalDateTime()));
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
        txtFechaHora.setText("");
        cmbDuracion.setSelectedItem("45");
        cmbEstado.setSelectedItem("programado");
        txtNotas.setText("");
    }

    private void volverAlMenu() {
        MenuPrincipal menu = new MenuPrincipal();
        menu.setVisible(true);
        this.dispose();
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
}
