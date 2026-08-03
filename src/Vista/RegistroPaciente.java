package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import util.Auditoria;
import util.Sesion;

public class RegistroPaciente extends JFrame {

    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtFechaNacimiento;
    private JComboBox<String> cmbGenero;
    private JTextField txtTelefono;
    private JTextField txtEmail;
    private JTextField txtDireccion;
    private JTextArea txtMotivoConsulta;
    private JComboBox<PsicologoItem> cmbPsicologo;

    private JTextArea txtAntecedentesPersonales;
    private JTextArea txtAntecedentesFamiliares;
    private JTextArea txtAnamnesis;
    private JTabbedPane tabs;

    private JTable tablaPacientes;
    private DefaultTableModel modeloTabla;

    private Integer pacienteIdActual;

    public RegistroPaciente() {
        setTitle("Historia Clínica de Pacientes");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        cargarPsicologos();
        cargarPacientes();
        setSize(760, 620);
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JPanel panelDatos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtNombre = new JTextField(20);
        txtApellido = new JTextField(20);
        txtFechaNacimiento = new JTextField(20);
        cmbGenero = new JComboBox<>(new String[]{"", "Masculino", "Femenino", "Otro"});
        txtTelefono = new JTextField(20);
        txtEmail = new JTextField(20);
        txtDireccion = new JTextField(20);
        txtMotivoConsulta = new JTextArea(3, 20);
        cmbPsicologo = new JComboBox<>();

        int fila = 0;
        agregarCampo(panelDatos, gbc, fila++, "Nombre*", txtNombre);
        agregarCampo(panelDatos, gbc, fila++, "Apellido*", txtApellido);
        agregarCampo(panelDatos, gbc, fila++, "Fecha de nacimiento (yyyy-mm-dd)", txtFechaNacimiento);
        agregarCampo(panelDatos, gbc, fila++, "Género", cmbGenero);
        agregarCampo(panelDatos, gbc, fila++, "Teléfono", txtTelefono);
        agregarCampo(panelDatos, gbc, fila++, "Email", txtEmail);
        agregarCampo(panelDatos, gbc, fila++, "Dirección", txtDireccion);
        agregarCampo(panelDatos, gbc, fila++, "Motivo de consulta", new JScrollPane(txtMotivoConsulta));
        agregarCampo(panelDatos, gbc, fila++, "Psicólogo asignado", cmbPsicologo);

        JPanel panelClinico = new JPanel(new GridBagLayout());
        GridBagConstraints gbcClinico = new GridBagConstraints();
        gbcClinico.insets = new Insets(4, 4, 4, 4);
        gbcClinico.fill = GridBagConstraints.BOTH;
        gbcClinico.weightx = 1;
        gbcClinico.weighty = 1;

        txtAntecedentesPersonales = new JTextArea(4, 20);
        txtAntecedentesFamiliares = new JTextArea(4, 20);
        txtAnamnesis = new JTextArea(4, 20);

        int filaClinico = 0;
        agregarCampoClinico(panelClinico, gbcClinico, filaClinico++, "Antecedentes personales", txtAntecedentesPersonales);
        agregarCampoClinico(panelClinico, gbcClinico, filaClinico++, "Antecedentes familiares", txtAntecedentesFamiliares);
        agregarCampoClinico(panelClinico, gbcClinico, filaClinico++, "Anamnesis", txtAnamnesis);

        tabs = new JTabbedPane();
        tabs.addTab("Datos personales", panelDatos);
        if (!Sesion.esSecretaria()) {
            tabs.addTab("Anamnesis y antecedentes", panelClinico);
        }

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(evt -> guardarPaciente());

        JButton btnNuevo = new JButton("Nuevo paciente");
        btnNuevo.addActionListener(evt -> limpiarCampos());

        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(evt -> volverAlMenu());

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnGuardar);
        panelBotones.add(btnNuevo);
        panelBotones.add(btnVolver);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Apellido", "Psicólogo", "Fecha Nac."}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaPacientes = new JTable(modeloTabla);
        tablaPacientes.getSelectionModel().addListSelectionListener(this::onSeleccionarPaciente);

