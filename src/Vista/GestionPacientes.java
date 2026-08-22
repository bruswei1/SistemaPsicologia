package Vista;

import dao.*;
import modelos.*;
import util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class GestionPacientes extends javax.swing.JFrame {

    private PacienteDAO pacienteDAO;
    private JTable tablaPacientes;
    private DefaultTableModel modeloTabla;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtEmail;
    private JTextField txtTelefono;
    private JTextField txtBusqueda;
    private JSpinner spinnerFecha;

    public GestionPacientes() {
        this.pacienteDAO = new PacienteDAO();
        initComponents();
        cargarPacientes();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initComponents() {
        setSize(1400, 800);
        setTitle("Gestión de Pacientes - Sistema de Psicología");

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JLabel lblTitulo = new JLabel("👥 Gestión de Pacientes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(new Color(33, 33, 33));
        mainPanel.add(lblTitulo, BorderLayout.NORTH);

        // Panel de búsqueda
        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setBackground(new Color(255, 255, 255));
        panelBusqueda.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Buscar Paciente"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panelBusqueda.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        panelBusqueda.add(new JLabel("Buscar por nombre:"));
        txtBusqueda = new JTextField(20);
        panelBusqueda.add(txtBusqueda);

        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.setBackground(new Color(79, 129, 245));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFocusPainted(false);
        btnBuscar.addActionListener(e -> buscarPacientes());
        panelBusqueda.add(btnBuscar);

        JButton btnLimpiar = new JButton("🔄 Limpiar");
        btnLimpiar.setBackground(new Color(200, 200, 200));
        btnLimpiar.setForeground(Color.BLACK);
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.addActionListener(e -> {
            txtBusqueda.setText("");
            cargarPacientes();
        });
        panelBusqueda.add(btnLimpiar);
        
        JLabel lblHint = new JLabel("Doble clic en un paciente para ver detalles completos");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblHint.setForeground(new Color(100, 100, 100));
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
        tablaPacientes.setBackground(new Color(255, 255, 255));
        tablaPacientes.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tablaPacientes.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaPacientes.setRowHeight(25);
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
        panelRegistro.setBackground(new Color(255, 255, 255));
        panelRegistro.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Nuevo Paciente"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panelRegistro.setLayout(new GridLayout(2, 4, 10, 10));

        panelRegistro.add(new JLabel("Nombre:"));
        txtNombre = new JTextField();
        panelRegistro.add(txtNombre);

        panelRegistro.add(new JLabel("Apellido:"));
        txtApellido = new JTextField();
        panelRegistro.add(txtApellido);

        panelRegistro.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        panelRegistro.add(txtEmail);

        panelRegistro.add(new JLabel("Teléfono:"));
        txtTelefono = new JTextField();
        panelRegistro.add(txtTelefono);

        panelRegistro.add(new JLabel("Fecha Nacimiento:"));
        spinnerFecha = new JSpinner(new javax.swing.SpinnerDateModel());
        panelRegistro.add(spinnerFecha);

        JButton btnGuardar = new JButton("➕ Guardar");
        btnGuardar.setBackground(new Color(76, 175, 80));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGuardar.setFocusPainted(false);
        btnGuardar.addActionListener(e -> guardarPaciente());
        panelRegistro.add(btnGuardar);

        mainPanel.add(panelRegistro, BorderLayout.SOUTH);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(255, 255, 255));
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        JButton btnEliminar = new JButton("🗑️ Eliminar");
        btnEliminar.setBackground(new Color(244, 67, 54));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFocusPainted(false);
        btnEliminar.addActionListener(e -> eliminarPaciente());
        footerPanel.add(btnEliminar);

        JButton btnVolver = new JButton("← Volver");
        btnVolver.setBackground(new Color(200, 200, 200));
        btnVolver.setForeground(Color.BLACK);
        btnVolver.setFocusPainted(false);
        btnVolver.addActionListener(e -> {
            this.dispose();
            MenuPrincipal menu = new MenuPrincipal();
            menu.setVisible(true);
        });
        footerPanel.add(btnVolver);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
    }

    private void cargarPacientes() {
        modeloTabla.setRowCount(0);
        try {
            List<Paciente> pacientes = pacienteDAO.obtenerTodos();
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
    }

    private void buscarPacientes() {
        String termino = txtBusqueda.getText().trim();
        if (termino.isEmpty()) {
            cargarPacientes();
            return;
        }

        modeloTabla.setRowCount(0);
        try {
            List<Paciente> pacientes = pacienteDAO.buscar(termino);
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
    }

    private void guardarPaciente() {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String email = txtEmail.getText().trim();
        String telefono = txtTelefono.getText().trim();

        if (nombre.isEmpty() || apellido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete nombre y apellido", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Paciente paciente = new Paciente(nombre, apellido, email, telefono);
            
            if (pacienteDAO.crear(paciente)) {
                JOptionPane.showMessageDialog(this, "Paciente guardado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarPacientes();
                limpiarFormulario();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
        DetallePaciente ventanaDetalle = new DetallePaciente(id);
        ventanaDetalle.setVisible(true);
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        txtTelefono.setText("");
    }

    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            System.out.println("FlatLaf no disponible");
        }

        SwingUtilities.invokeLater(() -> new GestionPacientes().setVisible(true));
    }
}
