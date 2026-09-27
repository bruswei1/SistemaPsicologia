package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import util.Auditoria;
import util.Sesion;

public class Agenda extends JPanel {

    private static final String[] ESTADOS = {"programado", "completado", "cancelado", "ausente"};

    /** Duración fija de una cita (la columna turnos.duracion_minutos se mantiene por compatibilidad). */
    private static final int DURACION_MINUTOS = 45;

    private final Runnable alVolver;
    private final boolean construirLayoutPropio;

    private JComboBox<PacienteItem> cmbPaciente;
    private JComboBox<PsicologoItem> cmbPsicologo;
    private SelectorFechaHora selectorFechaHora;
    private JComboBox<String> cmbEstado;
    private JTextField txtNotas;
    private JButton btnGuardar;
    private JButton btnEliminar;
    /** Formulario de la cita en un panel lateral (se abre con "Nueva cita" o al tocar una cita). */
    private PanelDeslizable panelFormulario;

    /** Lista completa de estudiantes, para poder filtrar el combo por nombre o cédula al escribir. */
    private final List<PacienteItem> pacientesTodos = new ArrayList<>();
    private boolean filtrandoCombo;

    private JTable tablaTurnos;
    private DefaultTableModel modeloTabla;
    private CardLayout cardLayoutVista;
    private JPanel panelVista;
    private PanelCalendario panelCalendario;
    private JButton btnVerCalendario;
    private boolean vistaCalendario;
    private LocalDate filtroDia;
    private JPanel tarjetaTabla;

    private Integer turnoIdActual;

    public Agenda(Runnable alVolver) {
        this(alVolver, true);
    }

    /**
     * @param construirLayoutPropio si es false, no arma su propio encabezado ni el botón
     *                              "Volver al menú" — se usa cuando este panel se embebe como
     *                              pestaña dentro de otro contenedor (ver {@link EstudiantesPanel}).
     */
    public Agenda(Runnable alVolver, boolean construirLayoutPropio) {
        this.alVolver = alVolver;
        this.construirLayoutPropio = construirLayoutPropio;
        initComponents();
        cargarPacientes();
        cargarPsicologos();
        cargarTurnos();
    }

    public void refrescar() {
        cargarPacientes();
        cargarPsicologos();
        cargarTurnos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Tema.SUPERFICIE);
        panelForm.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbPaciente = new JComboBox<>();
        configurarBusquedaEstudiante();
        cmbPsicologo = new JComboBox<>();
        selectorFechaHora = new SelectorFechaHora();
        cmbEstado = new JComboBox<>(ESTADOS);
        // Muestra "Programada", "Realizada", "No asistió"... pero guarda el valor crudo de la base.
        cmbEstado.setRenderer(Tema.rendererEstadoCombo());
        txtNotas = new JTextField(20);

        int fila = 0;
        agregarCampo(panelForm, gbc, fila++, "Estudiante*", cmbPaciente);
        agregarCampo(panelForm, gbc, fila++, "Profesional de Psicología*", cmbPsicologo);
        agregarCampo(panelForm, gbc, fila++, "Fecha y hora*", selectorFechaHora);
        agregarCampo(panelForm, gbc, fila++, "Estado", cmbEstado);
        agregarCampo(panelForm, gbc, fila++, "Notas", txtNotas);

        btnGuardar = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardar.setToolTipText("Guarda la cita y cierra el panel");
        btnGuardar.addActionListener(evt -> guardarTurno());

        JButton btnCancelar = Tema.botonSecundario("Cancelar");
        btnCancelar.setToolTipText("Cierra el panel sin guardar (Esc)");
        btnCancelar.addActionListener(evt -> cerrarFormulario());

        JPanel pieFormulario = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pieFormulario.setOpaque(false);
        pieFormulario.add(btnGuardar);
        pieFormulario.add(btnCancelar);

        panelForm.setOpaque(false);
        panelForm.setBorder(BorderFactory.createEmptyBorder());
        panelFormulario = new PanelDeslizable(440, panelForm, pieFormulario, this::cerrarFormulario);

        // Barra de acciones sobre la lista / el calendario.
        JButton btnNuevo = Tema.botonExito("Nueva cita", Icono.NUEVO);
        btnNuevo.setToolTipText("Abre el panel lateral para agendar una cita");
        btnNuevo.addActionListener(evt -> nuevaCita());

