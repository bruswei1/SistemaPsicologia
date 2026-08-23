package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import util.Auditoria;
import util.Sesion;

public class Sesiones extends JFrame {

    private JComboBox<PacienteItem> cmbPaciente;
    private JTextArea txtSubjetivo;
    private JTextArea txtObjetivo;
    private JTextArea txtAnalisis;
    private JTextArea txtPlan;
    private JTextArea txtNotasPrivadas;

    private JLabel lblCronometro;
    private JButton btnIniciar;
    private JButton btnDetener;
    private Timer timer;
    private int segundosTranscurridos;
    private Integer duracionMinutosRegistrada;

    private JTable tablaSesiones;
    private DefaultTableModel modeloTabla;

    private Integer sesionIdActual;

    public Sesiones() {
        setTitle("Notas de Sesión (SOAP)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(Tema.iconoApp());
        initComponents();
        cargarPacientes();
        setSize(820, 640);
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        cmbPaciente = new JComboBox<>();
        cmbPaciente.addActionListener(evt -> {
            limpiarFormularioSesion();
            cargarSesiones();
        });

        lblCronometro = new JLabel("00:00");
        lblCronometro.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCronometro.setForeground(Tema.PRIMARIO);
        btnIniciar = Tema.botonExito("Iniciar sesión");
        btnDetener = Tema.botonPeligro("Detener");
        btnDetener.setEnabled(false);
        btnIniciar.addActionListener(evt -> iniciarCronometro());
        btnDetener.addActionListener(evt -> detenerCronometro());

        JPanel panelCronometro = new JPanel();
        panelCronometro.setBackground(Tema.SUPERFICIE);
        panelCronometro.add(new JLabel("Cronómetro:"));
        panelCronometro.add(lblCronometro);
        panelCronometro.add(btnIniciar);
        panelCronometro.add(btnDetener);

        JPanel panelPaciente = new JPanel(new BorderLayout(4, 4));
        panelPaciente.add(new JLabel("Paciente*: "), BorderLayout.WEST);
        panelPaciente.add(cmbPaciente, BorderLayout.CENTER);
        panelPaciente.add(panelCronometro, BorderLayout.EAST);

        txtSubjetivo = new JTextArea(4, 20);
        txtObjetivo = new JTextArea(4, 20);
        txtAnalisis = new JTextArea(4, 20);
        txtPlan = new JTextArea(4, 20);
        txtNotasPrivadas = new JTextArea(4, 20);

        JPanel panelSoap = new JPanel(new GridLayout(2, 2, 6, 6));
        panelSoap.add(campoSoap("Subjetivo", txtSubjetivo));
        panelSoap.add(campoSoap("Objetivo", txtObjetivo));
        panelSoap.add(campoSoap("Análisis", txtAnalisis));
        panelSoap.add(campoSoap("Plan", txtPlan));

        JPanel panelPrivado = campoSoap("Notas privadas (no forman parte del informe)", txtNotasPrivadas);

        JButton btnGuardar = Tema.botonExito("Guardar sesión", Icono.GUARDAR);
        btnGuardar.addActionListener(evt -> guardarSesion());

        JButton btnNueva = Tema.botonPrimario("Nueva sesión", Icono.NUEVO);
        btnNueva.addActionListener(evt -> limpiarFormularioSesion());

        JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
        btnVolver.addActionListener(evt -> volverAlMenu());

        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(Tema.SUPERFICIE);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnNueva);
        panelBotones.add(btnVolver);

        JPanel panelFormulario = new JPanel(new BorderLayout(6, 6));
        panelFormulario.setBackground(Tema.SUPERFICIE);
        panelFormulario.add(panelPaciente, BorderLayout.NORTH);
        panelFormulario.add(panelSoap, BorderLayout.CENTER);
        panelFormulario.add(panelPrivado, BorderLayout.SOUTH);

        modeloTabla = new DefaultTableModel(new Object[]{"ID", "Fecha", "Duración (min)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaSesiones = new JTable(modeloTabla);
        Tema.estilizarTabla(tablaSesiones);
        tablaSesiones.getSelectionModel().addListSelectionListener(this::onSeleccionarSesion);
        JScrollPane scrollTabla = new JScrollPane(tablaSesiones);
        scrollTabla.setBorder(javax.swing.BorderFactory.createTitledBorder("Sesiones registradas"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelFormulario, scrollTabla);
        split.setResizeWeight(0.72);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(panelBotones, BorderLayout.SOUTH);
    }

    private JPanel campoSoap(String etiqueta, JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(new JLabel(etiqueta), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void iniciarCronometro() {
        btnIniciar.setEnabled(false);
        btnDetener.setEnabled(true);
        timer = new Timer(1000, evt -> {
            segundosTranscurridos++;
            lblCronometro.setText(String.format("%02d:%02d", segundosTranscurridos / 60, segundosTranscurridos % 60));
        });
        timer.start();
    }

    private void detenerCronometro() {
        if (timer != null) {
            timer.stop();
        }
        btnIniciar.setEnabled(true);
        btnDetener.setEnabled(false);
        duracionMinutosRegistrada = Math.max(1, Math.round(segundosTranscurridos / 60.0f));
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

    private Integer pacienteSeleccionadoId() {
        PacienteItem item = (PacienteItem) cmbPaciente.getSelectedItem();
        return item != null ? item.id : null;
    }

    private void cargarSesiones() {

        modeloTabla.setRowCount(0);

        Integer pacienteId = pacienteSeleccionadoId();
        if (pacienteId == null) {
            return;
        }

        String sql = "SELECT id, fecha, duracion_minutos FROM sesiones WHERE paciente_id=? ORDER BY fecha DESC";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, pacienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modeloTabla.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getTimestamp("fecha"),
                        rs.getObject("duracion_minutos")
                    });
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar sesiones: " + e.getMessage());
        }
    }

    private void onSeleccionarSesion(ListSelectionEvent evt) {

        if (evt.getValueIsAdjusting()) {
            return;
        }

        int fila = tablaSesiones.getSelectedRow();
        if (fila < 0) {
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        cargarSesion(id);
    }

    private void cargarSesion(int id) {

        String sql = "SELECT * FROM sesiones WHERE id=?";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return;
                }

                sesionIdActual = id;
                txtSubjetivo.setText(valorOVacio(rs.getString("subjetivo")));
                txtObjetivo.setText(valorOVacio(rs.getString("objetivo")));
                txtAnalisis.setText(valorOVacio(rs.getString("analisis")));
                txtPlan.setText(valorOVacio(rs.getString("plan")));
                txtNotasPrivadas.setText(valorOVacio(rs.getString("notas_privadas")));

                Object duracion = rs.getObject("duracion_minutos");
                duracionMinutosRegistrada = duracion != null ? (Integer) duracion : null;
                segundosTranscurridos = 0;
                lblCronometro.setText(duracionMinutosRegistrada != null ? duracionMinutosRegistrada + " min (guardado)" : "00:00");
            }

            Auditoria.registrar(cn, "VER_SESION", "sesiones", id, null);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar la sesión: " + e.getMessage());
        }
    }

    private void guardarSesion() {

        Integer pacienteId = pacienteSeleccionadoId();
        if (pacienteId == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un paciente");
            return;
        }

        boolean esEdicion = sesionIdActual != null;

        String sql = esEdicion
            ? "UPDATE sesiones SET subjetivo=?, objetivo=?, analisis=?, plan=?, notas_privadas=?, duracion_minutos=? WHERE id=?"
            : "INSERT INTO sesiones (paciente_id, psicologo_id, subjetivo, objetivo, analisis, plan, notas_privadas, duracion_minutos) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {

                if (esEdicion) {
                    setTextoONulo(ps, 1, txtSubjetivo.getText().trim());
                    setTextoONulo(ps, 2, txtObjetivo.getText().trim());
                    setTextoONulo(ps, 3, txtAnalisis.getText().trim());
                    setTextoONulo(ps, 4, txtPlan.getText().trim());
                    setTextoONulo(ps, 5, txtNotasPrivadas.getText().trim());
                    if (duracionMinutosRegistrada != null) {
                        ps.setInt(6, duracionMinutosRegistrada);
                    } else {
                        ps.setNull(6, Types.INTEGER);
                    }
                    ps.setInt(7, sesionIdActual);
                } else {
                    ps.setInt(1, pacienteId);
                    ps.setInt(2, Sesion.getUsuarioId());
                    setTextoONulo(ps, 3, txtSubjetivo.getText().trim());
                    setTextoONulo(ps, 4, txtObjetivo.getText().trim());
                    setTextoONulo(ps, 5, txtAnalisis.getText().trim());
                    setTextoONulo(ps, 6, txtPlan.getText().trim());
                    setTextoONulo(ps, 7, txtNotasPrivadas.getText().trim());
                    if (duracionMinutosRegistrada != null) {
                        ps.setInt(8, duracionMinutosRegistrada);
                    } else {
                        ps.setNull(8, Types.INTEGER);
                    }
                }

                ps.executeUpdate();
            }

            if (esEdicion) {
                Auditoria.registrar(cn, "EDITAR_SESION", "sesiones", sesionIdActual, null);
            } else {
                try (PreparedStatement ps = cn.prepareStatement("SELECT LAST_INSERT_ID() AS id");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    Auditoria.registrar(cn, "CREAR_SESION", "sesiones", rs.getInt("id"), null);
                }
            }

            JOptionPane.showMessageDialog(this, esEdicion ? "Sesión actualizada" : "Sesión registrada");
            limpiarFormularioSesion();
            cargarSesiones();

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

    private String valorOVacio(String valor) {
        return valor != null ? valor : "";
    }

    private void limpiarFormularioSesion() {
        sesionIdActual = null;
        tablaSesiones.clearSelection();
        txtSubjetivo.setText("");
        txtObjetivo.setText("");
        txtAnalisis.setText("");
        txtPlan.setText("");
        txtNotasPrivadas.setText("");
        segundosTranscurridos = 0;
        duracionMinutosRegistrada = null;
        lblCronometro.setText("00:00");
        if (timer != null) {
            timer.stop();
        }
        btnIniciar.setEnabled(true);
        btnDetener.setEnabled(false);
    }

    private void volverAlMenu() {
        if (timer != null) {
            timer.stop();
        }
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
}