        JScrollPane scrollTabla = new JScrollPane(tablaPacientes);
        scrollTabla.setBorder(javax.swing.BorderFactory.createTitledBorder("Pacientes registrados"));

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(tabs, BorderLayout.CENTER);
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

    private void agregarCampoClinico(JPanel panel, GridBagConstraints gbc, int fila, String etiqueta, JTextArea area) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        panel.add(new JLabel(etiqueta), gbc);

        gbc.gridy = fila + 1;
        panel.add(new JScrollPane(area), gbc);
    }

    private void cargarPsicologos() {

        cmbPsicologo.removeAllItems();

        if (Sesion.esPsicologo()) {
            cmbPsicologo.addItem(new PsicologoItem(Sesion.getUsuarioId(), Sesion.getNombre()));
            cmbPsicologo.setSelectedIndex(0);
            cmbPsicologo.setEnabled(false);
            return;
        }

        cmbPsicologo.addItem(new PsicologoItem(null, "Sin asignar"));

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

    private void guardarPaciente() {

        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();

        if (nombre.isEmpty() || apellido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nombre y apellido son obligatorios");
            return;
        }

        String fechaTexto = txtFechaNacimiento.getText().trim();
        java.sql.Date fechaNacimiento = null;
        if (!fechaTexto.isEmpty()) {
            try {
                fechaNacimiento = java.sql.Date.valueOf(fechaTexto);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "La fecha de nacimiento debe tener el formato yyyy-mm-dd");
                return;
            }
        }

        String genero = (String) cmbGenero.getSelectedItem();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String motivoConsulta = txtMotivoConsulta.getText().trim();
        String antecedentesPersonales = txtAntecedentesPersonales.getText().trim();
        String antecedentesFamiliares = txtAntecedentesFamiliares.getText().trim();
        String anamnesis = txtAnamnesis.getText().trim();

        PsicologoItem psicologoSeleccionado = (PsicologoItem) cmbPsicologo.getSelectedItem();
        Integer psicologoId = psicologoSeleccionado != null ? psicologoSeleccionado.id : null;

        boolean esEdicion = pacienteIdActual != null;

        String sql = esEdicion
            ? "UPDATE pacientes SET nombre=?, apellido=?, fecha_nacimiento=?, genero=?, telefono=?, email=?, direccion=?, "
                + "motivo_consulta=?, psicologo_id=?, antecedentes_personales=?, antecedentes_familiares=?, anamnesis=? WHERE id=?"
            : "INSERT INTO pacientes (nombre, apellido, fecha_nacimiento, genero, telefono, email, direccion, motivo_consulta, "
                + "psicologo_id, antecedentes_personales, antecedentes_familiares, anamnesis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setString(1, nombre);
                ps.setString(2, apellido);

                if (fechaNacimiento != null) {
                    ps.setDate(3, fechaNacimiento);
                } else {
                    ps.setNull(3, Types.DATE);
                }

                setTextoONulo(ps, 4, genero);
                setTextoONulo(ps, 5, telefono);
                setTextoONulo(ps, 6, email);
                setTextoONulo(ps, 7, direccion);
                setTextoONulo(ps, 8, motivoConsulta);

                if (psicologoId != null) {
                    ps.setInt(9, psicologoId);
                } else {
                    ps.setNull(9, Types.INTEGER);
                }

                setTextoONulo(ps, 10, antecedentesPersonales);
                setTextoONulo(ps, 11, antecedentesFamiliares);
                setTextoONulo(ps, 12, anamnesis);

                if (esEdicion) {
                    ps.setInt(13, pacienteIdActual);
                }

                ps.executeUpdate();
            }

            if (esEdicion) {
                Auditoria.registrar(cn, "EDITAR_PACIENTE", "pacientes", pacienteIdActual, null);
            } else {
                Integer nuevoId = obtenerUltimoId(cn);
                Auditoria.registrar(cn, "CREAR_PACIENTE", "pacientes", nuevoId, null);
            }

            JOptionPane.showMessageDialog(this, esEdicion ? "Paciente actualizado correctamente" : "Paciente registrado correctamente");
            limpiarCampos();
            cargarPacientes();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private Integer obtenerUltimoId(Connection cn) throws java.sql.SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT LAST_INSERT_ID() AS id");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("id") : null;
        }
    }

    private void setTextoONulo(PreparedStatement ps, int indice, String valor) throws java.sql.SQLException {
        if (valor == null || valor.isEmpty()) {
            ps.setNull(indice, Types.VARCHAR);
        } else {
            ps.setString(indice, valor);
        }
    }

    private void cargarPacientes() {

        modeloTabla.setRowCount(0);

        String sql =
            "SELECT p.id, p.nombre, p.apellido, u.nombre AS psicologo_nombre, p.fecha_nacimiento "
            + "FROM pacientes p LEFT JOIN usuarios u ON u.id = p.psicologo_id ";

        if (Sesion.esPsicologo()) {
            sql += "WHERE p.psicologo_id = ? ";
        }

        sql += "ORDER BY p.id DESC";

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
                            rs.getString("nombre"),
                            rs.getString("apellido"),
                            rs.getString("psicologo_nombre"),
                            rs.getDate("fecha_nacimiento")
                        });
                    }
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar pacientes: " + e.getMessage());
        }
    }

    private void onSeleccionarPaciente(ListSelectionEvent evt) {

        if (evt.getValueIsAdjusting()) {
            return;
        }

        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        cargarPaciente(id);
    }

    private void cargarPaciente(int id) {

        String sql = "SELECT * FROM pacientes WHERE id=?";

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        return;
                    }

                    pacienteIdActual = id;
                    txtNombre.setText(rs.getString("nombre"));
                    txtApellido.setText(rs.getString("apellido"));
                    txtFechaNacimiento.setText(
                        rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toString() : ""
                    );
                    cmbGenero.setSelectedItem(rs.getString("genero") != null ? rs.getString("genero") : "");
                    txtTelefono.setText(valorOVacio(rs.getString("telefono")));
                    txtEmail.setText(valorOVacio(rs.getString("email")));
                    txtDireccion.setText(valorOVacio(rs.getString("direccion")));
                    txtMotivoConsulta.setText(valorOVacio(rs.getString("motivo_consulta")));
                    txtAntecedentesPersonales.setText(valorOVacio(rs.getString("antecedentes_personales")));
                    txtAntecedentesFamiliares.setText(valorOVacio(rs.getString("antecedentes_familiares")));
                    txtAnamnesis.setText(valorOVacio(rs.getString("anamnesis")));

                    if (!Sesion.esPsicologo()) {
                        Integer psicologoId = (Integer) rs.getObject("psicologo_id");
                        seleccionarPsicologo(psicologoId);
                    }
                }
            }

            Auditoria.registrar(cn, "VER_PACIENTE", "pacientes", id, null);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar el paciente: " + e.getMessage());
        }
    }

    private void seleccionarPsicologo(Integer psicologoId) {
        for (int i = 0; i < cmbPsicologo.getItemCount(); i++) {
            PsicologoItem item = cmbPsicologo.getItemAt(i);
            if ((psicologoId == null && item.id == null) || (psicologoId != null && psicologoId.equals(item.id))) {
                cmbPsicologo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String valorOVacio(String valor) {
        return valor != null ? valor : "";
    }

    private void limpiarCampos() {
        pacienteIdActual = null;
        tablaPacientes.clearSelection();
        txtNombre.setText("");
        txtApellido.setText("");
        txtFechaNacimiento.setText("");
        cmbGenero.setSelectedIndex(0);
        txtTelefono.setText("");
        txtEmail.setText("");
        txtDireccion.setText("");
        txtMotivoConsulta.setText("");
        txtAntecedentesPersonales.setText("");
        txtAntecedentesFamiliares.setText("");
        txtAnamnesis.setText("");
        if (!Sesion.esPsicologo()) {
            cmbPsicologo.setSelectedIndex(0);
        }
        txtNombre.requestFocus();
    }

    private void volverAlMenu() {
        MenuPrincipal menu = new MenuPrincipal();
        menu.setVisible(true);
        this.dispose();
    }

    private static class PsicologoItem {
        final Integer id;
        final String nombre;

        PsicologoItem(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}
