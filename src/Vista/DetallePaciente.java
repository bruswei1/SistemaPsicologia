package Vista;

import dao.*;
import modelos.Paciente;
import modelos.HistoriaPsicologica;
import modelos.Sesion;
import util.Validador;
import util.ControlPermiso;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import modelos.Usuario;

public class DetallePaciente extends javax.swing.JFrame {

    private PacienteDAO pacienteDAO;
    private HistoriaPsicologicaDAO historiaDAO;
    private SesionDAO sesionDAO;
    private Paciente pacienteActual;
    private HistoriaPsicologica historiaActual;

    public DetallePaciente(int pacienteId) {
        this.pacienteDAO = new PacienteDAO();
        this.historiaDAO = new HistoriaPsicologicaDAO();
        this.sesionDAO = new SesionDAO();
        
        this.pacienteActual = pacienteDAO.obtenerPorId(pacienteId);
        this.historiaActual = historiaDAO.obtenerPorPaciente(pacienteId);
        
        initComponents();
        cargarDatos();
        setIconImage(Tema.iconoApp());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setSize(1000, 900);
        setTitle("Detalle del Paciente - Sistema de Psicología");

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Tema.SUPERFICIE);
        tabbedPane.setFont(Tema.TEXTO_CHICO);

        // Tab 1: Información Personal
        tabbedPane.addTab("Información Personal", new IconoSwing(Icono.PACIENTES, Tema.PRIMARIO, 16), crearPanelPersonal());

        // Tab 2: Información Clínica
        tabbedPane.addTab("Información Clínica", new IconoSwing(Icono.HISTORIA, Tema.PRIMARIO, 16), crearPanelClinica());

