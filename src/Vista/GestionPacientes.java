package Vista;

import dao.*;
import modelos.*;
import util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    private JComboBox<String> comboFiltroCurso;
    private SelectorCursoSeccion selectorCurso;
    private TitledBorder bordePanelRegistro;
    private Integer pacienteIdActual;

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
        add(Tema.panelEncabezado("Gestión de Estudiantes", "Registrar, editar y consultar estudiantes"),
            BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de búsqueda — dos filas explícitas (no una sola FlowLayout) porque el combo de
        // curso es demasiado ancho para entrar junto al resto: si se envuelve solo dentro de un
        // único FlowLayout, Swing calcula mal el alto preferido del panel (FlowLayout no lo
        // recalcula al ajustar líneas) y la fila que se envuelve termina superpuesta con la
        // sección de abajo en vez de empujarla.
        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setBackground(Tema.SUPERFICIE);
        panelBusqueda.setBorder(BorderFactory.createCompoundBorder(
            Tema.tituloSeccion("Buscar Estudiante"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panelBusqueda.setLayout(new BoxLayout(panelBusqueda, BoxLayout.Y_AXIS));

        JPanel filaNombre = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filaNombre.setBackground(Tema.SUPERFICIE);
        filaNombre.add(new JLabel("Buscar por nombre:"));
        txtBusqueda = new JTextField(20);
        txtBusqueda.setBorder(Tema.bordeCampo());
        filaNombre.add(txtBusqueda);

        JButton btnBuscar = Tema.botonPrimario("Buscar", Icono.BUSCAR);
        btnBuscar.addActionListener(e -> buscarPacientes());
        filaNombre.add(btnBuscar);

        JButton btnLimpiar = Tema.botonSecundario("Limpiar", Icono.LIMPIAR);
        btnLimpiar.addActionListener(e -> {
            txtBusqueda.setText("");
            comboFiltroCurso.setSelectedIndex(0);
            cargarPacientes();
        });
        filaNombre.add(btnLimpiar);
        filaNombre.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panelBusqueda.add(filaNombre);

        JPanel filaCurso = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filaCurso.setBackground(Tema.SUPERFICIE);
        filaCurso.add(new JLabel("Curso:"));

        java.util.List<String> etiquetasCurso = new java.util.ArrayList<>();
        etiquetasCurso.add("Todos");
        etiquetasCurso.addAll(util.EstructuraAcademica.todasLasEtiquetas());
        comboFiltroCurso = new JComboBox<>(etiquetasCurso.toArray(new String[0]));
        comboFiltroCurso.setPreferredSize(new Dimension(260, comboFiltroCurso.getPreferredSize().height));
        comboFiltroCurso.setToolTipText("Filtrar por curso");
        comboFiltroCurso.addActionListener(e -> {
            comboFiltroCurso.setToolTipText(String.valueOf(comboFiltroCurso.getSelectedItem()));
            if (txtBusqueda.getText().trim().isEmpty()) {
                cargarPacientes();
            } else {
                buscarPacientes();
            }
        });
        filaCurso.add(comboFiltroCurso);

        JLabel lblHint = new JLabel("Doble clic en un estudiante para ver detalles completos");
        lblHint.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblHint.setForeground(Tema.TEXTO_SECUNDARIO);
        filaCurso.add(lblHint);
        filaCurso.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panelBusqueda.add(filaCurso);

        mainPanel.add(panelBusqueda, BorderLayout.NORTH);

        // Tabla de estudiantes
        String[] columnas = {"ID", "Nombre", "Apellido", "Email", "Teléfono", "Género", "Curso"};
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
        scrollPane.setBorder(Tema.tituloSeccion("Estudiantes Registrados (doble clic para ver detalles)"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel de registro
        JPanel panelRegistro = new JPanel();
        panelRegistro.setBackground(Tema.SUPERFICIE);
        bordePanelRegistro = Tema.tituloSeccion("Nuevo Estudiante");
        panelRegistro.setBorder(BorderFactory.createCompoundBorder(
            bordePanelRegistro,
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

        JButton btnCancelar = Tema.botonSecundario("Cancelar");
        btnCancelar.addActionListener(e -> limpiarFormulario());
        panelRegistro.add(btnCancelar);

        // Panel de curso/sección (estructura académica oficial, ver util.EstructuraAcademica)
        JPanel panelCurso = new JPanel(new BorderLayout());
        panelCurso.setBackground(Tema.SUPERFICIE);
        panelCurso.setBorder(BorderFactory.createCompoundBorder(
            Tema.tituloSeccion("Curso y sección"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        selectorCurso = new SelectorCursoSeccion();
        panelCurso.add(selectorCurso, BorderLayout.WEST);

        JPanel panelFormulario = new JPanel(new BorderLayout(0, 10));
        panelFormulario.setBackground(Tema.SUPERFICIE);
        panelFormulario.add(panelRegistro, BorderLayout.NORTH);
        panelFormulario.add(panelCurso, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Tema.SUPERFICIE);
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        JButton btnEditar = Tema.botonPrimario("Editar", Icono.EDITAR);
        btnEditar.addActionListener(e -> editarPacienteSeleccionado());
        footerPanel.add(btnEditar);

        JButton btnEliminar = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminar.addActionListener(e -> eliminarPaciente());
        footerPanel.add(btnEliminar);

        JButton btnVolver = Tema.botonSecundario("Volver", Icono.VOLVER);
        btnVolver.addActionListener(e -> alVolver.run());
        footerPanel.add(btnVolver);
        Tema.atajoEscape(this, btnVolver, alVolver);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelFormulario, BorderLayout.CENTER);
        panelSur.add(footerPanel, BorderLayout.SOUTH);
        mainPanel.add(panelSur, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void cargarPacientes() {
        Tema.conCursorEspera(this, () -> {
            try {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())
                    : pacienteDAO.obtenerTodos();
                poblarTabla(pacientes);
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

        Tema.conCursorEspera(this, () -> {
            try {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.buscarPorPsicologo(termino, util.Sesion.getUsuarioId())
                    : pacienteDAO.buscar(termino);
                poblarTabla(pacientes);
            } catch (Exception e) {
                System.out.println("Error buscando pacientes: " + e.getMessage());
            }
        });
    }

    /** Puebla la tabla con la lista dada, aplicando el filtro de curso seleccionado (si hay uno). */
    private void poblarTabla(List<Paciente> pacientes) {
        modeloTabla.setRowCount(0);
        String filtroCurso = comboFiltroCurso != null ? (String) comboFiltroCurso.getSelectedItem() : "Todos";
        for (Paciente p : pacientes) {
            if (filtroCurso != null && !"Todos".equals(filtroCurso) && !filtroCurso.equals(p.getCurso())) {
                continue;
            }
            modeloTabla.addRow(new Object[]{
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getEmail() != null ? p.getEmail() : "N/A",
                p.getTelefono() != null ? p.getTelefono() : "N/A",
                p.getGenero() != null ? p.getGenero() : "N/A",
                p.getCurso() != null ? p.getCurso() : "N/A"
            });
        }
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
                if (pacienteIdActual != null) {
                    Paciente paciente = pacienteDAO.obtenerPorId(pacienteIdActual);
                    if (paciente == null) {
                        JOptionPane.showMessageDialog(this, "El estudiante ya no existe", "Error", JOptionPane.ERROR_MESSAGE);
                        limpiarFormulario();
                        cargarPacientes();
                        return;
                    }
                    paciente.setNombre(nombre);
                    paciente.setApellido(apellido);
                    paciente.setEmail(email);
                    paciente.setTelefono(telefono);
                    paciente.setGenero((String) comboGenero.getSelectedItem());
                    paciente.setCurso(selectorCurso.getCursoSeleccionado());

                    if (pacienteDAO.actualizar(paciente)) {
                        try (Connection cn = new conexion.Conexion().conectar()) {
                            Auditoria.registrar(cn, "EDITAR_PACIENTE", "pacientes", pacienteIdActual, null);
                        } catch (Exception ex) {
                            System.out.println("Error registrando auditoría: " + ex.getMessage());
                        }
                        JOptionPane.showMessageDialog(this, "Estudiante actualizado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        cargarPacientes();
                        limpiarFormulario();
                    } else {
                        JOptionPane.showMessageDialog(this, "Error al actualizar el estudiante", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    Paciente paciente = new Paciente(nombre, apellido, email, telefono);
                    paciente.setGenero((String) comboGenero.getSelectedItem());
                    paciente.setCurso(selectorCurso.getCursoSeleccionado());
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
                        JOptionPane.showMessageDialog(this, "Estudiante guardado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        cargarPacientes();
                        limpiarFormulario();
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "No se pudo guardar el estudiante. Revisá la consola de la aplicación para el detalle del "
                                + "error (por ejemplo, si falta aplicar la migración db-init/06_estructura_academica.sql "
                                + "en la base de datos).",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void editarPacienteSeleccionado() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        cargarPacienteParaEditar(id);
    }

    private void cargarPacienteParaEditar(int id) {
        Paciente paciente = pacienteDAO.obtenerPorId(id);
        if (paciente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el estudiante", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        pacienteIdActual = id;
        txtNombre.setText(paciente.getNombre());
        txtApellido.setText(paciente.getApellido());
        txtEmail.setText(paciente.getEmail());
        txtTelefono.setText(paciente.getTelefono());
        String genero = paciente.getGenero();
        comboGenero.setSelectedItem(
            ("Masculino".equals(genero) || "Femenino".equals(genero) || "Otro".equals(genero)) ? genero : "Otro");
        selectorCurso.setCursoSeleccionado(paciente.getCurso());

        bordePanelRegistro.setTitle("Editar Estudiante: " + paciente.getNombre() + " " + paciente.getApellido());
        repaint();
    }

    private void eliminarPaciente() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea eliminar este estudiante?", "Confirmación", JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            String error = pacienteDAO.eliminar(id);
            if (error == null) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "ELIMINAR_PACIENTE", "pacientes", id, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                JOptionPane.showMessageDialog(this, "Estudiante eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                if (id == (pacienteIdActual != null ? pacienteIdActual : -1)) {
                    limpiarFormulario();
                }
                cargarPacientes();
            } else {
                JOptionPane.showMessageDialog(this, error, "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void verDetallesPaciente() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        try (Connection cn = new conexion.Conexion().conectar()) {
            Auditoria.registrar(cn, "VER_PACIENTE", "pacientes", id, null);
        } catch (Exception ex) {
            System.out.println("Error registrando auditoría: " + ex.getMessage());
        }

        DetallePaciente ventanaDetalle = new DetallePaciente(id, () -> cargarPacienteParaEditar(id));
        ventanaDetalle.setVisible(true);
    }

    private void limpiarFormulario() {
        pacienteIdActual = null;
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        txtTelefono.setText("");
        comboGenero.setSelectedIndex(0);
        selectorCurso.setCursoSeleccionado(null);
        bordePanelRegistro.setTitle("Nuevo Estudiante");
        Tema.marcarError(txtNombre, true);
        Tema.marcarError(txtApellido, true);
        repaint();
    }

    /** Solo para probar esta pantalla de forma aislada, fuera del shell de MenuPrincipal. */
    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Gestión de Estudiantes (prueba aislada)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setIconImage(Tema.iconoApp());
            f.getContentPane().add(new GestionPacientes(() -> System.exit(0)));
            f.setSize(1400, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
