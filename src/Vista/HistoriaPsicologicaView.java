package Vista;

import dao.*;
import modelos.*;
import util.Auditoria;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoriaPsicologicaView extends javax.swing.JPanel {

    private final Runnable alVolver;
    private HistoriaPsicologicaDAO historiaDAO;
    private PacienteDAO pacienteDAO;
    private AuditoriaDAO auditoriaDAO;
    private JComboBox<Paciente> comboPaciente;
    private JTextArea txtAntecedentes;
    private JTextArea txtMotivoConsulta;
    private JTextArea txtObservaciones;
    private JTextArea txtDiagnostico;
    private JTextArea txtTratamiento;
    private JLabel lblUltimaActualizacion;
    private Integer historiaIdActual;

    public HistoriaPsicologicaView(Runnable alVolver) {
        this.alVolver = alVolver;
        this.historiaDAO = new HistoriaPsicologicaDAO();
        this.pacienteDAO = new PacienteDAO();
        this.auditoriaDAO = new AuditoriaDAO();

        initComponents();
        cargarPacientes();
    }

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
        JLabel lblTitulo = new JLabel("Historia Psicológica");
        lblTitulo.setFont(Tema.SUBTITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Tema.SUPERFICIE);
        headerPanel.add(lblTitulo, BorderLayout.WEST);

        JLabel lblSelectPaciente = new JLabel("Seleccionar Paciente:");
        comboPaciente = new JComboBox<>();
        comboPaciente.addActionListener(e -> cargarHistoria());

        JPanel selectorPanel = new JPanel();
        selectorPanel.setBackground(Tema.SUPERFICIE);
        selectorPanel.add(lblSelectPaciente);
        selectorPanel.add(comboPaciente);
        headerPanel.add(selectorPanel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Panel de contenido con tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Tema.SUPERFICIE);
        tabbedPane.setFont(Tema.TEXTO_CHICO);

        // Tab 1: Información General
        JPanel panelGeneral = new JPanel(new GridLayout(3, 2, 10, 10));
        panelGeneral.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelGeneral.setBackground(Tema.FONDO);

        panelGeneral.add(new JLabel("Antecedentes:"));
        txtAntecedentes = new JTextArea(5, 30);
        txtAntecedentes.setLineWrap(true);
        txtAntecedentes.setWrapStyleWord(true);
        panelGeneral.add(new JScrollPane(txtAntecedentes));

        panelGeneral.add(new JLabel("Motivo de Consulta:"));
        txtMotivoConsulta = new JTextArea(5, 30);
        txtMotivoConsulta.setLineWrap(true);
        txtMotivoConsulta.setWrapStyleWord(true);
        panelGeneral.add(new JScrollPane(txtMotivoConsulta));

        panelGeneral.add(new JLabel("Observaciones Generales:"));
        txtObservaciones = new JTextArea(5, 30);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        panelGeneral.add(new JScrollPane(txtObservaciones));

        tabbedPane.addTab("Información General", panelGeneral);

        // Tab 2: Diagnóstico y Tratamiento
        JPanel panelDiagnostico = new JPanel(new GridLayout(2, 1, 10, 10));
        panelDiagnostico.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelDiagnostico.setBackground(Tema.FONDO);

        panelDiagnostico.add(new JLabel("Diagnóstico:"));
        txtDiagnostico = new JTextArea(6, 40);
        txtDiagnostico.setLineWrap(true);
        txtDiagnostico.setWrapStyleWord(true);
        panelDiagnostico.add(new JScrollPane(txtDiagnostico));

        panelDiagnostico.add(new JLabel("Plan de Tratamiento:"));
        txtTratamiento = new JTextArea(6, 40);
        txtTratamiento.setLineWrap(true);
        txtTratamiento.setWrapStyleWord(true);
        panelDiagnostico.add(new JScrollPane(txtTratamiento));

        tabbedPane.addTab("Diagnóstico y Tratamiento", panelDiagnostico);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Tema.SUPERFICIE);
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        lblUltimaActualizacion = new JLabel("Última actualización: N/A");
        lblUltimaActualizacion.setForeground(Tema.TEXTO_SECUNDARIO);
        footerPanel.add(lblUltimaActualizacion);

        JButton btnGuardar = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardar.addActionListener(e -> guardarHistoria());
        footerPanel.add(btnGuardar);

        JButton btnExportar = Tema.botonPrimario("Exportar Reporte", Icono.EXPORTAR);
        btnExportar.addActionListener(e -> exportarReporte());
        footerPanel.add(btnExportar);

        JButton btnHistorial = Tema.botonSecundario("Historial de cambios");
        btnHistorial.addActionListener(e -> verHistorialCambios());
        footerPanel.add(btnHistorial);

        JButton btnVolver = Tema.botonSecundario("Volver", Icono.VOLVER);
        btnVolver.addActionListener(e -> alVolver.run());
        footerPanel.add(btnVolver);
        Tema.atajoEscape(this, btnVolver, alVolver);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void cargarPacientes() {
        comboPaciente.removeAllItems();
        try {
            List<Paciente> pacientes = pacienteDAO.obtenerTodos();
            for (Paciente p : pacientes) {
                comboPaciente.addItem(p);
            }
        } catch (Exception e) {
            System.out.println("Error cargando pacientes: " + e.getMessage());
        }
    }

    private void cargarHistoria() {
        try {
            Paciente p = (Paciente) comboPaciente.getSelectedItem();
            if (p == null) return;

            HistoriaPsicologica historia = historiaDAO.obtenerPorPaciente(p.getId());

            if (historia != null) {
                historiaIdActual = historia.getId();
                txtAntecedentes.setText(historia.getAntecedentes() != null ? historia.getAntecedentes() : "");
                txtMotivoConsulta.setText(historia.getMotivoConsulta() != null ? historia.getMotivoConsulta() : "");
                txtObservaciones.setText(historia.getObservacionesGenerales() != null ? historia.getObservacionesGenerales() : "");
                txtDiagnostico.setText(historia.getDiagnostico() != null ? historia.getDiagnostico() : "");
                txtTratamiento.setText(historia.getTratamiento() != null ? historia.getTratamiento() : "");

                if (historia.getUltimaActualizacion() != null) {
                    lblUltimaActualizacion.setText("Última actualización: " + historia.getUltimaActualizacion());
                }

                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "VER_HISTORIA", "historia_psicologica", historiaIdActual, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
            } else {
                historiaIdActual = null;
                limpiarFormulario();
            }
        } catch (Exception e) {
            System.out.println("Error cargando historia: " + e.getMessage());
        }
    }

    private void guardarHistoria() {
        Paciente p = (Paciente) comboPaciente.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Tema.conCursorEspera(this, () -> {
            try {
                HistoriaPsicologica historia = historiaDAO.obtenerPorPaciente(p.getId());

                if (historia == null) {
                    historia = new HistoriaPsicologica();
                    historia.setPacienteId(p.getId());
                    historiaDAO.crear(historia);
                    historia = historiaDAO.obtenerPorPaciente(p.getId());
                }

                historia.setAntecedentes(txtAntecedentes.getText());
                historia.setMotivoConsulta(txtMotivoConsulta.getText());
                historia.setObservacionesGenerales(txtObservaciones.getText());
                historia.setDiagnostico(txtDiagnostico.getText());
                historia.setTratamiento(txtTratamiento.getText());

                if (historiaDAO.actualizar(historia)) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "EDITAR_HISTORIA", "historia_psicologica", historia.getId(), null);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                    JOptionPane.showMessageDialog(this, "Historia guardada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cargarHistoria();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al guardar", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void verHistorialCambios() {
        if (historiaIdActual == null) {
            JOptionPane.showMessageDialog(this, "Este paciente todavía no tiene historia clínica guardada", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<AuditoriaDAO.Registro> registros = auditoriaDAO.obtenerPorEntidad("historia_psicologica", historiaIdActual);

        String[] columnas = {"Fecha", "Usuario", "Acción"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (AuditoriaDAO.Registro r : registros) {
            modelo.addRow(new Object[]{r.fecha.format(formato), r.usuarioNombre, r.accion});
        }

        JTable tabla = new JTable(modelo);
        Tema.estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(450, 250));

        JOptionPane.showMessageDialog(this, scroll,
            "Historial de cambios (no guarda versiones de texto, solo quién y cuándo)",
            JOptionPane.PLAIN_MESSAGE);
    }

    private void limpiarFormulario() {
        txtAntecedentes.setText("");
        txtMotivoConsulta.setText("");
        txtObservaciones.setText("");
        txtDiagnostico.setText("");
        txtTratamiento.setText("");
        lblUltimaActualizacion.setText("Última actualización: N/A");
    }
    
    private void exportarReporte() {
        try {
            Paciente p = (Paciente) comboPaciente.getSelectedItem();
            if (p == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un paciente", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            HistoriaPsicologica historia = historiaDAO.obtenerPorPaciente(p.getId());
            if (historia == null) {
                JOptionPane.showMessageDialog(this, "No hay datos para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String contenido = util.GeneradorReportes.generarReportePaciente(p, historia);
            String nombreArchivo = util.GeneradorReportes.obtenerNombreArchivoReporte(p.getNombre());

            java.io.File carpetaReportes = new java.io.File("reportes");
            if (!carpetaReportes.exists()) {
                carpetaReportes.mkdirs();
            }
            String rutaArchivo = new java.io.File(carpetaReportes, nombreArchivo).getPath();

            if (util.GeneradorReportes.guardarReportePDF(contenido, rutaArchivo)) {
                JOptionPane.showMessageDialog(this, "Reporte guardado en: " + rutaArchivo, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar reporte", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Solo para probar esta pantalla de forma aislada, fuera del shell de MenuPrincipal. */
    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Historia Psicológica (prueba aislada)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setIconImage(Tema.iconoApp());
            f.getContentPane().add(new HistoriaPsicologicaView(() -> System.exit(0)));
            f.setSize(1000, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
