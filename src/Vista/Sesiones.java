package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

public class Sesiones extends JPanel {

    private final Runnable alVolver;

    private JComboBox<PacienteItem> cmbPaciente;
    private JComboBox<TurnoItem> cmbTurno;
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

    public Sesiones(Runnable alVolver) {
        this.alVolver = alVolver;
        initComponents();
        cargarPacientes();
    }

    public void refrescar() {
        cargarPacientes();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        cmbPaciente = new JComboBox<>();
        cmbPaciente.addActionListener(evt -> {
            limpiarFormularioSesion();
            cargarTurnosDelPaciente();
            cargarSesiones();
        });

        cmbTurno = new JComboBox<>();

        lblCronometro = new JLabel("00:00");
        lblCronometro.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCronometro.setForeground(Tema.PRIMARIO);
        btnIniciar = Tema.botonExito("Iniciar sesión");
        btnDetener = Tema.botonPeligro("Pausar");
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
        panelPaciente.setBackground(Tema.SUPERFICIE);
        panelPaciente.add(new JLabel("Paciente*: "), BorderLayout.WEST);
        panelPaciente.add(cmbPaciente, BorderLayout.CENTER);
        panelPaciente.add(panelCronometro, BorderLayout.EAST);

        JPanel panelTurno = new JPanel(new BorderLayout(4, 4));
        panelTurno.setBackground(Tema.SUPERFICIE);
        panelTurno.add(new JLabel("Turno asociado (opcional): "), BorderLayout.WEST);
        panelTurno.add(cmbTurno, BorderLayout.CENTER);

        JPanel panelPacienteYTurno = new JPanel();
        panelPacienteYTurno.setBackground(Tema.SUPERFICIE);
        panelPacienteYTurno.setLayout(new BoxLayout(panelPacienteYTurno, BoxLayout.Y_AXIS));
        panelPacienteYTurno.add(panelPaciente);
        panelPacienteYTurno.add(panelTurno);

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

        JButton btnExportar = Tema.botonPrimario("Exportar nota", Icono.EXPORTAR);
        btnExportar.addActionListener(evt -> exportarNota());

        JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
        btnVolver.addActionListener(evt -> volverAlMenu());
        Tema.atajoEscape(this, btnVolver, this::volverAlMenu);

        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(Tema.SUPERFICIE);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnNueva);
        panelBotones.add(btnExportar);
        panelBotones.add(btnVolver);

        JPanel panelFormulario = new JPanel(new BorderLayout(6, 6));
        panelFormulario.setBackground(Tema.SUPERFICIE);
        panelFormulario.add(panelPacienteYTurno, BorderLayout.NORTH);
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