        // Tab 3: Sesiones
        tabbedPane.addTab("Sesiones", new IconoSwing(Icono.SESIONES, Tema.PRIMARIO, 16), crearPanelSesiones());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Tema.SUPERFICIE);
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        JButton btnEditar = Tema.botonPrimario("Editar", Icono.EDITAR);
        btnEditar.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Funcionalidad en desarrollo", "Info", JOptionPane.INFORMATION_MESSAGE);
        });
        footerPanel.add(btnEditar);

        JButton btnCerrar = Tema.botonSecundario("Cerrar", Icono.VOLVER);
        btnCerrar.addActionListener(e -> this.dispose());
        footerPanel.add(btnCerrar);
        Tema.atajoEscape(getRootPane(), btnCerrar, this::dispose);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
    }

    private JPanel crearPanelPersonal() {
        JPanel panel = new JPanel();
        panel.setBackground(Tema.FONDO);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Sección: Datos Básicos
        panel.add(crearSeccion("DATOS BÁSICOS", 
            new String[]{"Nombre Completo", "Email", "Teléfono", "Género", "Fecha Nacimiento", "Dirección"},
            new String[]{
                pacienteActual != null ? pacienteActual.getNombre() + " " + pacienteActual.getApellido() : "N/A",
                pacienteActual != null ? (pacienteActual.getEmail() != null ? pacienteActual.getEmail() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getTelefono() != null ? pacienteActual.getTelefono() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getGenero() != null ? pacienteActual.getGenero() : "No especificado") : "N/A",
                pacienteActual != null && pacienteActual.getFechaNacimiento() != null ? 
                    pacienteActual.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "No especificada",
                pacienteActual != null ? (pacienteActual.getDireccion() != null ? pacienteActual.getDireccion() : "No especificada") : "N/A"
            }
        ));

        panel.add(Box.createVerticalStrut(20));

        // Sección: Motivo de Consulta
        panel.add(crearSeccionTexto("MOTIVO DE CONSULTA", 
            pacienteActual != null && pacienteActual.getMotivoConsulta() != null ? 
                pacienteActual.getMotivoConsulta() : "No especificado"));

        panel.add(Box.createVerticalStrut(20));

        // Sección: Antecedentes
        if (pacienteActual != null) {
            panel.add(crearSeccionTexto("ANTECEDENTES PERSONALES", 
                pacienteActual.getAntecedentesPersonales() != null ? 
                    pacienteActual.getAntecedentesPersonales() : "No especificado"));

            panel.add(Box.createVerticalStrut(10));

            panel.add(crearSeccionTexto("ANTECEDENTES FAMILIARES", 
                pacienteActual.getAntecedenteFamiliares() != null ? 
                    pacienteActual.getAntecedenteFamiliares() : "No especificado"));

            panel.add(Box.createVerticalStrut(10));

            panel.add(crearSeccionTexto("ANAMNESIS", 
                pacienteActual.getAnamnesis() != null ? 
                    pacienteActual.getAnamnesis() : "No especificada"));
        }

        panel.add(Box.createVerticalGlue());
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBackground(Tema.FONDO);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel crearPanelClinica() {
        JPanel panel = new JPanel();
        panel.setBackground(Tema.FONDO);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        if (historiaActual != null) {
            panel.add(crearSeccionTexto("MOTIVO DE CONSULTA", 
                historiaActual.getMotivoConsulta() != null ? 
                    historiaActual.getMotivoConsulta() : "No especificado"));

            panel.add(Box.createVerticalStrut(20));

            panel.add(crearSeccionTexto("OBSERVACIONES GENERALES", 
                historiaActual.getObservacionesGenerales() != null ? 
                    historiaActual.getObservacionesGenerales() : "No especificado"));

            panel.add(Box.createVerticalStrut(20));

            panel.add(crearSeccionTexto("DIAGNÓSTICO", 
                historiaActual.getDiagnostico() != null ? 
                    historiaActual.getDiagnostico() : "No especificado"));

            panel.add(Box.createVerticalStrut(20));

            panel.add(crearSeccionTexto("TRATAMIENTO", 
                historiaActual.getTratamiento() != null ? 
                    historiaActual.getTratamiento() : "No especificado"));

            panel.add(Box.createVerticalStrut(20));

            if (historiaActual.getUltimaActualizacion() != null) {
                JLabel lblFecha = new JLabel("Última actualización: " + 
                    historiaActual.getUltimaActualizacion().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                lblFecha.setFont(Tema.TEXTO_ITALICA_CHICA);
                lblFecha.setForeground(Tema.TEXTO_SECUNDARIO);
                panel.add(lblFecha);
            }
        } else {
            JLabel lblNoData = new JLabel("No hay información clínica registrada para este paciente");
            lblNoData.setFont(Tema.TEXTO);
            lblNoData.setForeground(Tema.TEXTO_SECUNDARIO);
            panel.add(lblNoData);
        }

        panel.add(Box.createVerticalGlue());
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBackground(Tema.FONDO);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel crearPanelSesiones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columnas = {"ID", "Fecha", "Psicólogo", "Duración", "Estado"};
        DefaultTableModel modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tablaSesiones = new JTable(modeloTabla);
        Tema.estilizarTabla(tablaSesiones);

        if (pacienteActual != null) {
            List<Sesion> sesiones = sesionDAO.obtenerPorPaciente(pacienteActual.getId());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            
            for (Sesion s : sesiones) {
                Usuario u = usuarioDAO.obtenerPorId(s.getPsicologoId());
                modeloTabla.addRow(new Object[]{
                    s.getId(),
                    s.getFechaCreacion().format(formatter),
                    u != null ? u.getNombre() : "N/A",
                    s.getObservaciones() != null ? s.getObservaciones() : "N/A",
                    "Completada"
                });
            }
        }

        JScrollPane scrollPane = new JScrollPane(tablaSesiones);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearSeccion(String titulo, String[] etiquetas, String[] valores) {
        JPanel panel = new JPanel();
        panel.setBackground(Tema.SUPERFICIE);
        panel.setLayout(new GridLayout(etiquetas.length, 2, 10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(titulo),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        for (int i = 0; i < etiquetas.length; i++) {
            JLabel lblEtiqueta = new JLabel(etiquetas[i] + ":");
            lblEtiqueta.setFont(Tema.BOTON);
            lblEtiqueta.setForeground(Tema.PRIMARIO);
            panel.add(lblEtiqueta);

            JLabel lblValor = new JLabel(valores[i]);
            lblValor.setFont(Tema.TEXTO_CHICO);
            panel.add(lblValor);
        }

        return panel;
    }

    private JPanel crearSeccionTexto(String titulo, String contenido) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(titulo),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextArea txtArea = new JTextArea(4, 50);
        txtArea.setText(contenido);
        txtArea.setLineWrap(true);
        txtArea.setWrapStyleWord(true);
        txtArea.setEditable(false);
        txtArea.setBackground(Tema.BORDE_SUAVE);
        txtArea.setFont(Tema.TEXTO_CHICO);

        JScrollPane scrollPane = new JScrollPane(txtArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void cargarDatos() {
        if (pacienteActual == null) {
            JOptionPane.showMessageDialog(this, "Paciente no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
            this.dispose();
        }
    }

    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> new DetallePaciente(1).setVisible(true));
    }
}