        btnEliminar = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminar.setToolTipText("Elimina la cita seleccionada");
        btnEliminar.addActionListener(evt -> eliminarTurno());
        btnEliminar.setEnabled(false);

        btnVerCalendario = Tema.botonSecundario("Ver calendario", Icono.AGENDA);
        btnVerCalendario.addActionListener(evt -> {
            vistaCalendario = !vistaCalendario;
            Tema.cambiarConFundido(SwingUtilities.getWindowAncestor(this), panelVista, cardLayoutVista,
                vistaCalendario ? "calendario" : "lista", !vistaCalendario);
            btnVerCalendario.setText(vistaCalendario ? "Ver lista" : "Ver calendario");
            if (vistaCalendario) {
                panelCalendario.refrescar();
            }
        });

        JLabel lblAyuda = new JLabel("Tocá una cita para verla o editarla");
        lblAyuda.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblAyuda.setForeground(Tema.TEXTO_SECUNDARIO);

        JPanel accionesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accionesIzquierda.setOpaque(false);
        accionesIzquierda.add(btnNuevo);
        accionesIzquierda.add(btnEliminar);
        accionesIzquierda.add(lblAyuda);

        JPanel accionesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accionesDerecha.setOpaque(false);
        accionesDerecha.add(btnVerCalendario);
        if (construirLayoutPropio) {
            JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
            btnVolver.addActionListener(evt -> alVolver.run());
            Tema.atajoEscape(this, btnVolver, () -> {
                if (formularioAbierto()) {
                    cerrarFormulario();
                } else {
                    alVolver.run();
                }
            });
            accionesDerecha.add(btnVolver);
        }

        JPanel barraAcciones = new JPanel(new BorderLayout());
        barraAcciones.setOpaque(false);
        barraAcciones.add(accionesIzquierda, BorderLayout.WEST);
        barraAcciones.add(accionesDerecha, BorderLayout.EAST);
        Tema.compactarAlAchicar(barraAcciones, btnEliminar, btnVerCalendario);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Estudiante", "Curso", "Profesional", "Fecha y hora", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaTurnos = Tema.crearTablaConVacio(modeloTabla, "No hay citas para mostrar");
        Tema.estilizarTabla(tablaTurnos);
        tablaTurnos.getColumnModel().getColumn(5).setCellRenderer(Tema.rendererEstado());
        tablaTurnos.getSelectionModel().addListSelectionListener(this::onSeleccionarTurno);