        add(split, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
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

    private void cargarTurnosDelPaciente() {
        cmbTurno.removeAllItems();
        cmbTurno.addItem(new TurnoItem(null, "(Sin turno asociado)"));

        Integer pacienteId = pacienteSeleccionadoId();
        if (pacienteId == null) {
            return;
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String sql = "SELECT id, fecha_hora, estado FROM turnos WHERE paciente_id = ? ";
        if (Sesion.esPsicologo()) {
            sql += "AND psicologo_id = ? ";
        }
        sql += "ORDER BY fecha_hora DESC";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, pacienteId);
            if (Sesion.esPsicologo()) {
                ps.setInt(2, Sesion.getUsuarioId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String etiqueta = rs.getTimestamp("fecha_hora").toLocalDateTime().format(formato)
                        + " (" + rs.getString("estado") + ")";
                    cmbTurno.addItem(new TurnoItem(rs.getInt("id"), etiqueta));
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar turnos: " + e.getMessage());
        }
    }

    private void seleccionarTurno(Integer turnoId) {
        for (int i = 0; i < cmbTurno.getItemCount(); i++) {
            TurnoItem item = cmbTurno.getItemAt(i);
            if ((turnoId == null && item.id == null) || (turnoId != null && turnoId.equals(item.id))) {
                cmbTurno.setSelectedIndex(i);
                return;
            }
        }
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
                Object turnoIdRaw = rs.getObject("turno_id");
                seleccionarTurno(turnoIdRaw != null ? (Integer) turnoIdRaw : null);

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
        TurnoItem turnoSeleccionado = (TurnoItem) cmbTurno.getSelectedItem();
        Integer turnoId = turnoSeleccionado != null ? turnoSeleccionado.id : null;

        String sql = esEdicion
            ? "UPDATE sesiones SET subjetivo=?, objetivo=?, analisis=?, plan=?, notas_privadas=?, duracion_minutos=?, turno_id=? WHERE id=?"
            : "INSERT INTO sesiones (paciente_id, psicologo_id, subjetivo, objetivo, analisis, plan, notas_privadas, duracion_minutos, turno_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                    if (turnoId != null) {
                        ps.setInt(7, turnoId);
                    } else {
                        ps.setNull(7, Types.INTEGER);
                    }
                    ps.setInt(8, sesionIdActual);
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
                    if (turnoId != null) {
                        ps.setInt(9, turnoId);
                    } else {
                        ps.setNull(9, Types.INTEGER);
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

            // Si la nota queda vinculada a un turno que todavía estaba "programado", asumimos que
            // la sesión se llevó a cabo y lo pasamos a "completado" (no pisa cancelado/ausente,
            // por si se marcó así a propósito).
            boolean turnoCompletado = false;
            if (turnoId != null) {
                try (PreparedStatement psTurno = cn.prepareStatement(
                        "UPDATE turnos SET estado='completado' WHERE id=? AND estado='programado'")) {
                    psTurno.setInt(1, turnoId);
                    turnoCompletado = psTurno.executeUpdate() > 0;
                }
                if (turnoCompletado) {
                    Auditoria.registrar(cn, "COMPLETAR_TURNO", "turnos", turnoId, "Completado automáticamente al guardar la sesión");
                }
            }

            JOptionPane.showMessageDialog(this, (esEdicion ? "Sesión actualizada" : "Sesión registrada")
                + (turnoCompletado ? "\nEl turno asociado se marcó como completado." : ""));
            limpiarFormularioSesion();
            cargarTurnosDelPaciente();
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

    private void exportarNota() {
        Integer pacienteId = pacienteSeleccionadoId();
        if (pacienteId == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un paciente");
            return;
        }
        if (txtSubjetivo.getText().trim().isEmpty() && txtObjetivo.getText().trim().isEmpty()
                && txtAnalisis.getText().trim().isEmpty() && txtPlan.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos cargados en la nota para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PacienteItem paciente = (PacienteItem) cmbPaciente.getSelectedItem();
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("NOTA DE SESIÓN (SOAP)\n");
        sb.append("================================================================================\n\n");
        sb.append("Paciente: ").append(paciente != null ? paciente.nombre : "N/A").append("\n");
        sb.append("Fecha de exportación: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");
        sb.append("SUBJETIVO\n--------------------------------------------------------------------------------\n");
        sb.append(txtSubjetivo.getText().trim()).append("\n\n");
        sb.append("OBJETIVO\n--------------------------------------------------------------------------------\n");
        sb.append(txtObjetivo.getText().trim()).append("\n\n");
        sb.append("ANÁLISIS\n--------------------------------------------------------------------------------\n");
        sb.append(txtAnalisis.getText().trim()).append("\n\n");
        sb.append("PLAN\n--------------------------------------------------------------------------------\n");
        sb.append(txtPlan.getText().trim()).append("\n");
        sb.append("================================================================================\n");

        java.io.File carpetaReportes = new java.io.File("reportes");
        if (!carpetaReportes.exists()) {
            carpetaReportes.mkdirs();
        }
        String nombreArchivo = "NotaSOAP_" + (paciente != null ? paciente.nombre.replace(" ", "_") : "paciente")
            + "_" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        String rutaArchivo = new java.io.File(carpetaReportes, nombreArchivo).getPath();

        try {
            if (util.GeneradorReportes.guardarReportePDF(sb.toString(), rutaArchivo)) {
                JOptionPane.showMessageDialog(this, "Nota exportada en: " + rutaArchivo, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al exportar la nota", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormularioSesion() {
        sesionIdActual = null;
        tablaSesiones.clearSelection();
        if (cmbTurno.getItemCount() > 0) {
            cmbTurno.setSelectedIndex(0);
        }
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
        alVolver.run();
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

    private static class TurnoItem {
        final Integer id;
        final String etiqueta;

        TurnoItem(Integer id, String etiqueta) {
            this.id = id;
            this.etiqueta = etiqueta;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
