package Vista;

import dao.*;
import modelos.*;
import util.Auditoria;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GestionTurnos extends javax.swing.JFrame {

    private TurnoDAO turnoDAO;
    private PacienteDAO pacienteDAO;
    private UsuarioDAO usuarioDAO;
    private JTable tablaTurnos;
    private DefaultTableModel modeloTabla;
    private JComboBox<Paciente> comboPaciente;
    private JComboBox<Usuario> comboPsicologo;
    private JSpinner spinnerFechaHora;

    public GestionTurnos() {
        this.turnoDAO = new TurnoDAO();
        this.pacienteDAO = new PacienteDAO();
        this.usuarioDAO = new UsuarioDAO();

        initComponents();
        cargarDatos();
        setIconImage(Tema.iconoApp());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initComponents() {
        setSize(1200, 700);
        setTitle("Gestión de Turnos - Sistema de Psicología");

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JLabel lblTitulo = new JLabel("Gestión de Turnos");
        lblTitulo.setFont(Tema.SUBTITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        mainPanel.add(lblTitulo, BorderLayout.NORTH);

        // Panel de registro
        JPanel panelRegistro = new JPanel();
        panelRegistro.setBackground(Tema.SUPERFICIE);
        panelRegistro.setLayout(new GridLayout(2, 3, 10, 10));
        panelRegistro.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Nuevo Turno"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Paciente
        panelRegistro.add(new JLabel("Paciente:"));
        comboPaciente = new JComboBox<>();
        panelRegistro.add(comboPaciente);

        // Psicólogo
        panelRegistro.add(new JLabel("Psicólogo:"));
        comboPsicologo = new JComboBox<>();
        panelRegistro.add(comboPsicologo);

        // Fecha y Hora
        panelRegistro.add(new JLabel("Fecha y Hora:"));
        spinnerFechaHora = new JSpinner(new javax.swing.SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinnerFechaHora, "dd/MM/yyyy HH:mm");
        spinnerFechaHora.setEditor(editor);
        panelRegistro.add(spinnerFechaHora);

        JButton btnGuardar = Tema.botonExito("Guardar Turno", Icono.GUARDAR);
        btnGuardar.addActionListener(e -> guardarTurno());
        panelRegistro.add(btnGuardar);

        mainPanel.add(panelRegistro, BorderLayout.NORTH);

        // Tabla de turnos
        String[] columnas = {"ID", "Paciente", "Psicólogo", "Fecha/Hora", "Duración", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaTurnos = new JTable(modeloTabla);
        Tema.estilizarTabla(tablaTurnos);
        tablaTurnos.getColumnModel().getColumn(5).setCellRenderer(Tema.rendererEstado());

        JScrollPane scrollPane = new JScrollPane(tablaTurnos);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Turnos Registrados"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Tema.SUPERFICIE);
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        JButton btnEliminar = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminar.addActionListener(e -> eliminarTurno());
        footerPanel.add(btnEliminar);

        JButton btnVolver = Tema.botonSecundario("Volver", Icono.VOLVER);
        btnVolver.addActionListener(e -> {
            this.dispose();
            MenuPrincipal menu = new MenuPrincipal();
            menu.setVisible(true);
        });
        footerPanel.add(btnVolver);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
    }

    private void cargarDatos() {
        try {
            boolean esPsicologo = util.Sesion.esPsicologo();

            // Cargar pacientes (un psicólogo solo ve los suyos)
            List<Paciente> pacientes = esPsicologo
                ? pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())
                : pacienteDAO.obtenerTodos();
            for (Paciente p : pacientes) {
                comboPaciente.addItem(p);
            }

            // Cargar psicólogos: si el usuario logueado es psicólogo, el combo queda
            // fijo en sí mismo (no puede asignar turnos a otro psicólogo)
            if (esPsicologo) {
                Usuario yo = usuarioDAO.obtenerPorId(util.Sesion.getUsuarioId());
                if (yo != null) {
                    comboPsicologo.addItem(yo);
                }
                comboPsicologo.setEnabled(false);
            } else {
                List<Usuario> psicologos = usuarioDAO.obtenerPorRol("psicologo");
                for (Usuario u : psicologos) {
                    comboPsicologo.addItem(u);
                }
            }

            // Cargar turnos en tabla
            actualizarTabla();

        } catch (Exception e) {
            System.out.println("Error cargando datos: " + e.getMessage());
        }
    }

    private void actualizarTabla() {
        modeloTabla.setRowCount(0);
        try {
            List<Turno> turnos = util.Sesion.esPsicologo()
                ? turnoDAO.obtenerPendientesPorPsicologo(util.Sesion.getUsuarioId())
                : turnoDAO.obtenerTodosPendientes();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Turno t : turnos) {
                Paciente p = pacienteDAO.obtenerPorId(t.getPacienteId());
                Usuario u = usuarioDAO.obtenerPorId(t.getPsicologoId());

                modeloTabla.addRow(new Object[]{
                    t.getId(),
                    p != null ? p.getNombre() : "N/A",
                    u != null ? u.getNombre() : "N/A",
                    t.getFechaHora().format(formatter),
                    t.getDuracionMinutos() + " min",
                    t.getEstado()
                });
            }
        } catch (Exception e) {
            System.out.println("Error actualizando tabla: " + e.getMessage());
        }
    }

    private void guardarTurno() {
        try {
            Paciente p = (Paciente) comboPaciente.getSelectedItem();
            Usuario u = (Usuario) comboPsicologo.getSelectedItem();
            java.util.Date fechaDate = (java.util.Date) spinnerFechaHora.getValue();
            LocalDateTime fechaHora = new java.sql.Timestamp(fechaDate.getTime()).toLocalDateTime();

            if (p == null || u == null) {
                JOptionPane.showMessageDialog(this, "Complete todos los campos", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Turno turno = new Turno(p.getId(), u.getId(), fechaHora);
            int nuevoId = turnoDAO.crear(turno);
            if (nuevoId > 0) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "CREAR_TURNO", "turnos", nuevoId, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                JOptionPane.showMessageDialog(this, "Turno guardado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                actualizarTabla();
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar turno", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarTurno() {
        int fila = tablaTurnos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un turno", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        if (turnoDAO.eliminar(id)) {
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, "ELIMINAR_TURNO", "turnos", id, null);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Turno eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            actualizarTabla();
        }
    }

    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> new GestionTurnos().setVisible(true));
    }
}
