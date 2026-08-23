package Vista;

import dao.*;
import modelos.*;
import util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class GestionPacientes extends javax.swing.JPanel {

    private final Runnable alVolver;
    private PacienteDAO pacienteDAO;
    private JTable tablaPacientes;
    private DefaultTableModel modeloTabla;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtEmail;
    private JTextField txtTelefono;
    private JTextField txtBusqueda;
    private JComboBox<String> comboGenero;

    public GestionPacientes(Runnable alVolver) {
        this.alVolver = alVolver;
        this.pacienteDAO = new PacienteDAO();
        initComponents();
        cargarPacientes();
    }

    /** Recarga los datos; MenuPrincipal la llama cada vez que se navega a esta pantalla. */
    public void refrescar() {
        cargarPacientes();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JLabel lblTitulo = new JLabel("Gestión de Pacientes");
        lblTitulo.setFont(Tema.SUBTITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        mainPanel.add(lblTitulo, BorderLayout.NORTH);

        // Panel de búsqueda
        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setBackground(Tema.SUPERFICIE);
        panelBusqueda.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Buscar Paciente"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panelBusqueda.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        panelBusqueda.add(new JLabel("Buscar por nombre:"));
        txtBusqueda = new JTextField(20);
        txtBusqueda.setBorder(Tema.bordeCampo());
        panelBusqueda.add(txtBusqueda);

        JButton btnBuscar = Tema.botonPrimario("Buscar", Icono.BUSCAR);
        btnBuscar.addActionListener(e -> buscarPacientes());
        panelBusqueda.add(btnBuscar);

        JButton btnLimpiar = Tema.botonSecundario("Limpiar", Icono.LIMPIAR);
        btnLimpiar.addActionListener(e -> {
            txtBusqueda.setText("");
            cargarPacientes();
        });
        panelBusqueda.add(btnLimpiar);

        JLabel lblHint = new JLabel("Doble clic en un paciente para ver detalles completos");
        lblHint.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblHint.setForeground(Tema.TEXTO_SECUNDARIO);
        panelBusqueda.add(lblHint);

        mainPanel.add(panelBusqueda, BorderLayout.NORTH);

        // Tabla de pacientes
        String[] columnas = {"ID", "Nombre", "Apellido", "Email", "Teléfono", "Género"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaPacientes = new JTable(modeloTabla);
        Tema.estilizarTabla(tablaPacientes);
        tablaPacientes.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    verDetallesPaciente();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tablaPacientes);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Pacientes Registrados (doble clic para ver detalles)"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel de registro
        JPanel panelRegistro = new JPanel();
        panelRegistro.setBackground(Tema.SUPERFICIE);
        panelRegistro.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Nuevo Paciente"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panelRegistro.setLayout(new GridLayout(2, 4, 10, 10));

        panelRegistro.add(new JLabel("Nombre:"));
        txtNombre = new JTextField();
        txtNombre.setBorder(Tema.bordeCampo());
        panelRegistro.add(txtNombre);

        panelRegistro.add(new JLabel("Apellido:"));
        txtApellido = new JTextField();
        txtApellido.setBorder(Tema.bordeCampo());
        panelRegistro.add(txtApellido);

        panelRegistro.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        txtEmail.setBorder(Tema.bordeCampo());
        panelRegistro.add(txtEmail);

        panelRegistro.add(new JLabel("Teléfono:"));
        txtTelefono = new JTextField();
        txtTelefono.setBorder(Tema.bordeCampo());
        panelRegistro.add(txtTelefono);

        panelRegistro.add(new JLabel("Género:"));
        comboGenero = new JComboBox<>(new String[]{"Masculino", "Femenino", "Otro"});
        panelRegistro.add(comboGenero);

        JButton btnGuardar = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardar.addActionListener(e -> guardarPaciente());
        panelRegistro.add(btnGuardar);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Tema.SUPERFICIE);
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        JButton btnEliminar = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminar.addActionListener(e -> eliminarPaciente());
        footerPanel.add(btnEliminar);

        JButton btnVolver = Tema.botonSecundario("Volver", Icono.VOLVER);
        btnVolver.addActionListener(e -> alVolver.run());
        footerPanel.add(btnVolver);
        Tema.atajoEscape(this, btnVolver, alVolver);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelRegistro, BorderLayout.CENTER);
        panelSur.add(footerPanel, BorderLayout.SOUTH);
        mainPanel.add(panelSur, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void cargarPacientes() {
        modeloTabla.setRowCount(0);
        Tema.conCursorEspera(this, () -> {
            try {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())
                    : pacienteDAO.obtenerTodos();
                for (Paciente p : pacientes) {
                    modeloTabla.addRow(new Object[]{
                        p.getId(),
                        p.getNombre(),
                        p.getApellido(),
                        p.getEmail() != null ? p.getEmail() : "N/A",
                        p.getTelefono() != null ? p.getTelefono() : "N/A",
                        p.getGenero() != null ? p.getGenero() : "N/A"
                    });
                }
            } catch (Exception e) {
                System.out.println("Error cargando pacientes: " + e.getMessage());
            }
        });
    }

    private void buscarPacientes() {
        String termino = txtBusqueda.getText().trim();
        if (termino.isEmpty()) {
            cargarPacientes();
            return;
        }

        modeloTabla.setRowCount(0);
        Tema.conCursorEspera(this, () -> {
            try {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.buscarPorPsicologo(termino, util.Sesion.getUsuarioId())
                    : pacienteDAO.buscar(termino);
                for (Paciente p : pacientes) {
                    modeloTabla.addRow(new Object[]{
                        p.getId(),
                        p.getNombre(),
                        p.getApellido(),
                        p.getEmail() != null ? p.getEmail() : "N/A",
                        p.getTelefono() != null ? p.getTelefono() : "N/A",
                        p.getGenero() != null ? p.getGenero() : "N/A"
                    });
                }
            } catch (Exception e) {
                System.out.println("Error buscando pacientes: " + e.getMessage());
            }
        });
    }

    private void guardarPaciente() {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String email = txtEmail.getText().trim();
        String telefono = txtTelefono.getText().trim();

        Tema.marcarError(txtNombre, !nombre.isEmpty());
        Tema.marcarError(txtApellido, !apellido.isEmpty());

        if (nombre.isEmpty() || apellido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete nombre y apellido", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Tema.conCursorEspera(this, () -> {
            try {
                Paciente paciente = new Paciente(nombre, apellido, email, telefono);
                paciente.setGenero((String) comboGenero.getSelectedItem());
                if (util.Sesion.esPsicologo()) {
                    paciente.setPsicologoId(util.Sesion.getUsuarioId());
                }

                int nuevoId = pacienteDAO.crear(paciente);
                if (nuevoId > 0) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "CREAR_PACIENTE", "pacientes", nuevoId, null);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                    JOptionPane.showMessageDialog(this, "Paciente guardado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cargarPacientes();
                    limpiarFormulario();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void eliminarPaciente() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea eliminar este paciente?", "Confirmación", JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            if (pacienteDAO.eliminar(id)) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "ELIMINAR_PACIENTE", "pacientes", id, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                JOptionPane.showMessageDialog(this, "Paciente eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarPacientes();
            }
        }
    }

    private void verDetallesPaciente() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        try (Connection cn = new conexion.Conexion().conectar()) {
            Auditoria.registrar(cn, "VER_PACIENTE", "pacientes", id, null);
        } catch (Exception ex) {
            System.out.println("Error registrando auditoría: " + ex.getMessage());
        }

        DetallePaciente ventanaDetalle = new DetallePaciente(id);
        ventanaDetalle.setVisible(true);
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        txtTelefono.setText("");
        comboGenero.setSelectedIndex(0);
        Tema.marcarError(txtNombre, true);
        Tema.marcarError(txtApellido, true);
    }

    /** Solo para probar esta pantalla de forma aislada, fuera del shell de MenuPrincipal. */
    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Gestión de Pacientes (prueba aislada)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setIconImage(Tema.iconoApp());
            f.getContentPane().add(new GestionPacientes(() -> System.exit(0)));
            f.setSize(1400, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