        JScrollPane scrollTabla = new JScrollPane(tablaTurnos);
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());
        tarjetaTabla = Tema.panelTarjeta("Citas");
        tarjetaTabla.add(scrollTabla, BorderLayout.CENTER);

        panelCalendario = new PanelCalendario(this::filtrarPorDia);

        cardLayoutVista = new CardLayout();
        panelVista = new JPanel(cardLayoutVista);
        panelVista.add(tarjetaTabla, "lista");
        panelVista.add(panelCalendario, "calendario");

        if (construirLayoutPropio) {
            add(Tema.panelEncabezado("Agenda de Citas", "Programar y gestionar citas de estudiantes"),
                BorderLayout.NORTH);
        }

        // Antes el formulario ocupaba siempre la mitad de arriba; ahora la lista/calendario usa
        // todo el alto y el formulario aparece al costado solo cuando hace falta.
        JPanel columnaCitas = new JPanel(new BorderLayout(0, 10));
        columnaCitas.setOpaque(false);
        columnaCitas.add(barraAcciones, BorderLayout.NORTH);
        columnaCitas.add(panelVista, BorderLayout.CENTER);

        JPanel principal = new JPanel(new BorderLayout(10, 0));
        principal.setBackground(Tema.FONDO);
        principal.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        principal.add(columnaCitas, BorderLayout.CENTER);
        principal.add(panelFormulario, BorderLayout.EAST);
        add(principal, BorderLayout.CENTER);
    }

    private void nuevaCita() {
        limpiarCampos();
        panelFormulario.setTitulo("Nueva cita", "Elegí estudiante, profesional, fecha y hora");
        panelFormulario.abrir();
        SwingUtilities.invokeLater(() -> {
            // Foco en el buscador de estudiante con todo el texto seleccionado (escribir lo
            // reemplaza para buscar), pero mostrando el comienzo del nombre.
            JTextField editor = (JTextField) cmbPaciente.getEditor().getEditorComponent();
            editor.requestFocusInWindow();
            editor.setCaretPosition(editor.getText().length());
            editor.moveCaretPosition(0);
        });
    }

    boolean formularioAbierto() {
        return panelFormulario.estaAbierto();
    }

    /** Cierra el panel de la cita y deja la tabla sin selección. */
    void cerrarFormulario() {
        turnoIdActual = null;
        tablaTurnos.clearSelection();
        btnEliminar.setEnabled(false);
        panelFormulario.cerrar();
    }

    private void filtrarPorDia(LocalDate dia) {
        filtroDia = dia;
        vistaCalendario = false;
        Tema.cambiarConFundido(SwingUtilities.getWindowAncestor(this), panelVista, cardLayoutVista, "lista", true);
        btnVerCalendario.setText("Ver calendario");
        cargarTurnos();
    }

    /** Etiqueta arriba y campo abajo, a todo el ancho del panel lateral. */
    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int fila, String etiqueta, java.awt.Component campo) {
        gbc.gridx = 0;
        gbc.gridy = fila * 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(fila == 0 ? 0 : 10, 0, 4, 0);
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(Tema.fuente(java.awt.Font.BOLD, 12));
        lbl.setForeground(Tema.TEXTO_SECUNDARIO);
        panel.add(lbl, gbc);

        gbc.gridy = fila * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(campo, gbc);
    }

    private void cargarPacientes() {
        Tema.enSegundoPlano(
            this,
            this::cargarPacientesDesdeBaseDeDatos,
            items -> {
                cmbPaciente.removeAllItems();
                pacientesTodos.clear();
                pacientesTodos.addAll(items);
                for (PacienteItem item : items) {
                    cmbPaciente.addItem(item);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar estudiantes: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private List<PacienteItem> cargarPacientesDesdeBaseDeDatos() throws Exception {
        String sql = "SELECT id, nombre, apellido, curso, ci FROM pacientes ";
        if (Sesion.esPsicologo()) {
            sql += "WHERE psicologo_id = ? ";
        }
        sql += "ORDER BY nombre";

        List<PacienteItem> items = new ArrayList<>();
        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            if (Sesion.esPsicologo()) {
                ps.setInt(1, Sesion.getUsuarioId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new PacienteItem(rs.getInt("id"),
                        rs.getString("nombre") + " " + rs.getString("apellido"),
                        rs.getString("curso"), rs.getString("ci")));
                }
            }
        }
        return items;
    }

    /** Hace el combo de estudiantes editable y lo filtra por nombre o cédula a medida que se escribe. */
    private void configurarBusquedaEstudiante() {
        cmbPaciente.setEditable(true);
        final JTextField editor = (JTextField) cmbPaciente.getEditor().getEditorComponent();
        // El texto del estudiante es largo (nombre · curso · CI): al elegir uno, el campo quedaba
        // mostrando el final del texto y no el nombre. Se vuelve al comienzo.
        cmbPaciente.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            if (!editor.isFocusOwner()) {
                editor.setCaretPosition(0);
            }
        }));
        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { alEscribir(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { alEscribir(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { alEscribir(); }

            private void alEscribir() {
                if (filtrandoCombo) {
                    return;
                }
                Object sel = cmbPaciente.getSelectedItem();
                if (sel instanceof PacienteItem && sel.toString().equals(editor.getText())) {
                    return; // el texto es el de un estudiante recién elegido, no re-filtrar
                }
                SwingUtilities.invokeLater(() -> filtrarEstudiantes(editor));
            }
        });
    }

    private void filtrarEstudiantes(JTextField editor) {
        if (filtrandoCombo) {
            return;
        }
        String texto = editor.getText();
        String q = texto.trim().toLowerCase();

        filtrandoCombo = true;
        try {
            DefaultComboBoxModel<PacienteItem> modelo = new DefaultComboBoxModel<>();
            for (PacienteItem p : pacientesTodos) {
                boolean coincide = q.isEmpty()
                    || p.nombre.toLowerCase().contains(q)
                    || (p.ci != null && p.ci.toLowerCase().contains(q));
                if (coincide) {
                    modelo.addElement(p);
                }
            }
            cmbPaciente.setModel(modelo);
            cmbPaciente.setSelectedItem(null);
            editor.setText(texto);
            editor.setCaretPosition(editor.getText().length());
        } finally {
            filtrandoCombo = false;
        }

        if (!q.isEmpty() && cmbPaciente.getItemCount() > 0) {
            cmbPaciente.showPopup();
        } else {
            cmbPaciente.hidePopup();
        }
    }

    /** Repone la lista completa de estudiantes en el combo (sin disparar el filtro). */
    private void restaurarListaEstudiantes() {
        filtrandoCombo = true;
        try {
            DefaultComboBoxModel<PacienteItem> modelo = new DefaultComboBoxModel<>();
            for (PacienteItem p : pacientesTodos) {
                modelo.addElement(p);
            }
            cmbPaciente.setModel(modelo);
        } finally {
            filtrandoCombo = false;
        }
    }

    /** El estudiante elegido; null si se escribió texto libre sin elegir uno de la lista. */
    private PacienteItem resolverEstudianteSeleccionado() {
        Object sel = cmbPaciente.getSelectedItem();
        if (sel instanceof PacienteItem) {
            return (PacienteItem) sel;
        }
        if (cmbPaciente.getItemCount() == 1) {
            return cmbPaciente.getItemAt(0);
        }
        return null;
    }

    /** Pasa a "ausente" las citas "programado" cuyo día ya terminó y nadie tocó (ni atención ni estado). */
    private void marcarVencidasComoAusente() {
        // Inicio del día de HOY según la PC, no CURDATE() de MySQL: el servidor corre en UTC y
        // desde las 21:00 (hora de Paraguay) ya "es mañana" para él — las citas de la tarde/noche
        // de hoy se marcaban como ausentes antes de tiempo.
        String sql = "UPDATE turnos SET estado='ausente' "
            + "WHERE estado='programado' AND fecha_hora < ?"
            + (Sesion.esPsicologo() ? " AND psicologo_id = ?" : "");

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDate.now().atStartOfDay()));
            if (Sesion.esPsicologo()) {
                ps.setInt(2, Sesion.getUsuarioId());
            }
            int n = ps.executeUpdate();
            if (n > 0) {
                Auditoria.registrar(cn, "AUSENTE_AUTO", "turnos", null,
                    n + " cita(s) vencida(s) marcada(s) como \"No asistió\"");
            }

        } catch (Exception e) {
            System.out.println("Error marcando citas vencidas: " + e.getMessage());
        }
    }

    private void cargarPsicologos() {

        cmbPsicologo.removeAllItems();

        if (Sesion.esPsicologo()) {
            cmbPsicologo.addItem(new PsicologoItem(Sesion.getUsuarioId(), Sesion.getNombre()));
            cmbPsicologo.setSelectedIndex(0);
            cmbPsicologo.setEnabled(false);
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                String sql = "SELECT id, nombre FROM usuarios WHERE rol='psicologo' AND activo=1 ORDER BY nombre";
                List<PsicologoItem> items = new ArrayList<>();
                try (Connection cn = new Conexion().conectar();
                     PreparedStatement ps = cn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new PsicologoItem(rs.getInt("id"), rs.getString("nombre")));
                    }
                }
                return items;
            },
            items -> {
                cmbPsicologo.removeAllItems();
                for (PsicologoItem item : items) {
                    cmbPsicologo.addItem(item);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar psicólogos: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void guardarTurno() {

        PacienteItem paciente = resolverEstudianteSeleccionado();
        PsicologoItem psicologo = (PsicologoItem) cmbPsicologo.getSelectedItem();

        if (paciente == null || psicologo == null) {
            JOptionPane.showMessageDialog(this, "Elegí un estudiante de la lista y un profesional", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Timestamp fechaHora = selectorFechaHora.getTimestamp();
        int duracion = DURACION_MINUTOS;
        String estado = (String) cmbEstado.getSelectedItem();
        String notas = txtNotas.getText().trim();
        boolean esEdicion = turnoIdActual != null;
        Integer idActual = turnoIdActual;

        // Deshabilitado mientras dura todo el flujo (chequeo de solapamiento + guardado real): al
        // correr en segundo plano, un doble clic podía disparar dos citas duplicadas.
        btnGuardar.setEnabled(false);

        // Primero se chequea el solapamiento en segundo plano; recién si el usuario confirma (o no
        // hay solapamiento) se dispara el guardado real, también en segundo plano.
        Tema.enSegundoPlano(
            this,
            () -> hayTurnoSolapado(psicologo.id, fechaHora, duracion, esEdicion ? idActual : null),
            solapado -> {
                if (solapado) {
                    int opcion = JOptionPane.showConfirmDialog(this,
                        "Este horario se superpone con otra cita de " + psicologo.nombre + ". ¿Guardar de todos modos?",
                        "Citas superpuestas", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (opcion != JOptionPane.YES_OPTION) {
                        btnGuardar.setEnabled(true);
                        return;
                    }
                }
                guardarTurnoEnBaseDeDatos(paciente, psicologo, fechaHora, duracion, estado, notas, esEdicion, idActual);
            },
            error -> {
                btnGuardar.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    /**
     * creado_en explícito con la hora de la PC en el alta (el servidor MySQL corre en UTC);
     * fecha_hora es la hora de la cita elegida por quien agenda, no la actual, así que no
     * necesita este ajuste.
     */
    private void guardarTurnoEnBaseDeDatos(
            PacienteItem paciente, PsicologoItem psicologo, Timestamp fechaHora, int duracion,
            String estado, String notas, boolean esEdicion, Integer idActual) {

        Tema.enSegundoPlano(
            this,
            () -> {
                String sql = esEdicion
                    ? "UPDATE turnos SET paciente_id=?, psicologo_id=?, fecha_hora=?, duracion_minutos=?, estado=?, notas=? WHERE id=?"
                    : "INSERT INTO turnos (paciente_id, psicologo_id, fecha_hora, duracion_minutos, estado, notas, creado_en) VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (Connection cn = new Conexion().conectar()) {

                    if (cn == null) {
                        throw new IllegalStateException("No se pudo conectar a la base de datos");
                    }

                    try (PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, paciente.id);
                        ps.setInt(2, psicologo.id);
                        ps.setTimestamp(3, fechaHora);
                        ps.setInt(4, duracion);
                        ps.setString(5, estado);

                        if (notas.isEmpty()) {
                            ps.setNull(6, Types.VARCHAR);
                        } else {
                            ps.setString(6, notas);
                        }

                        if (esEdicion) {
                            ps.setInt(7, idActual);
                        } else {
                            ps.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
                        }

                        ps.executeUpdate();
                    }

                    if (esEdicion) {
                        Auditoria.registrar(cn, "EDITAR_TURNO", "turnos", idActual, null);
                    } else {
                        try (PreparedStatement ps = cn.prepareStatement("SELECT LAST_INSERT_ID() AS id");
                             ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            Auditoria.registrar(cn, "CREAR_TURNO", "turnos", rs.getInt("id"), null);
                        }
                    }
                }
                return null;
            },
            resultado -> {
                btnGuardar.setEnabled(true);
                Tema.mostrarNotificacion(this, esEdicion ? "Cita actualizada" : "Cita agendada");
                limpiarCampos();
                panelFormulario.cerrar();
            },
            error -> {
                btnGuardar.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    private void eliminarTurno() {
        if (turnoIdActual == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná una cita de la tabla para eliminar",
                "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(this, "¿Eliminar definitivamente esta cita?",
            "Eliminar cita", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        int idAEliminar = turnoIdActual;

        Tema.enSegundoPlano(
            this,
            () -> {
                try (Connection cn = new Conexion().conectar()) {

                    if (cn == null) {
                        throw new IllegalStateException("No se pudo conectar a la base de datos");
                    }

                    try (PreparedStatement ps = cn.prepareStatement("DELETE FROM turnos WHERE id=?")) {
                        ps.setInt(1, idAEliminar);
                        ps.executeUpdate();
                    }

                    Auditoria.registrar(cn, "ELIMINAR_TURNO", "turnos", idAEliminar, null);
                }
                return null;
            },
            resultado -> {
                Tema.mostrarNotificacion(this, "Cita eliminada");
                limpiarCampos();
                panelFormulario.cerrar();
            },
            error -> {
                if (error instanceof java.sql.SQLIntegrityConstraintViolationException) {
                    JOptionPane.showMessageDialog(this,
                        "No se puede eliminar: esta cita tiene una atención registrada. Cambiá el estado a \"cancelado\" en vez de borrarla.",
                        "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        );
    }

    private void cargarTurnos() {

        Tema.actualizarTituloTarjeta(tarjetaTabla, filtroDia != null
            ? "Citas del " + filtroDia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (clic en \"Quitar filtro\" en el calendario para ver todas)"
            : "Citas");

        LocalDate diaFiltro = filtroDia;

        Tema.enSegundoPlano(
            this,
            () -> cargarFilasTurnos(diaFiltro),
            filas -> {
                modeloTabla.setRowCount(0);
                for (Object[] fila : filas) {
                    modeloTabla.addRow(fila);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar citas: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    /** Corre en el hilo de fondo: primero pasa a "ausente" las citas vencidas, después trae la lista. */
    private List<Object[]> cargarFilasTurnos(LocalDate diaFiltro) throws Exception {
        marcarVencidasComoAusente();

        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT t.id, CONCAT(p.nombre,' ',p.apellido) AS paciente, p.curso AS curso, u.nombre AS psicologo, t.fecha_hora, t.estado "
            + "FROM turnos t JOIN pacientes p ON p.id = t.paciente_id JOIN usuarios u ON u.id = t.psicologo_id WHERE 1=1 ");

        if (Sesion.esPsicologo()) {
            sqlBuilder.append("AND t.psicologo_id = ? ");
        }
        if (diaFiltro != null) {
            sqlBuilder.append("AND DATE(t.fecha_hora) = ? ");
        }
        sqlBuilder.append("ORDER BY t.fecha_hora ASC");

        List<Object[]> filas = new ArrayList<>();

        try (Connection cn = new Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sqlBuilder.toString())) {

                int indice = 1;
                if (Sesion.esPsicologo()) {
                    ps.setInt(indice++, Sesion.getUsuarioId());
                }
                if (diaFiltro != null) {
                    ps.setDate(indice, java.sql.Date.valueOf(diaFiltro));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        filas.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("paciente"),
                            rs.getString("curso") != null ? rs.getString("curso") : "N/A",
                            rs.getString("psicologo"),
                            rs.getTimestamp("fecha_hora"),
                            rs.getString("estado")
                        });
                    }
                }
            }
        }
        return filas;
    }

    private void onSeleccionarTurno(ListSelectionEvent evt) {

        if (evt.getValueIsAdjusting()) {
            return;
        }

        int fila = tablaTurnos.getSelectedRow();
        if (fila < 0) {
            return;
        }

        int id = (int) modeloTabla.getValueAt(tablaTurnos.convertRowIndexToModel(fila), 0);
        cargarTurno(id);
    }

    private void cargarTurno(int id) {
        Tema.enSegundoPlano(
            this,
            () -> cargarTurnoDesdeBaseDeDatos(id),
            datos -> {
                if (datos == null) {
                    return;
                }
                turnoIdActual = id;
                btnEliminar.setEnabled(true);
                panelFormulario.setTitulo("Cita del " + datos.fechaHora.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")), "Modificá lo que haga falta y tocá Guardar");
                panelFormulario.abrir();
                seleccionarPaciente(datos.pacienteId);
                seleccionarPsicologo(datos.psicologoId);
                selectorFechaHora.setTimestamp(datos.fechaHora);
                cmbEstado.setSelectedItem(datos.estado);
                txtNotas.setText(datos.notas != null ? datos.notas : "");
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar la cita: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private static final class DatosTurno {
        int pacienteId;
        int psicologoId;
        Timestamp fechaHora;
        String estado;
        String notas;
    }

    private DatosTurno cargarTurnoDesdeBaseDeDatos(int id) throws Exception {
        String sql = "SELECT * FROM turnos WHERE id=?";

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                DatosTurno datos = new DatosTurno();
                datos.pacienteId = rs.getInt("paciente_id");
                datos.psicologoId = rs.getInt("psicologo_id");
                datos.fechaHora = rs.getTimestamp("fecha_hora");
                datos.estado = rs.getString("estado");
                datos.notas = rs.getString("notas");
                return datos;
            }
        }
    }

    private void seleccionarPaciente(int pacienteId) {
        restaurarListaEstudiantes();
        for (int i = 0; i < cmbPaciente.getItemCount(); i++) {
            if (cmbPaciente.getItemAt(i).id == pacienteId) {
                cmbPaciente.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarPsicologo(int psicologoId) {
        for (int i = 0; i < cmbPsicologo.getItemCount(); i++) {
            if (cmbPsicologo.getItemAt(i).id == psicologoId) {
                cmbPsicologo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void limpiarCampos() {
        turnoIdActual = null;
        btnEliminar.setEnabled(false);
        tablaTurnos.clearSelection();
        restaurarListaEstudiantes();
        if (cmbPaciente.getItemCount() > 0) {
            cmbPaciente.setSelectedIndex(0);
        }
        if (!Sesion.esPsicologo() && cmbPsicologo.getItemCount() > 0) {
            cmbPsicologo.setSelectedIndex(0);
        }
        selectorFechaHora.setValor(LocalDateTime.now());
        cmbEstado.setSelectedItem("programado");
        txtNotas.setText("");
        filtroDia = null;
        cargarTurnos();
    }

    /** true si [inicio, inicio+duracion) se superpone con otro turno activo del mismo psicólogo. */
    private boolean hayTurnoSolapado(int psicologoId, Timestamp inicio, int duracionMinutos, Integer excluirId) {
        String sql = "SELECT COUNT(*) AS total FROM turnos "
            + "WHERE psicologo_id = ? AND estado <> 'cancelado' "
            + (excluirId != null ? "AND id <> ? " : "")
            + "AND fecha_hora < ? "
            + "AND DATE_ADD(fecha_hora, INTERVAL duracion_minutos MINUTE) > ?";

        Timestamp fin = new Timestamp(inicio.getTime() + duracionMinutos * 60_000L);

        try (Connection cn = new Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            int indice = 1;
            ps.setInt(indice++, psicologoId);
            if (excluirId != null) {
                ps.setInt(indice++, excluirId);
            }
            ps.setTimestamp(indice++, fin);
            ps.setTimestamp(indice, inicio);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }

        } catch (Exception e) {
            System.out.println("Error verificando solapamiento: " + e.getMessage());
            return false;
        }
    }

    private static class PacienteItem {
        final int id;
        final String nombre;
        final String curso;
        final String ci;

        PacienteItem(int id, String nombre, String curso, String ci) {
            this.id = id;
            this.nombre = nombre;
            this.curso = curso;
            this.ci = ci;
        }

        @Override
        public String toString() {
            String base = (curso != null && !curso.isEmpty()) ? nombre + " — " + curso : nombre;
            return (ci != null && !ci.isEmpty()) ? base + " · CI " + ci : base;
        }
    }

    private static class PsicologoItem {
        final int id;
        final String nombre;

        PsicologoItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    /** Vista de calendario mensual: una tarjeta por día con la cantidad de turnos; clic filtra la lista. */
    private static class PanelCalendario extends JPanel {
        private final Consumer<LocalDate> alSeleccionarDia;
        private YearMonth mesActual = YearMonth.now();
        private JLabel lblMes;
        private JPanel grilla;

        PanelCalendario(Consumer<LocalDate> alSeleccionarDia) {
            this.alSeleccionarDia = alSeleccionarDia;
            setLayout(new BorderLayout(10, 10));
            setBackground(Tema.SUPERFICIE);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

            JPanel cabecera = new JPanel(new BorderLayout());
            cabecera.setBackground(Tema.SUPERFICIE);

            JButton btnAnterior = Tema.botonSecundario("", Icono.ANTERIOR);
            btnAnterior.setToolTipText("Mes anterior");
            btnAnterior.addActionListener(e -> {
                mesActual = mesActual.minusMonths(1);
                refrescar();
            });
            JButton btnSiguiente = Tema.botonSecundario("", Icono.SIGUIENTE);
            btnSiguiente.setToolTipText("Mes siguiente");
            btnSiguiente.addActionListener(e -> {
                mesActual = mesActual.plusMonths(1);
                refrescar();
            });
            JButton btnQuitarFiltro = Tema.botonPrimario("Ver todas las citas");
            btnQuitarFiltro.addActionListener(e -> alSeleccionarDia.accept(null));

            lblMes = new JLabel("");
            lblMes.setFont(Tema.SUBTITULO);
            lblMes.setForeground(Tema.TEXTO_PRIMARIO);
            lblMes.setHorizontalAlignment(JLabel.CENTER);

            JPanel navegacion = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            navegacion.setBackground(Tema.SUPERFICIE);
            navegacion.add(btnAnterior);
            navegacion.add(lblMes);
            navegacion.add(btnSiguiente);

            cabecera.add(navegacion, BorderLayout.CENTER);
            cabecera.add(btnQuitarFiltro, BorderLayout.EAST);
            add(cabecera, BorderLayout.NORTH);

            grilla = new JPanel(new GridLayout(0, 7, 4, 4));
            grilla.setBackground(Tema.SUPERFICIE);
            add(grilla, BorderLayout.CENTER);

            refrescar();
        }

        void refrescar() {
            lblMes.setText(capitalizar(mesActual.getMonth().getDisplayName(TextStyle.FULL, new Locale("es")))
                + " " + mesActual.getYear());

            YearMonth mes = mesActual;
            Tema.enSegundoPlano(
                this,
                () -> cargarConteoDelMes(mes),
                turnosPorDia -> construirGrilla(mes, turnosPorDia),
                error -> System.out.println("Error cargando conteo del calendario: " + error.getMessage())
            );
        }

        /** Si el usuario ya cambió de mes mientras esto cargaba, se descarta (llegó tarde). */
        private void construirGrilla(YearMonth mes, Map<Integer, Integer> turnosPorDia) {
            if (!mes.equals(mesActual)) {
                return;
            }

            grilla.removeAll();
            String[] diasSemana = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
            for (String d : diasSemana) {
                JLabel lbl = new JLabel(d, JLabel.CENTER);
                lbl.setFont(Tema.BOTON);
                lbl.setForeground(Tema.TEXTO_SECUNDARIO);
                grilla.add(lbl);
            }

            LocalDate primerDia = mes.atDay(1);
            int offset = primerDia.getDayOfWeek().getValue() % 7; // getDayOfWeek: Lun=1..Dom=7 -> alinear Dom=0
            for (int i = 0; i < offset; i++) {
                JPanel vacio = new JPanel();
                vacio.setBackground(Tema.SUPERFICIE);
                grilla.add(vacio);
            }

            int totalDias = mes.lengthOfMonth();
            for (int dia = 1; dia <= totalDias; dia++) {
                LocalDate fecha = mes.atDay(dia);
                int cantidad = turnosPorDia.getOrDefault(dia, 0);
                grilla.add(crearCeldaDia(fecha, cantidad));
            }

            grilla.revalidate();
            grilla.repaint();
        }

        private String capitalizar(String s) {
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        private JPanel crearCeldaDia(LocalDate fecha, int cantidadTurnos) {
            boolean hoy = fecha.equals(LocalDate.now());
            JPanel celda = new JPanel(new BorderLayout());
            celda.setBackground(hoy ? Tema.ACENTO_AZUL.insignia : Tema.SUPERFICIE);
            celda.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            celda.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

            JLabel lblDia = new JLabel(String.valueOf(fecha.getDayOfMonth()));
            lblDia.setFont(Tema.TEXTO_CHICO);
            lblDia.setForeground(Tema.TEXTO_PRIMARIO);
            lblDia.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));
            celda.add(lblDia, BorderLayout.NORTH);

            if (cantidadTurnos > 0) {
                JLabel lblCantidad = new JLabel(cantidadTurnos + (cantidadTurnos == 1 ? " cita" : " citas"));
                lblCantidad.setFont(Tema.TEXTO_ITALICA_CHICA);
                lblCantidad.setForeground(Tema.ACENTO_AZUL.icono);
                lblCantidad.setHorizontalAlignment(JLabel.CENTER);
                celda.add(lblCantidad, BorderLayout.SOUTH);
            }

            celda.setPreferredSize(new java.awt.Dimension(0, 55));
            celda.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    alSeleccionarDia.accept(fecha);
                }
            });

            return celda;
        }

        private Map<Integer, Integer> cargarConteoDelMes(YearMonth mes) {
            Map<Integer, Integer> resultado = new HashMap<>();
            LocalDate desde = mes.atDay(1);
            LocalDate hasta = mes.atEndOfMonth();

            String sql = "SELECT DAY(fecha_hora) AS dia, COUNT(*) AS total FROM turnos "
                + "WHERE fecha_hora >= ? AND fecha_hora < ? ";
            if (Sesion.esPsicologo()) {
                sql += "AND psicologo_id = ? ";
            }
            sql += "GROUP BY dia";

            try (Connection cn = new Conexion().conectar();
                 PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
                if (Sesion.esPsicologo()) {
                    ps.setInt(3, Sesion.getUsuarioId());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resultado.put(rs.getInt("dia"), rs.getInt("total"));
                    }
                }

            } catch (Exception e) {
                System.out.println("Error cargando conteo del calendario: " + e.getMessage());
            }

            return resultado;
        }
    }
}
