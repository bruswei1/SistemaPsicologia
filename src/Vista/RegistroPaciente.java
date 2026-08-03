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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class RegistroPaciente extends JFrame {

    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtFechaNacimiento;
    private JComboBox<String> cmbGenero;
    private JTextField txtTelefono;
    private JTextField txtEmail;
    private JTextField txtDireccion;
    private JTextArea txtMotivoConsulta;
    private JTable tablaPacientes;
    private DefaultTableModel modeloTabla;

    public RegistroPaciente() {
        setTitle("Registro de Pacientes");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        cargarPacientes();
        setSize(700, 550);
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JPanel panelFormulario = new JPanel(new GridBagLayout());
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

        int fila = 0;
        agregarCampo(panelFormulario, gbc, fila++, "Nombre*", txtNombre);
        agregarCampo(panelFormulario, gbc, fila++, "Apellido*", txtApellido);
        agregarCampo(panelFormulario, gbc, fila++, "Fecha de nacimiento (yyyy-mm-dd)", txtFechaNacimiento);
        agregarCampo(panelFormulario, gbc, fila++, "Género", cmbGenero);
        agregarCampo(panelFormulario, gbc, fila++, "Teléfono", txtTelefono);
        agregarCampo(panelFormulario, gbc, fila++, "Email", txtEmail);
        agregarCampo(panelFormulario, gbc, fila++, "Dirección", txtDireccion);
        agregarCampo(panelFormulario, gbc, fila++, "Motivo de consulta", new JScrollPane(txtMotivoConsulta));

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(evt -> guardarPaciente());

        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(evt -> limpiarCampos());

        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(evt -> volverAlMenu());

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnGuardar);
        panelBotones.add(btnLimpiar);
        panelBotones.add(btnVolver);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Apellido", "Fecha Nac.", "Teléfono", "Email"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaPacientes = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaPacientes);
        scrollTabla.setBorder(javax.swing.BorderFactory.createTitledBorder("Pacientes registrados"));

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(panelFormulario, BorderLayout.CENTER);
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

        String sql =
            "INSERT INTO pacientes (nombre, apellido, fecha_nacimiento, genero, telefono, email, direccion, motivo_consulta) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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

                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Paciente registrado correctamente");
            limpiarCampos();
            cargarPacientes();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
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

        String sql = "SELECT id, nombre, apellido, fecha_nacimiento, telefono, email FROM pacientes ORDER BY id DESC";

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    modeloTabla.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getDate("fecha_nacimiento"),
                        rs.getString("telefono"),
                        rs.getString("email")
                    });
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar pacientes: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtFechaNacimiento.setText("");
        cmbGenero.setSelectedIndex(0);
        txtTelefono.setText("");
        txtEmail.setText("");
        txtDireccion.setText("");
        txtMotivoConsulta.setText("");
        txtNombre.requestFocus();
    }

    private void volverAlMenu() {
        MenuPrincipal menu = new MenuPrincipal();
        menu.setVisible(true);
        this.dispose();
    }
}
