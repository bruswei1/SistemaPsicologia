package Vista;

import dao.*;
import modelos.Paciente;
import modelos.HistoriaPsicologica;
import modelos.DocumentoPaciente;
import modelos.Usuario;
import util.Validador;
import util.ControlPermiso;
import util.Auditoria;
import util.Sesion;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ficha completa de un estudiante. Es un panel que se muestra DENTRO de la ventana principal
 * (en {@link EstudiantesPanel}, en lugar de la lista), no una ventana aparte: "Volver a la
 * lista" o Esc regresan a la lista de estudiantes.
 */
public class DetallePaciente extends javax.swing.JPanel {

    private final Runnable alEditar;
    private final Runnable alCerrar;
    private PacienteDAO pacienteDAO;
    private HistoriaPsicologicaDAO historiaDAO;
    private DocumentoPacienteDAO documentoDAO;
    private Paciente pacienteActual;

    // Pestaña "Seguimiento del Estudiante" (historia_psicologica): funciona como Atenciones —
    // cada Guardar crea una entrada fechada nueva, el formulario queda en blanco listo para la
    // próxima, y la tabla de abajo lista las entradas anteriores; hacer clic en una la carga de
    // nuevo en el formulario, mostrada en solo lectura (ver aplicarSoloLecturaSeguimiento) —
    // una entrada ya guardada no se puede volver a editar, solo exportar/imprimir.
    private JTextArea txtSeguimientoAntecedentes;
    private JTextArea txtSeguimientoMotivo;
    private JTextArea txtSeguimientoObservaciones;
    private JTextArea txtSeguimientoDiagnostico;
    private JTextArea txtSeguimientoTratamiento;
    private JComboBox<String> cmbMotivosFrecuentes;
    private JTextField txtCodigoCie10;
    private JComboBox<String> cmbTipoAcoso;
    private JCheckBox chkEsReiterado;
    private JButton btnGuardarSeguimiento;
    private JLabel lblEstadoSeguimiento;
    private JTable tablaSeguimiento;
    private DefaultTableModel modeloTablaSeguimiento;
    private Integer seguimientoIdActual;
    private JTextField txtBuscarSeguimiento;

    // Adjuntos del estudiante (documentos_paciente): archivos en disco bajo adjuntos/<id>/,
    // separados por estudiante; solo metadata en la base de datos.
    private JTable tablaAdjuntos;
    private DefaultTableModel modeloTablaAdjuntos;
    private JButton btnAdjuntar;
    private JButton btnAbrirAdjunto;
    private JButton btnEliminarAdjunto;
    private JLabel lblEstadoAdjunto;

    private JTabbedPane tabs;
    private LineaDeTiempo lineaDeTiempo;
    private JPanel panelPestanaSeguimiento;
    private JPanel panelPestanaAtenciones;
    private JPanel panelPestanaAdjuntos;

    // Pestaña "Atenciones" (sesiones SOAP + cronómetro) — antes Sesiones.
    private JComboBox<TurnoItem> cmbTurnoSesion;
    private JTextArea txtSubjetivo;
    private JTextArea txtObjetivo;
    private JTextArea txtAnalisis;
    private JTextArea txtPlan;
    private JTextArea txtNotasPrivadas;
    private JLabel lblCronometro;
    private JButton btnIniciarCronometro;
    private JButton btnDetenerCronometro;
    private JButton btnGuardarSesion;
    private JLabel lblEstadoSesion;
    private Timer timerSesion;
    private int segundosTranscurridosSesion;
    private Integer duracionMinutosRegistrada;
    private Integer duracionSegundosRegistrada;
    private JTable tablaSesionesEditable;
    private DefaultTableModel modeloTablaSesiones;
    private Integer sesionIdActual;
    private JTextField txtBuscarSesiones;

    /**
     * @param alEditar se llama después de cerrar la ficha con "Editar datos"
     * @param alCerrar vuelve a la lista (quien muestra la ficha decide cómo)
     */
    public DetallePaciente(int pacienteId, Runnable alEditar, Runnable alCerrar) {
        this.alEditar = alEditar;
        this.alCerrar = alCerrar;
        this.pacienteDAO = new PacienteDAO();
        this.historiaDAO = new HistoriaPsicologicaDAO();
        this.documentoDAO = new DocumentoPacienteDAO();

        this.pacienteActual = pacienteDAO.obtenerPorId(pacienteId);

        if (pacienteActual != null) {
            initComponents();
        }
    }

    public Paciente getPaciente() {
        return pacienteActual;
    }

    /**
     * Para salir de la ficha hacia otro lado (p. ej. abrir otro estudiante con Ctrl+K): pregunta
     * si hay algo sin guardar y, si se confirma, frena el cronómetro y sigue con {@code despues}.
     */
    public void salir(Runnable despues) {
        confirmarSiHayBorrador(() -> {
            detenerCronometroSesion(false);
            despues.run();
        });
    }

    /** true mientras corre el cronómetro de una atención (el profesional está atendiendo). */
    public boolean cronometroEnMarcha() {
        return timerSesion != null && timerSesion.isRunning();
    }

    /** true si hay una entrada de seguimiento o una atención escrita y todavía sin guardar. */
    public boolean tieneBorrador() {
        return hayBorradorSeguimiento() || hayBorradorAtencion();
    }

    /** false si el estudiante ya no existe (borrado por otro usuario): quien la abre muestra el aviso. */
    public boolean estudianteEncontrado() {
        return pacienteActual != null;
    }

    /**
     * Vuelve a la lista, frenando antes el cronómetro de una atención en curso. Si hay una
     * atención o una entrada de seguimiento escrita y sin guardar, primero pregunta (antes se
     * perdía en silencio al tocar "Volver a la lista").
     */
    public void cerrar() {
        confirmarSiHayBorrador(() -> {
            detenerCronometroSesion(false);
            alCerrar.run();
        });
    }

    /** true si el formulario de Seguimiento tiene texto nuevo (no una entrada ya guardada abierta en solo lectura). */
    private boolean hayBorradorSeguimiento() {
        return txtSeguimientoAntecedentes != null && seguimientoIdActual == null && !formularioSeguimientoVacio();
    }

    /** true si hay una atención nueva escrita o con el cronómetro andando, todavía sin guardar. */
    private boolean hayBorradorAtencion() {
        if (txtSubjetivo == null || sesionIdActual != null) {
            return false;
        }
        boolean hayTexto = !txtSubjetivo.getText().trim().isEmpty() || !txtObjetivo.getText().trim().isEmpty()
            || !txtAnalisis.getText().trim().isEmpty() || !txtPlan.getText().trim().isEmpty()
            || !txtNotasPrivadas.getText().trim().isEmpty();
        return hayTexto || (timerSesion != null && timerSesion.isRunning());
    }

    /** Corre {@code accion} directamente, o después de confirmar si hay algo sin guardar. */
    private void confirmarSiHayBorrador(Runnable accion) {
        boolean seguimiento = hayBorradorSeguimiento();
        boolean atencion = hayBorradorAtencion();
        if (!seguimiento && !atencion) {
            accion.run();
            return;
        }
        String que = seguimiento && atencion ? "una entrada de seguimiento y una atención"
            : seguimiento ? "una entrada de seguimiento" : "una atención";
        confirmarDescarte("Tenés " + que + " sin guardar. Si salís ahora, se pierde lo escrito.", "Salir sin guardar", accion);
    }

    private void confirmarDescarte(String mensaje, String textoConfirmar, Runnable accion) {
        Tema.confirmar(this, "Hay cambios sin guardar", mensaje, textoConfirmar, "Seguir editando", accion);
    }

    /** "Nueva entrada": si lo escrito todavía no se guardó, pregunta antes de vaciar el formulario. */
    private void nuevaEntradaSeguimiento() {
        if (hayBorradorSeguimiento()) {
            confirmarDescarte("La entrada de seguimiento que estás escribiendo todavía no se guardó.",
                "Descartar y empezar otra", this::limpiarFormularioSeguimiento);
        } else {
            limpiarFormularioSeguimiento();
        }
    }

    private void nuevaAtencion() {
        if (hayBorradorAtencion()) {
            confirmarDescarte("La atención que estás registrando todavía no se guardó.",
                "Descartar y empezar otra", this::limpiarFormularioSesion);
        } else {
            limpiarFormularioSesion();
        }
    }

    private void initComponents() {

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tabs
        tabs = new JTabbedPane();
        JTabbedPane tabbedPane = tabs;
        tabbedPane.setBackground(Tema.SUPERFICIE);
        tabbedPane.setFont(Tema.TEXTO_CHICO);

        // Tab 1: Información Personal
        tabbedPane.addTab("Información Personal", new IconoSwing(Icono.PACIENTES, Tema.PRIMARIO, 16), crearPanelPersonal());

        // Las notas de atención y la historia psicológica son clínicas/confidenciales:
        // no disponibles para el rol "secretaria" (mismo criterio que antes vivía en
        // MenuPrincipal.mostrarSesiones()/mostrarHistoria()).
        boolean esSecretaria = Sesion.esSecretaria();
        if (!esSecretaria) {
            // Tab 2: Seguimiento del Estudiante (historia psicológica completa, editable)
            panelPestanaSeguimiento = crearPanelClinica();
            tabbedPane.addTab("Seguimiento del Estudiante", new IconoSwing(Icono.HISTORIA, Tema.PRIMARIO, 16), panelPestanaSeguimiento);

            // Tab 3: Atenciones (notas SOAP + cronómetro, editable)
            panelPestanaAtenciones = crearPanelSesiones();
            tabbedPane.addTab("Atenciones", new IconoSwing(Icono.SESIONES, Tema.PRIMARIO, 16), panelPestanaAtenciones);

            // Tab 4: Adjuntos (antes escondidos al fondo de "Seguimiento")
            panelPestanaAdjuntos = crearPanelAdjuntos();
            tabbedPane.addTab("Adjuntos", new IconoSwing(Icono.CARPETA, Tema.PRIMARIO, 16), panelPestanaAdjuntos);
            cargarAdjuntos();
        }

        mainPanel.add(crearBarraAcciones(esSecretaria), BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) {
                cargarLineaDeTiempo(); // puede haber cambiado algo en las otras pestañas
            }
        });
        registrarAtajos();

        // Footer
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Tema.FONDO);

        JLabel lblAtajos = new JLabel(esSecretaria ? "Esc vuelve a la lista de estudiantes"
            : "Atajos: Ctrl+S guarda · Ctrl+E exporta · Ctrl+1…4 cambia de pestaña · Esc vuelve a la lista");
        lblAtajos.setFont(Tema.TEXTO_CHICO);
        lblAtajos.setForeground(Tema.TEXTO_SECUNDARIO);
        footerPanel.add(lblAtajos, BorderLayout.WEST);

        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        String nombreCompleto = pacienteActual.getNombre() + " " + pacienteActual.getApellido();
        String subtitulo = "Estudiantes  ›  " + nombreCompleto
            + (pacienteActual.getCurso() != null && !pacienteActual.getCurso().isEmpty() ? "  ·  " + pacienteActual.getCurso() : "");
        setLayout(new BorderLayout());
        add(Tema.panelEncabezado(nombreCompleto, subtitulo), BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Acciones del estudiante siempre a la vista, arriba de las pestañas, sin importar en cuál
     * se esté (antes "Editar" estaba abajo a la derecha y adjuntos/carpetas escondidos en
     * "Seguimiento").
     */
    private JPanel crearBarraAcciones(boolean esSecretaria) {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);

        JButton btnVolver = Tema.botonSecundario("Volver a la lista", Icono.VOLVER);
        btnVolver.addActionListener(e -> cerrar());
        barra.add(btnVolver);
        Tema.atajoEscape(this, btnVolver, this::cerrar);
        barra.add(separadorVertical());

        JButton btnEditar = Tema.botonPrimario("Editar datos", Icono.EDITAR);
        btnEditar.setToolTipText("Abre el formulario con los datos personales del estudiante para modificarlos");
        // Mismo control de borradores que "Volver a la lista": si se cancela, no se abre el editor.
        btnEditar.addActionListener(e -> confirmarSiHayBorrador(() -> {
            detenerCronometroSesion(false);
            alCerrar.run();
            alEditar.run();
        }));
        barra.add(btnEditar);

        if (!esSecretaria) {
            barra.add(separadorVertical());

            JButton btnAdjuntarRapido = Tema.botonSecundario("Adjuntar archivos", Icono.NUEVO);
            btnAdjuntarRapido.setToolTipText("Adjuntar imágenes, PDF o documentos a este estudiante");
            btnAdjuntarRapido.addActionListener(e -> {
                tabs.setSelectedComponent(panelPestanaAdjuntos);
                adjuntarArchivo();
            });
            barra.add(btnAdjuntarRapido);

            JButton btnCarpetaAdjuntos = Tema.botonSecundario("Carpeta de adjuntos", Icono.CARPETA);
            btnCarpetaAdjuntos.setToolTipText("Abre en el Explorador los archivos adjuntos de este estudiante");
            btnCarpetaAdjuntos.addActionListener(e -> abrirCarpetaAdjuntos());
            barra.add(btnCarpetaAdjuntos);

            JButton btnCarpetaReportes = Tema.botonSecundario("Carpeta de reportes", Icono.CARPETA);
            btnCarpetaReportes.setToolTipText("Abre en el Explorador las fichas y notas ya exportadas de este estudiante");
            btnCarpetaReportes.addActionListener(e -> abrirCarpetaReportes());
            barra.add(btnCarpetaReportes);

            Tema.compactarAlAchicar(barra, btnAdjuntarRapido, btnCarpetaAdjuntos, btnCarpetaReportes);
        }
        return barra;
    }

    /**
     * Ctrl+S / Ctrl+E actúan sobre la pestaña visible (Seguimiento o Atenciones), igual que los
     * botones Guardar/Exportar de esa pestaña. Ctrl+1…4 salta de pestaña.
     */
    private void registrarAtajos() {
        Tema.atajo(this, KeyStroke.getKeyStroke("control S"), () -> {
            Component actual = tabs.getSelectedComponent();
            if (actual != null && actual == panelPestanaSeguimiento && btnGuardarSeguimiento.isEnabled()) {
                guardarHistoriaSeguimiento();
            } else if (actual != null && actual == panelPestanaAtenciones && btnGuardarSesion.isEnabled()) {
                guardarSesionAtencion();
            }
        });
        Tema.atajo(this, KeyStroke.getKeyStroke("control E"), () -> {
            Component actual = tabs.getSelectedComponent();
            if (actual != null && actual == panelPestanaSeguimiento) {
                exportarReporteHistoria();
            } else if (actual != null && actual == panelPestanaAtenciones) {
                exportarNotaSesion();
            }
        });
        for (int i = 1; i <= 4; i++) {
            final int indice = i - 1;
            Tema.atajo(this, KeyStroke.getKeyStroke("control " + i), () -> {
                if (indice < tabs.getTabCount()) {
                    tabs.setSelectedIndex(indice);
                }
            });
        }
    }

    private JPanel crearPanelPersonal() {
        // Toma el ancho de su columna (no se desborda por debajo de la línea de tiempo).
        JPanel panel = new PanelAnchoDelScroll();
        panel.setBackground(Tema.FONDO);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Sección: Datos Básicos
        String etiquetaConsentimiento;
        if (pacienteActual == null) {
            etiquetaConsentimiento = "N/A";
        } else if (pacienteActual.isConsentimientoTutor()) {
            etiquetaConsentimiento = pacienteActual.getConsentimientoFecha() != null
                ? "Sí (registrado el " + pacienteActual.getConsentimientoFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")"
                : "Sí";
        } else {
            etiquetaConsentimiento = "No registrado";
        }

        panel.add(crearSeccion("DATOS BÁSICOS",
            new String[]{"Nombre Completo", "Curso", "CI", "Teléfono", "Género", "Nombre padre/madre/tutor", "CI padre/madre/tutor", "Consentimiento del tutor"},
            new String[]{
                pacienteActual != null ? pacienteActual.getNombre() + " " + pacienteActual.getApellido() : "N/A",
                pacienteActual != null ? (pacienteActual.getCurso() != null ? pacienteActual.getCurso() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getCi() != null ? pacienteActual.getCi() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getTelefono() != null ? pacienteActual.getTelefono() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getGenero() != null ? pacienteActual.getGenero() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getNombreTutor() != null ? pacienteActual.getNombreTutor() : "No especificado") : "N/A",
                pacienteActual != null ? (pacienteActual.getCiTutor() != null ? pacienteActual.getCiTutor() : "No especificado") : "N/A",
                etiquetaConsentimiento
            }
        ));

        panel.add(Box.createVerticalStrut(10));

        JLabel lblVerSeguimiento = new JLabel(
            "<html>El motivo de la atención, antecedentes y anamnesis se registran en la pestaña \"Seguimiento del Estudiante\".</html>");
        lblVerSeguimiento.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblVerSeguimiento.setForeground(Tema.TEXTO_SECUNDARIO);
        lblVerSeguimiento.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblVerSeguimiento);

        panel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBackground(Tema.FONDO);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(Tema.FONDO);

        // Datos a la izquierda, línea de tiempo del caso a la derecha.
        lineaDeTiempo = new LineaDeTiempo(this::abrirEventoDeLineaDeTiempo);
        JPanel tarjetaLinea = Tema.panelTarjeta(Sesion.esSecretaria() ? "Citas del estudiante" : "Línea de tiempo del caso");
        tarjetaLinea.add(lineaDeTiempo, BorderLayout.CENTER);
        JPanel envLinea = new JPanel(new BorderLayout());
        envLinea.setBackground(Tema.FONDO);
        envLinea.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 15));
        envLinea.add(tarjetaLinea, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridLayout(1, 2, 0, 0));
        wrapper.setBackground(Tema.FONDO);
        wrapper.add(scrollPane);
        wrapper.add(envLinea);
        cargarLineaDeTiempo();
        return wrapper;
    }

    /** Recarga la línea de tiempo (al abrir la ficha y cada vez que se vuelve a "Información Personal"). */
    /** Panel que dentro de un JScrollPane toma el ancho visible (solo scroll vertical). */
    private static class PanelAnchoDelScroll extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private void cargarLineaDeTiempo() {
        if (pacienteActual == null || lineaDeTiempo == null) {
            return;
        }
        int pacienteId = pacienteActual.getId();
        boolean incluirClinico = !Sesion.esSecretaria();
        Tema.enSegundoPlano(
            this,
            () -> LineaDeTiempo.cargar(pacienteId, incluirClinico),
            lineaDeTiempo::setEventos,
            error -> lineaDeTiempo.mostrarMensaje("No se pudo cargar la línea de tiempo: " + error.getMessage())
        );
    }

    /** Clic en un evento de la línea de tiempo: lleva a la pestaña correspondiente con ese registro abierto. */
    private void abrirEventoDeLineaDeTiempo(LineaDeTiempo.Evento evento) {
        switch (evento.tipo) {
            case SEGUIMIENTO:
                tabs.setSelectedComponent(panelPestanaSeguimiento);
                seleccionarOCargar(tablaSeguimiento, modeloTablaSeguimiento, evento.id, () -> {
                    if (hayBorradorSeguimiento()) {
                        confirmarDescarte("La entrada de seguimiento que estás escribiendo todavía no se guardó. "
                            + "Si abrís otra, se pierde lo escrito.", "Descartar y abrir", () -> {
                                limpiarFormularioSeguimiento();
                                cargarSeguimiento(evento.id);
                            });
                    } else {
                        cargarSeguimiento(evento.id);
                    }
                });
                break;
            case ATENCION:
                tabs.setSelectedComponent(panelPestanaAtenciones);
                seleccionarOCargar(tablaSesionesEditable, modeloTablaSesiones, evento.id, () -> {
                    if (hayBorradorAtencion()) {
                        confirmarDescarte("La atención que estás registrando todavía no se guardó. "
                            + "Si abrís otra, se pierde lo escrito.", "Descartar y abrir", () -> {
                                limpiarFormularioSesion();
                                cargarSesion(evento.id);
                            });
                    } else {
                        cargarSesion(evento.id);
                    }
                });
                break;
            case ADJUNTO:
                tabs.setSelectedComponent(panelPestanaAdjuntos);
                seleccionarOCargar(tablaAdjuntos, modeloTablaAdjuntos, evento.id, () -> { });
                break;
            default:
                break;
        }
    }

    /**
     * Selecciona en la tabla la fila con ese id (la selección dispara la carga habitual, con su
     * control de borradores). Si la fila no está visible — p. ej. la tabla tiene un filtro de
     * búsqueda escrito —, carga el registro directamente.
     */
    private void seleccionarOCargar(JTable tabla, DefaultTableModel modelo, int id, Runnable cargarDirecto) {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (Integer.valueOf(id).equals(modelo.getValueAt(i, 0))) {
                int fila = tabla.convertRowIndexToView(i);
                if (fila >= 0) {
                    tabla.setRowSelectionInterval(fila, fila);
                    tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
                    return;
                }
            }
        }
        cargarDirecto.run();
    }

    // ==================== Seguimiento del Estudiante (historia_psicologica) ====================
    // Mismo patrón que Atenciones: cada Guardar crea una entrada fechada nueva, el formulario se
    // vacía después, y la tabla de abajo permite reabrir una entrada anterior en solo lectura
    // (no se puede editar una vez guardada, solo exportar/imprimir).

    private JPanel crearPanelClinica() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtSeguimientoAntecedentes = new JTextArea(4, 20);
        txtSeguimientoMotivo = new JTextArea(4, 20);
        txtSeguimientoObservaciones = new JTextArea(4, 20);
        txtSeguimientoDiagnostico = new JTextArea(4, 20);
        txtSeguimientoTratamiento = new JTextArea(4, 20);

        JPanel panelCampos = new JPanel(new GridLayout(3, 2, 6, 6));
        panelCampos.add(campoSoap("Antecedentes", txtSeguimientoAntecedentes));
        panelCampos.add(campoMotivoConPresets(txtSeguimientoMotivo));
        panelCampos.add(campoSoap("Observaciones generales", txtSeguimientoObservaciones));
        panelCampos.add(campoSoap("Diagnóstico situacional", txtSeguimientoDiagnostico));
        panelCampos.add(campoSoap("Plan de intervención", txtSeguimientoTratamiento));

        btnGuardarSeguimiento = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardarSeguimiento.setToolTipText("Guarda una entrada nueva de seguimiento (Ctrl+S)");
        btnGuardarSeguimiento.addActionListener(e -> guardarHistoriaSeguimiento());

        JButton btnNueva = Tema.botonPrimario("Nueva entrada", Icono.NUEVO);
        btnNueva.addActionListener(e -> nuevaEntradaSeguimiento());

        JButton btnExportar = Tema.botonPrimario("Exportar Reporte", Icono.EXPORTAR);
        btnExportar.setToolTipText("Guarda la ficha en un archivo y te ofrece abrirlo (Ctrl+E)");
        btnExportar.addActionListener(e -> exportarReporteHistoria());

        JButton btnImprimir = Tema.botonSecundario("Imprimir");
        btnImprimir.addActionListener(e -> imprimirFormularioSeguimiento());

        JButton btnHistorialCambios = Tema.botonSecundario("Historial de cambios");
        btnHistorialCambios.addActionListener(e -> verHistorialCambiosSeguimiento());

        lblEstadoSeguimiento = new JLabel(" ");
        lblEstadoSeguimiento.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblEstadoSeguimiento.setForeground(Tema.TEXTO_SECUNDARIO);

        // Separa visualmente las dos acciones "activas" (Guardar/Nueva entrada) de las de
        // referencia sobre una entrada ya guardada (Exportar/Imprimir/Historial), que antes se
        // leían todas con el mismo peso en una sola fila de 5 botones.
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelBotones.setBackground(Tema.SUPERFICIE);
        panelBotones.add(btnGuardarSeguimiento);
        panelBotones.add(btnNueva);
        panelBotones.add(separadorVertical());
        panelBotones.add(btnExportar);
        panelBotones.add(btnImprimir);
        panelBotones.add(btnHistorialCambios);
        panelBotones.add(lblEstadoSeguimiento);

        JPanel panelFormulario = new JPanel(new BorderLayout(6, 6));
        panelFormulario.setBackground(Tema.SUPERFICIE);
        panelFormulario.add(panelCampos, BorderLayout.CENTER);
        panelFormulario.add(campoClasificacion(), BorderLayout.SOUTH);

        modeloTablaSeguimiento = new DefaultTableModel(new Object[]{"ID", "Fecha", "Profesional"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaSeguimiento = Tema.crearTablaConVacio(modeloTablaSeguimiento, "Todavía no hay entradas de seguimiento");
        Tema.estilizarTabla(tablaSeguimiento);
        tablaSeguimiento.getSelectionModel().addListSelectionListener(this::onSeleccionarSeguimiento);
        JScrollPane scrollTabla = new JScrollPane(tablaSeguimiento);
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());

        JPanel panelFiltroSeguimiento = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelFiltroSeguimiento.setOpaque(false);
        panelFiltroSeguimiento.add(new JLabel("Buscar:"));
        txtBuscarSeguimiento = new JTextField(24);
        txtBuscarSeguimiento.setToolTipText("Busca en fecha, profesional, motivo, antecedentes, observaciones, diagnóstico y plan");
        txtBuscarSeguimiento.addActionListener(e -> cargarSeguimientos());
        panelFiltroSeguimiento.add(txtBuscarSeguimiento);
        JButton btnBuscarSeguimiento = Tema.botonSecundario("Buscar", Icono.BUSCAR);
        btnBuscarSeguimiento.addActionListener(e -> cargarSeguimientos());
        panelFiltroSeguimiento.add(btnBuscarSeguimiento);

        JPanel panelTablaConFiltro = new JPanel(new BorderLayout(0, 4));
        panelTablaConFiltro.setOpaque(false);
        panelTablaConFiltro.add(panelFiltroSeguimiento, BorderLayout.NORTH);
        panelTablaConFiltro.add(scrollTabla, BorderLayout.CENTER);

        JPanel tarjetaSeguimiento = Tema.panelTarjeta("Entradas de seguimiento registradas");
        tarjetaSeguimiento.add(panelTablaConFiltro, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new BorderLayout(0, 8));
        panelInferior.setBackground(Tema.FONDO);
        panelInferior.add(tarjetaSeguimiento, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelFormulario, panelInferior);
        split.setResizeWeight(0.6);

        panel.add(split, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);

        cargarSeguimientos();
        aplicarSoloLecturaSeguimiento(false);

        // Si la ventana queda más chica que todo el contenido (formulario + tabla + adjuntos +
        // botones), esto agrega una barra de scroll en vez de dejar algo tapado sin forma de
        // llegar a verlo.
        JScrollPane scrollPanel = new JScrollPane(panel);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder());
        scrollPanel.getVerticalScrollBar().setUnitIncrement(16);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPanel, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Combo de motivos frecuentes para llamar la atención a un estudiante, encima del campo
     * libre de "Motivo de la atención". Elegir uno lo agrega al texto en vez de reemplazarlo,
     * para poder combinarlo con una aclaración propia.
     */
    private JPanel campoMotivoConPresets(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        String[] motivosFrecuentes = new String[util.MotivosAtencion.PRESETS.length + 1];
        motivosFrecuentes[0] = "Seleccionar un motivo frecuente...";
        System.arraycopy(util.MotivosAtencion.PRESETS, 0, motivosFrecuentes, 1, util.MotivosAtencion.PRESETS.length);

        cmbMotivosFrecuentes = new JComboBox<>(motivosFrecuentes);
        // Insertar apenas se elige la opción (sin botón extra) — un botón separado de
        // "Agregar" llevaba a guardar el motivo vacío si el usuario elegía del combo y
        // apretaba Guardar sin acordarse de confirmarlo aparte.
        cmbMotivosFrecuentes.addActionListener(e -> {
            int indice = cmbMotivosFrecuentes.getSelectedIndex();
            if (indice <= 0) {
                return;
            }
            String texto = (String) cmbMotivosFrecuentes.getSelectedItem();
            String actual = area.getText();
            area.setText(actual.trim().isEmpty() ? texto : actual + "\n" + texto);
            cmbMotivosFrecuentes.setSelectedIndex(0);
        });

        JPanel panelPresets = new JPanel(new BorderLayout(4, 0));
        panelPresets.add(cmbMotivosFrecuentes, BorderLayout.CENTER);

        JPanel panelSuperior = new JPanel(new BorderLayout(2, 2));
        panelSuperior.add(new JLabel("Motivo de la atención"), BorderLayout.NORTH);
        panelSuperior.add(panelPresets, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Clasificación opcional de la entrada: código CIE-10 (estándar del MSPBS para salud mental)
     * y, cuando aplica, el tipo de acoso escolar según la Ley 4633/2012 más si es una situación
     * reiterada (umbral legal que distingue "acoso" de un incidente aislado).
     */
    private JPanel campoClasificacion() {
        JPanel contenedor = new JPanel(new BorderLayout(0, 4));
        contenedor.setOpaque(false);

        JLabel lblTitulo = new JLabel("Clasificación (opcional)");
        lblTitulo.setFont(Tema.fuente(Font.BOLD, 13));
        lblTitulo.setForeground(Tema.PRIMARIO);
        contenedor.add(lblTitulo, BorderLayout.NORTH);

        // Fila horizontal de ancho completo (antes vivía apretada en una celda de grilla del
        // mismo tamaño que los textareas grandes de al lado, lo que la hacía ver desbalanceada):
        // acá tiene todo el ancho del formulario para acomodar sus tres controles chicos en línea.
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        panel.setOpaque(false);
        contenedor.add(panel, BorderLayout.CENTER);

        panel.add(new JLabel("Código CIE-10:"));
        txtCodigoCie10 = new JTextField(8);
        txtCodigoCie10.setToolTipText("Opcional — ej. Z55, F91.3");
        panel.add(txtCodigoCie10);

        panel.add(new JLabel("Tipo de acoso:"));
        cmbTipoAcoso = new JComboBox<>(new String[]{
            "No aplica",
            util.TipoAcosoEscolar.etiqueta(util.TipoAcosoEscolar.DIRECTO),
            util.TipoAcosoEscolar.etiqueta(util.TipoAcosoEscolar.INDIRECTO),
            util.TipoAcosoEscolar.etiqueta(util.TipoAcosoEscolar.VERBAL)
        });
        panel.add(cmbTipoAcoso);

        chkEsReiterado = new JCheckBox("¿Es reiterado? (Ley 4633/2012)");
        chkEsReiterado.setToolTipText("El acoso escolar exige que la situación sea reiterada, no un incidente aislado");
        panel.add(chkEsReiterado);

        return contenedor;
    }

    /** Código corto (util.TipoAcosoEscolar) elegido en el combo, o null si es "No aplica". */
    private String tipoAcosoSeleccionadoCodigo() {
        switch (cmbTipoAcoso.getSelectedIndex()) {
            case 1: return util.TipoAcosoEscolar.DIRECTO;
            case 2: return util.TipoAcosoEscolar.INDIRECTO;
            case 3: return util.TipoAcosoEscolar.VERBAL;
            default: return null;
        }
    }

    /** Selecciona en el combo el ítem correspondiente al código guardado (o "No aplica" si es null). */
    private void seleccionarTipoAcoso(String codigo) {
        if (util.TipoAcosoEscolar.DIRECTO.equals(codigo)) {
            cmbTipoAcoso.setSelectedIndex(1);
        } else if (util.TipoAcosoEscolar.INDIRECTO.equals(codigo)) {
            cmbTipoAcoso.setSelectedIndex(2);
        } else if (util.TipoAcosoEscolar.VERBAL.equals(codigo)) {
            cmbTipoAcoso.setSelectedIndex(3);
        } else {
            cmbTipoAcoso.setSelectedIndex(0);
        }
    }

    /**
     * Una vez guardada, una entrada de Seguimiento queda de solo lectura: solo se puede
     * exportar/imprimir, no reeditar (pedido explícito para conservar el registro tal como
     * quedó redactado en su momento).
     */
    private void aplicarSoloLecturaSeguimiento(boolean soloLectura) {
        txtSeguimientoAntecedentes.setEditable(!soloLectura);
        txtSeguimientoMotivo.setEditable(!soloLectura);
        txtSeguimientoObservaciones.setEditable(!soloLectura);
        txtSeguimientoDiagnostico.setEditable(!soloLectura);
        txtSeguimientoTratamiento.setEditable(!soloLectura);
        cmbMotivosFrecuentes.setEnabled(!soloLectura);
        txtCodigoCie10.setEditable(!soloLectura);
        cmbTipoAcoso.setEnabled(!soloLectura);
        chkEsReiterado.setEnabled(!soloLectura);
        btnGuardarSeguimiento.setEnabled(!soloLectura);
        lblEstadoSeguimiento.setText(soloLectura
            ? "Esta entrada ya fue guardada y no se puede editar. Solo podés exportarla o imprimirla."
            : " ");
    }

    /**
     * Pestaña "Adjuntos" — antes era una tarjeta chica al fondo de "Seguimiento del Estudiante",
     * debajo de un divisor, y había que hacer scroll para descubrirla. Se pueden arrastrar
     * archivos desde el Explorador directo a la pestaña.
     */
    private JPanel crearPanelAdjuntos() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        modeloTablaAdjuntos = new DefaultTableModel(new Object[]{"ID", "Archivo", "Subido por", "Fecha"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaAdjuntos = Tema.crearTablaConVacio(modeloTablaAdjuntos,
            "Todavía no hay adjuntos. Arrastrá archivos acá o tocá \"Adjuntar archivos\".");
        Tema.estilizarTabla(tablaAdjuntos);
        tablaAdjuntos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaAdjuntos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaAdjuntos.getColumnModel().getColumn(1).setPreferredWidth(420);
        tablaAdjuntos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt)) {
                    abrirAdjuntoSeleccionado();
                }
            }
        });
        tablaAdjuntos.getSelectionModel().addListSelectionListener(e -> actualizarBotonesAdjuntos());
        tablaAdjuntos.setComponentPopupMenu(crearMenuAdjuntos());
        Tema.seleccionarFilaConClicDerecho(tablaAdjuntos);
        tablaAdjuntos.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "abrirAdjunto");
        tablaAdjuntos.getActionMap().put("abrirAdjunto", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                abrirAdjuntoSeleccionado();
            }
        });
        tablaAdjuntos.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "eliminarAdjunto");
        tablaAdjuntos.getActionMap().put("eliminarAdjunto", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                eliminarAdjuntoSeleccionado();
            }
        });

        JScrollPane scroll = new JScrollPane(tablaAdjuntos);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel panelBotonesAdjuntos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panelBotonesAdjuntos.setOpaque(false);

        btnAdjuntar = Tema.botonPrimario("Adjuntar archivos", Icono.NUEVO);
        btnAdjuntar.setToolTipText("Elegí uno o varios archivos (imágenes, PDF, documentos). También podés arrastrarlos a esta pestaña.");
        btnAdjuntar.addActionListener(e -> adjuntarArchivo());
        panelBotonesAdjuntos.add(btnAdjuntar);

        btnAbrirAdjunto = Tema.botonSecundario("Abrir", Icono.EXPORTAR);
        btnAbrirAdjunto.setToolTipText("Abre el archivo seleccionado (doble clic o Enter)");
        btnAbrirAdjunto.addActionListener(e -> abrirAdjuntoSeleccionado());
        panelBotonesAdjuntos.add(btnAbrirAdjunto);

        btnEliminarAdjunto = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminarAdjunto.setToolTipText("Elimina el archivo seleccionado (Supr)");
        btnEliminarAdjunto.addActionListener(e -> eliminarAdjuntoSeleccionado());
        panelBotonesAdjuntos.add(btnEliminarAdjunto);

        JButton btnCarpeta = Tema.botonSecundario("Abrir carpeta", Icono.CARPETA);
        btnCarpeta.setToolTipText("Abre en el Explorador la carpeta con los adjuntos de este estudiante");
        btnCarpeta.addActionListener(e -> abrirCarpetaAdjuntos());
        panelBotonesAdjuntos.add(btnCarpeta);

        lblEstadoAdjunto = new JLabel(" ");
        lblEstadoAdjunto.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblEstadoAdjunto.setForeground(Tema.TEXTO_SECUNDARIO);
        panelBotonesAdjuntos.add(lblEstadoAdjunto);

        panel.add(panelBotonesAdjuntos, BorderLayout.SOUTH);

        JPanel tarjeta = Tema.panelTarjeta("Archivos adjuntos (imágenes, artículos, documentos del caso)");
        tarjeta.add(panel, BorderLayout.CENTER);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(Tema.FONDO);
        contenedor.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenedor.add(tarjeta, BorderLayout.CENTER);

        // Arrastrar y soltar: sobre la tabla, el área vacía debajo o el resto de la pestaña.
        TransferHandler soltarArchivos = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport soporte) {
                return soporte.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport soporte) {
                if (!canImport(soporte)) {
                    return false;
                }
                try {
                    List<File> archivos = (List<File>) soporte.getTransferable()
                        .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    adjuntarArchivos(archivos.toArray(new File[0]));
                    return true;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DetallePaciente.this, "No se pudieron leer los archivos arrastrados: "
                        + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        };
        tablaAdjuntos.setTransferHandler(soltarArchivos);
        tablaAdjuntos.setFillsViewportHeight(true);
        scroll.setTransferHandler(soltarArchivos);
        contenedor.setTransferHandler(soltarArchivos);
        tarjeta.setTransferHandler(soltarArchivos);

        actualizarBotonesAdjuntos();
        return contenedor;
    }

    private JPopupMenu crearMenuAdjuntos() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem abrir = new JMenuItem("Abrir", new IconoSwing(Icono.EXPORTAR, Tema.PRIMARIO, 14));
        abrir.setFont(abrir.getFont().deriveFont(Font.BOLD));
        abrir.addActionListener(e -> abrirAdjuntoSeleccionado());
        menu.add(abrir);

        JMenuItem mostrar = new JMenuItem("Mostrar en carpeta", new IconoSwing(Icono.CARPETA, Tema.PRIMARIO, 14));
        mostrar.addActionListener(e -> mostrarAdjuntoEnCarpeta());
        menu.add(mostrar);

        menu.addSeparator();
        JMenuItem eliminar = new JMenuItem("Eliminar", new IconoSwing(Icono.ELIMINAR, Tema.PELIGRO, 14));
        eliminar.addActionListener(e -> eliminarAdjuntoSeleccionado());
        menu.add(eliminar);
        return menu;
    }

    private void actualizarBotonesAdjuntos() {
        boolean haySeleccion = tablaAdjuntos.getSelectedRow() >= 0;
        btnAbrirAdjunto.setEnabled(haySeleccion);
        btnEliminarAdjunto.setEnabled(haySeleccion);
    }

    private void abrirCarpetaAdjuntos() {
        if (pacienteActual != null) {
            Tema.abrirCarpeta(this, util.Carpetas.adjuntosDe(pacienteActual.getId()));
        }
    }

    private void abrirCarpetaReportes() {
        if (pacienteActual != null) {
            Tema.abrirCarpeta(this, util.Carpetas.reportesDe(pacienteActual.getId(),
                pacienteActual.getNombre(), pacienteActual.getApellido()));
        }
    }

    private void mostrarAdjuntoEnCarpeta() {
        Integer id = adjuntoSeleccionadoId();
        if (id == null) {
            return;
        }
        DocumentoPaciente doc = documentoDAO.obtenerPorId(id);
        if (doc == null) {
            return;
        }
        try {
            util.Carpetas.mostrarEnCarpeta(util.Carpetas.resolver(doc.getRutaArchivo()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir la carpeta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Muestra la cantidad de adjuntos en el título de la pestaña, p. ej. "Adjuntos (3)". */
    private void actualizarTituloPestanaAdjuntos() {
        if (tabs == null) {
            return;
        }
        int indice = tabs.indexOfComponent(panelPestanaAdjuntos);
        if (indice >= 0) {
            int n = modeloTablaAdjuntos.getRowCount();
            tabs.setTitleAt(indice, n > 0 ? "Adjuntos (" + n + ")" : "Adjuntos");
        }
    }

    private void cargarAdjuntos() {
        if (pacienteActual == null) {
            modeloTablaAdjuntos.setRowCount(0);
            return;
        }
        int pacienteId = pacienteActual.getId();

        Tema.enSegundoPlano(
            this,
            () -> cargarFilasAdjuntos(pacienteId),
            filas -> {
                modeloTablaAdjuntos.setRowCount(0);
                for (Object[] fila : filas) {
                    modeloTablaAdjuntos.addRow(fila);
                }
                actualizarTituloPestanaAdjuntos();
                actualizarBotonesAdjuntos();
            },
            error -> System.out.println("Error cargando adjuntos: " + error.getMessage())
        );
    }

    /**
     * Corre en el hilo de fondo de {@link Tema#enSegundoPlano}. Trae todos los usuarios en una
     * sola consulta (antes era una consulta por adjunto — un N+1 al pintar la tabla).
     */
    private List<Object[]> cargarFilasAdjuntos(int pacienteId) {
        java.util.Map<Integer, String> nombresPorId = new java.util.HashMap<>();
        for (Usuario u : new UsuarioDAO().obtenerTodos()) {
            nombresPorId.put(u.getId(), u.getNombre());
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Object[]> filas = new java.util.ArrayList<>();
        for (DocumentoPaciente doc : documentoDAO.obtenerPorPaciente(pacienteId)) {
            String nombreUsuario = doc.getSubidoPor() != null ? nombresPorId.get(doc.getSubidoPor()) : null;
            filas.add(new Object[]{
                doc.getId(),
                doc.getNombreArchivo(),
                nombreUsuario != null ? nombreUsuario : "N/A",
                doc.getSubidoEn() != null ? doc.getSubidoEn().format(formato) : ""
            });
        }
        return filas;
    }

    private Integer adjuntoSeleccionadoId() {
        int fila = tablaAdjuntos.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        return (int) modeloTablaAdjuntos.getValueAt(tablaAdjuntos.convertRowIndexToModel(fila), 0);
    }

    private void adjuntarArchivo() {
        if (pacienteActual == null) {
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccionar archivos para adjuntar al estudiante");
        selector.setMultiSelectionEnabled(true);
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        adjuntarArchivos(selector.getSelectedFiles());
    }

    /** Copia los archivos (elegidos con el selector o arrastrados a la pestaña) en segundo plano. */
    private void adjuntarArchivos(File[] origenes) {
        if (pacienteActual == null || origenes == null || origenes.length == 0) {
            return;
        }
        if (!btnAdjuntar.isEnabled()) {
            JOptionPane.showMessageDialog(this, "Esperá a que termine de adjuntar los archivos anteriores",
                "Adjuntando", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (File f : origenes) {
            if (f.isDirectory()) {
                JOptionPane.showMessageDialog(this, "\"" + f.getName() + "\" es una carpeta. Arrastrá los archivos que contiene, no la carpeta.",
                    "No se puede adjuntar una carpeta", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        int pacienteId = pacienteActual.getId();
        Integer usuarioId = Sesion.getUsuarioId();

        // La copia (y, si el origen es un archivo remoto — OneDrive, "Enlace a tu teléfono"/
        // CrossDevice, etc. — la descarga previa que Windows hace de forma transparente) puede
        // tardar varios segundos o fallar por timeout; correrla en el hilo de UI congelaba toda
        // la ventana. Con SwingWorker corre en segundo plano y la ventana sigue respondiendo.
        btnAdjuntar.setEnabled(false);
        lblEstadoAdjunto.setText(origenes.length == 1
            ? "Adjuntando \"" + origenes[0].getName() + "\"... puede tardar si el archivo está en la nube o en el teléfono"
            : "Adjuntando " + origenes.length + " archivos... puede tardar si están en la nube o en el teléfono");

        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            private final List<String> errores = new java.util.ArrayList<>();

            @Override
            protected Integer doInBackground() {
                int adjuntados = 0;
                for (File origen : origenes) {
                    String error = adjuntarUno(origen);
                    if (error == null) {
                        adjuntados++;
                    } else {
                        errores.add(origen.getName() + ": " + error);
                    }
                }
                return adjuntados;
            }

            /** Copia un archivo a adjuntos/&lt;id&gt;/ y lo registra; devuelve el error, o null si salió bien. */
            private String adjuntarUno(File origen) {
                File destino = null;
                try {
                    String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    destino = util.Carpetas.archivoUnico(util.Carpetas.adjuntosDe(pacienteId),
                        timestamp + "_" + util.Carpetas.nombreSeguro(origen.getName()));
                    Files.copy(origen.toPath(), destino.toPath());

                    String nombre = origen.getName();
                    int puntoExt = nombre.lastIndexOf('.');
                    String extension = puntoExt >= 0 && puntoExt < nombre.length() - 1 ? nombre.substring(puntoExt + 1) : null;

                    DocumentoPaciente doc = new DocumentoPaciente();
                    doc.setPacienteId(pacienteId);
                    doc.setNombreArchivo(nombre);
                    // Relativa a la carpeta de datos: si se lanza la app desde otro lugar, o se
                    // mueve la carpeta entera a otra PC, el adjunto se sigue encontrando.
                    doc.setRutaArchivo(util.Carpetas.rutaRelativa(destino));
                    doc.setTipo(extension);
                    doc.setSubidoPor(usuarioId);

                    int nuevoId = documentoDAO.crear(doc);
                    if (nuevoId <= 0) {
                        destino.delete();
                        return "no se pudo registrar el adjunto en la base de datos";
                    }
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "ADJUNTAR_DOCUMENTO", "documentos_paciente", nuevoId, nombre);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                    return null;
                } catch (Exception e) {
                    if (destino != null && destino.exists()) {
                        destino.delete();
                    }
                    String mensaje = e.getMessage() != null ? e.getMessage() : e.toString();
                    String minusculas = mensaje.toLowerCase();
                    boolean archivoRemoto = minusculas.contains("nube") || minusculas.contains("cloud")
                        || minusculas.contains("tiempo de espera") || minusculas.contains("timeout");
                    return archivoRemoto
                        ? "está en un almacenamiento remoto (nube o \"Enlace a tu teléfono\") y tardó demasiado en "
                            + "descargarse. Abrilo una vez desde el Explorador de Windows para que quede guardado localmente, "
                            + "o elegí un archivo que ya esté en esta PC."
                        : mensaje;
                }
            }

            @Override
            protected void done() {
                btnAdjuntar.setEnabled(true);
                lblEstadoAdjunto.setText(" ");
                int adjuntados;
                try {
                    adjuntados = get();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DetallePaciente.this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (adjuntados > 0) {
                    cargarAdjuntos();
                    Tema.mostrarNotificacion(DetallePaciente.this, adjuntados == 1
                        ? "Archivo adjuntado correctamente"
                        : adjuntados + " archivos adjuntados correctamente");
                }
                if (!errores.isEmpty()) {
                    JOptionPane.showMessageDialog(DetallePaciente.this,
                        (errores.size() == 1 ? "No se pudo adjuntar el archivo:\n\n" : "No se pudieron adjuntar estos archivos:\n\n")
                            + String.join("\n\n", errores),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void abrirAdjuntoSeleccionado() {
        Integer id = adjuntoSeleccionadoId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un archivo adjunto", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DocumentoPaciente doc = documentoDAO.obtenerPorId(id);
        if (doc == null) {
            JOptionPane.showMessageDialog(this, "El adjunto ya no existe", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File archivo = util.Carpetas.resolver(doc.getRutaArchivo());
        if (!archivo.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo \"" + doc.getNombreArchivo() + "\" ya no está en disco.\n\n"
                + "Se esperaba en:\n" + archivo.getAbsolutePath(), "Archivo no encontrado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            util.Carpetas.abrir(archivo);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarAdjuntoSeleccionado() {
        Integer id = adjuntoSeleccionadoId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un archivo adjunto", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(this, "¿Eliminar este archivo adjunto?", "Eliminar adjunto",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                DocumentoPaciente doc = documentoDAO.obtenerPorId(id);
                boolean exito = documentoDAO.eliminar(id);
                if (exito) {
                    if (doc != null) {
                        util.Carpetas.resolver(doc.getRutaArchivo()).delete();
                    }
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "ELIMINAR_DOCUMENTO", "documentos_paciente", id, doc != null ? doc.getNombreArchivo() : null);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return exito;
            },
            exito -> {
                if (exito) {
                    cargarAdjuntos();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar el adjunto", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void cargarSeguimientos() {
        if (pacienteActual == null) {
            modeloTablaSeguimiento.setRowCount(0);
            return;
        }
        int pacienteId = pacienteActual.getId();
        String filtro = txtBuscarSeguimiento != null ? txtBuscarSeguimiento.getText().trim().toLowerCase() : "";

        Tema.enSegundoPlano(
            this,
            () -> cargarFilasSeguimiento(pacienteId, filtro),
            filas -> {
                modeloTablaSeguimiento.setRowCount(0);
                for (Object[] fila : filas) {
                    modeloTablaSeguimiento.addRow(fila);
                }
            },
            error -> System.out.println("Error cargando seguimientos: " + error.getMessage())
        );
    }

    /**
     * Corre en el hilo de fondo de {@link Tema#enSegundoPlano}. Trae todos los usuarios en una
     * sola consulta (antes era una consulta por entrada — un N+1 al pintar la tabla).
     */
    private List<Object[]> cargarFilasSeguimiento(int pacienteId, String filtro) {
        List<HistoriaPsicologica> entradas = historiaDAO.obtenerListaPorPaciente(pacienteId);
        java.util.Map<Integer, String> nombresPorId = new java.util.HashMap<>();
        for (Usuario u : new UsuarioDAO().obtenerTodos()) {
            nombresPorId.put(u.getId(), u.getNombre());
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<Object[]> filas = new java.util.ArrayList<>();
        for (HistoriaPsicologica h : entradas) {
            String nombreProfesional = nombresPorId.getOrDefault(h.getPsicologoId(), "N/A");
            String fechaTexto = h.getFechaCreacion() != null ? h.getFechaCreacion().format(formato) : "";

            if (!filtro.isEmpty() && !coincideFiltroSeguimiento(h, nombreProfesional, fechaTexto, filtro)) {
                continue;
            }

            filas.add(new Object[]{h.getId(), fechaTexto, nombreProfesional});
        }
        return filas;
    }

    private boolean coincideFiltroSeguimiento(HistoriaPsicologica h, String nombreProfesional, String fechaTexto, String filtro) {
        return contieneFiltro(fechaTexto, filtro) || contieneFiltro(nombreProfesional, filtro)
            || contieneFiltro(h.getAntecedentes(), filtro) || contieneFiltro(h.getMotivoConsulta(), filtro)
            || contieneFiltro(h.getObservacionesGenerales(), filtro) || contieneFiltro(h.getDiagnostico(), filtro)
            || contieneFiltro(h.getTratamiento(), filtro) || contieneFiltro(h.getCodigoCie10(), filtro)
            || contieneFiltro(util.TipoAcosoEscolar.etiqueta(h.getTipoAcoso()), filtro);
    }

    private boolean contieneFiltro(String valor, String filtro) {
        return valor != null && valor.toLowerCase().contains(filtro);
    }

    private void onSeleccionarSeguimiento(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }

        int fila = tablaSeguimiento.getSelectedRow();
        if (fila < 0) {
            return;
        }

        int id = (int) modeloTablaSeguimiento.getValueAt(tablaSeguimiento.convertRowIndexToModel(fila), 0);
        if (hayBorradorSeguimiento()) {
            // Se deselecciona hasta confirmar; si confirma, se carga la entrada elegida.
            tablaSeguimiento.clearSelection();
            confirmarDescarte("La entrada de seguimiento que estás escribiendo todavía no se guardó. "
                + "Si abrís otra, se pierde lo escrito.", "Descartar y abrir", () -> {
                    limpiarFormularioSeguimiento();
                    cargarSeguimiento(id);
                });
            return;
        }
        cargarSeguimiento(id);
    }

    private void cargarSeguimiento(int id) {
        Tema.enSegundoPlano(
            this,
            () -> historiaDAO.obtenerPorId(id),
            h -> {
                if (h == null) {
                    return;
                }
                seguimientoIdActual = id;
                txtSeguimientoAntecedentes.setText(valorOVacio(h.getAntecedentes()));
                txtSeguimientoMotivo.setText(valorOVacio(h.getMotivoConsulta()));
                txtSeguimientoObservaciones.setText(valorOVacio(h.getObservacionesGenerales()));
                txtSeguimientoDiagnostico.setText(valorOVacio(h.getDiagnostico()));
                txtSeguimientoTratamiento.setText(valorOVacio(h.getTratamiento()));
                txtCodigoCie10.setText(valorOVacio(h.getCodigoCie10()));
                seleccionarTipoAcoso(h.getTipoAcoso());
                chkEsReiterado.setSelected(h.isEsReiterado());
                aplicarSoloLecturaSeguimiento(true);

                Tema.enSegundoPlano(
                    this,
                    () -> {
                        try (Connection cn = new conexion.Conexion().conectar()) {
                            Auditoria.registrar(cn, "VER_HISTORIA", "historia_psicologica", id, null);
                        }
                        return null;
                    },
                    resultado -> { },
                    error -> System.out.println("Error registrando auditoría: " + error.getMessage())
                );
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void guardarHistoriaSeguimiento() {
        if (pacienteActual == null) {
            return;
        }

        Integer idActual = seguimientoIdActual;
        int pacienteId = pacienteActual.getId();
        String antecedentes = txtSeguimientoAntecedentes.getText();
        String motivo = txtSeguimientoMotivo.getText();
        String observaciones = txtSeguimientoObservaciones.getText();
        String diagnostico = txtSeguimientoDiagnostico.getText();
        String tratamiento = txtSeguimientoTratamiento.getText();
        String cie10 = txtCodigoCie10.getText();
        String tipoAcoso = tipoAcosoSeleccionadoCodigo();
        boolean reiterado = chkEsReiterado.isSelected();
        Integer psicologoId;
        try {
            psicologoId = Sesion.getUsuarioId();
        } catch (Exception e) {
            psicologoId = null;
        }
        Integer psicologoIdFinal = psicologoId;

        // Deshabilitado mientras dura el guardado: evita crear dos entradas de un doble clic.
        btnGuardarSeguimiento.setEnabled(false);

        Tema.enSegundoPlano(
            this,
            () -> guardarHistoriaSeguimientoEnBaseDeDatos(
                idActual, pacienteId, psicologoIdFinal, antecedentes, motivo, observaciones,
                diagnostico, tratamiento, cie10, tipoAcoso, reiterado),
            resultado -> {
                btnGuardarSeguimiento.setEnabled(true);
                if (resultado.entradaInexistente) {
                    JOptionPane.showMessageDialog(this, "La entrada ya no existe", "Error", JOptionPane.ERROR_MESSAGE);
                    limpiarFormularioSeguimiento();
                    cargarSeguimientos();
                } else if (resultado.exito) {
                    Tema.mostrarNotificacion(this,
                        idActual != null ? "Entrada de seguimiento actualizada" : "Entrada de seguimiento registrada");
                    limpiarFormularioSeguimiento();
                    cargarSeguimientos();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al guardar", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> {
                btnGuardarSeguimiento.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    private static final class ResultadoGuardadoSeguimiento {
        boolean exito;
        boolean entradaInexistente;
    }

    /** Corre en el hilo de fondo de {@link Tema#enSegundoPlano} — sin tocar componentes Swing acá. */
    private ResultadoGuardadoSeguimiento guardarHistoriaSeguimientoEnBaseDeDatos(
            Integer idActual, int pacienteId, Integer psicologoId, String antecedentes, String motivo,
            String observaciones, String diagnostico, String tratamiento, String cie10, String tipoAcoso,
            boolean reiterado) {

        ResultadoGuardadoSeguimiento resultado = new ResultadoGuardadoSeguimiento();
        boolean esEdicion = idActual != null;
        HistoriaPsicologica historia;

        if (esEdicion) {
            historia = historiaDAO.obtenerPorId(idActual);
            if (historia == null) {
                resultado.entradaInexistente = true;
                return resultado;
            }
        } else {
            historia = new HistoriaPsicologica();
            historia.setPacienteId(pacienteId);
            historia.setPsicologoId(psicologoId);
        }

        historia.setAntecedentes(antecedentes);
        historia.setMotivoConsulta(motivo);
        historia.setObservacionesGenerales(observaciones);
        historia.setDiagnostico(diagnostico);
        historia.setTratamiento(tratamiento);
        historia.setCodigoCie10(cie10);
        historia.setTipoAcoso(tipoAcoso);
        historia.setEsReiterado(reiterado);

        if (esEdicion) {
            resultado.exito = historiaDAO.actualizar(historia);
            if (resultado.exito) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "EDITAR_HISTORIA", "historia_psicologica", idActual, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
            }
        } else {
            int nuevoId = historiaDAO.crear(historia);
            resultado.exito = nuevoId > 0;
            if (resultado.exito) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "CREAR_HISTORIA", "historia_psicologica", nuevoId, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
            }
        }

        return resultado;
    }

    /** true si los 5 campos del formulario de Seguimiento están vacíos (nada para exportar/imprimir). */
    private boolean formularioSeguimientoVacio() {
        return txtSeguimientoAntecedentes.getText().trim().isEmpty() && txtSeguimientoMotivo.getText().trim().isEmpty()
            && txtSeguimientoObservaciones.getText().trim().isEmpty() && txtSeguimientoDiagnostico.getText().trim().isEmpty()
            && txtSeguimientoTratamiento.getText().trim().isEmpty();
    }

    /**
     * Arma el texto del reporte a partir de lo que se ve actualmente en el formulario (cargado
     * o recién escrito), no necesariamente lo último guardado en la base — mismo criterio que
     * "Exportar nota" en Atenciones. Lo usan tanto "Exportar Reporte" como "Imprimir".
     */
    private String contenidoReporteSeguimientoActual() {
        HistoriaPsicologica historia = new HistoriaPsicologica();
        historia.setAntecedentes(txtSeguimientoAntecedentes.getText());
        historia.setMotivoConsulta(txtSeguimientoMotivo.getText());
        historia.setObservacionesGenerales(txtSeguimientoObservaciones.getText());
        historia.setDiagnostico(txtSeguimientoDiagnostico.getText());
        historia.setTratamiento(txtSeguimientoTratamiento.getText());
        historia.setCodigoCie10(txtCodigoCie10.getText());
        historia.setTipoAcoso(tipoAcosoSeleccionadoCodigo());
        historia.setEsReiterado(chkEsReiterado.isSelected());
        return util.GeneradorReportes.generarReportePaciente(pacienteActual, historia);
    }

    private void exportarReporteHistoria() {
        if (pacienteActual == null) {
            return;
        }
        if (formularioSeguimientoVacio()) {
            JOptionPane.showMessageDialog(this, "No hay datos cargados para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String contenido = contenidoReporteSeguimientoActual();
        exportarArchivoEstudiante("Ficha", contenido, "Reporte exportado", "EXPORTAR_FICHA", "pacientes", pacienteActual.getId());
    }

    /**
     * Guarda un reporte en reportes/Estudiantes/&lt;estudiante&gt;/, lo registra en auditoría
     * (exportar saca datos confidenciales del sistema: tiene que quedar quién y cuándo) y ofrece
     * abrirlo. Compartido por "Exportar Reporte" y "Exportar nota".
     */
    private void exportarArchivoEstudiante(String prefijo, String contenido, String tituloDialogo,
            String accionAuditoria, String entidad, Integer entidadId) {
        Paciente paciente = pacienteActual;
        Tema.enSegundoPlano(
            this,
            () -> {
                File carpeta = util.Carpetas.reportesDe(paciente.getId(), paciente.getNombre(), paciente.getApellido());
                File archivo = util.Carpetas.archivoUnico(carpeta,
                    util.GeneradorReportes.nombreArchivoEstudiante(prefijo, paciente, "txt"));
                util.GeneradorReportes.guardarTexto(contenido, archivo, false);
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, accionAuditoria, entidad, entidadId, archivo.getName());
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                return archivo;
            },
            archivo -> Tema.mostrarArchivoGuardado(this, tituloDialogo, archivo),
            error -> JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    /** Muestra una vista previa del formulario de Seguimiento antes de mandarlo a la impresora. */
    private void imprimirFormularioSeguimiento() {
        if (pacienteActual == null) {
            return;
        }
        if (formularioSeguimientoVacio()) {
            JOptionPane.showMessageDialog(this, "No hay datos cargados para imprimir", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String titulo = pacienteActual.getNombre() + " " + pacienteActual.getApellido();
            Tema.mostrarVistaPreviaImpresion(this, titulo, contenidoReporteSeguimientoActual());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al imprimir: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Quién vio/editó esta entrada de Seguimiento y cuándo (tabla `auditoria`, acciones VER_HISTORIA/EDITAR_HISTORIA/CREAR_HISTORIA). */
    private void verHistorialCambiosSeguimiento() {
        if (seguimientoIdActual == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná una entrada guardada para ver su historial", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int idEntrada = seguimientoIdActual;
        Tema.enSegundoPlano(
            this,
            () -> new AuditoriaDAO().obtenerPorEntidad("historia_psicologica", idEntrada),
            registros -> mostrarDialogoHistorialSeguimiento(idEntrada, registros),
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void mostrarDialogoHistorialSeguimiento(int idEntrada, List<AuditoriaDAO.Registro> registros) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        DefaultTableModel modelo = new DefaultTableModel(new Object[]{"Fecha", "Usuario", "Acción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (AuditoriaDAO.Registro r : registros) {
            modelo.addRow(new Object[]{r.fecha.format(formato), r.usuarioNombre, r.accion});
        }

        JTable tabla = Tema.crearTablaConVacio(modelo, "Todavía no hay registros de auditoría para esta entrada.");
        Tema.estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        Tema.mostrarHoja(this, "Historial de accesos", "Entrada de Seguimiento #" + idEntrada + " · quién la vio o modificó y cuándo",
            scroll, new Dimension(640, 440));
    }

    private void limpiarFormularioSeguimiento() {
        seguimientoIdActual = null;
        if (tablaSeguimiento != null) {
            tablaSeguimiento.clearSelection();
        }
        txtSeguimientoAntecedentes.setText("");
        txtSeguimientoMotivo.setText("");
        txtSeguimientoObservaciones.setText("");
        txtSeguimientoDiagnostico.setText("");
        txtSeguimientoTratamiento.setText("");
        txtCodigoCie10.setText("");
        cmbTipoAcoso.setSelectedIndex(0);
        chkEsReiterado.setSelected(false);
        aplicarSoloLecturaSeguimiento(false);
    }

    // ==================== Atenciones (sesiones SOAP + cronómetro) ====================

    private JPanel crearPanelSesiones() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        cmbTurnoSesion = new JComboBox<>();

        lblCronometro = new JLabel("00:00");
        lblCronometro.setFont(Tema.fuente(Font.BOLD, 18));
        lblCronometro.setForeground(Tema.PRIMARIO);
        btnIniciarCronometro = Tema.botonExito("Iniciar atención");
        btnDetenerCronometro = Tema.botonPeligro("Pausar");
        btnDetenerCronometro.setEnabled(false);
        btnIniciarCronometro.addActionListener(evt -> iniciarCronometroSesion());
        btnDetenerCronometro.addActionListener(evt -> detenerCronometroSesion(true));

        JPanel panelCronometro = new JPanel();
        panelCronometro.setBackground(Tema.SUPERFICIE);
        panelCronometro.add(new JLabel("Cronómetro:"));
        panelCronometro.add(lblCronometro);
        panelCronometro.add(btnIniciarCronometro);
        panelCronometro.add(btnDetenerCronometro);

        JPanel panelTurno = new JPanel(new BorderLayout(4, 4));
        panelTurno.setBackground(Tema.SUPERFICIE);
        panelTurno.add(new JLabel("Cita asociada (opcional): "), BorderLayout.WEST);
        panelTurno.add(cmbTurnoSesion, BorderLayout.CENTER);
        panelTurno.add(panelCronometro, BorderLayout.EAST);

        txtSubjetivo = new JTextArea(4, 20);
        txtObjetivo = new JTextArea(4, 20);
        txtAnalisis = new JTextArea(4, 20);
        txtPlan = new JTextArea(4, 20);
        txtNotasPrivadas = new JTextArea(4, 20);

        JPanel panelSoap = new JPanel(new GridLayout(2, 2, 6, 6));
        panelSoap.add(campoSoap("Relato del estudiante (Subjetivo)", txtSubjetivo));
        panelSoap.add(campoSoap("Observación profesional (Objetivo)", txtObjetivo));
        panelSoap.add(campoSoap("Análisis de la situación", txtAnalisis));
        panelSoap.add(campoSoap("Acuerdos y recomendaciones (Plan)", txtPlan));

        JPanel panelPrivado = campoSoap("Seguimiento y notas privadas (uso interno, no forman parte del informe)", txtNotasPrivadas);

        btnGuardarSesion = Tema.botonExito("Guardar atención", Icono.GUARDAR);
        btnGuardarSesion.setToolTipText("Guarda la atención (Ctrl+S)");
        btnGuardarSesion.addActionListener(evt -> guardarSesionAtencion());

        JButton btnNueva = Tema.botonPrimario("Nueva atención", Icono.NUEVO);
        btnNueva.addActionListener(evt -> nuevaAtencion());

        JButton btnExportar = Tema.botonPrimario("Exportar nota", Icono.EXPORTAR);
        btnExportar.setToolTipText("Guarda la nota en un archivo y te ofrece abrirla (Ctrl+E)");
        btnExportar.addActionListener(evt -> exportarNotaSesion());

        lblEstadoSesion = new JLabel(" ");
        lblEstadoSesion.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblEstadoSesion.setForeground(Tema.TEXTO_SECUNDARIO);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelBotones.setBackground(Tema.SUPERFICIE);
        panelBotones.add(btnGuardarSesion);
        panelBotones.add(btnNueva);
        panelBotones.add(separadorVertical());
        panelBotones.add(btnExportar);
        panelBotones.add(lblEstadoSesion);

        JPanel panelFormulario = new JPanel(new BorderLayout(6, 6));
        panelFormulario.setBackground(Tema.SUPERFICIE);
        panelFormulario.add(panelTurno, BorderLayout.NORTH);
        panelFormulario.add(panelSoap, BorderLayout.CENTER);
        panelFormulario.add(panelPrivado, BorderLayout.SOUTH);

        modeloTablaSesiones = new DefaultTableModel(new Object[]{"ID", "Fecha", "Duración"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaSesionesEditable = Tema.crearTablaConVacio(modeloTablaSesiones, "Todavía no hay atenciones registradas");
        Tema.estilizarTabla(tablaSesionesEditable);
        tablaSesionesEditable.getSelectionModel().addListSelectionListener(this::onSeleccionarSesion);
        JScrollPane scrollTabla = new JScrollPane(tablaSesionesEditable);
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());

        JPanel panelFiltroSesiones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelFiltroSesiones.setOpaque(false);
        panelFiltroSesiones.add(new JLabel("Buscar:"));
        txtBuscarSesiones = new JTextField(24);
        txtBuscarSesiones.setToolTipText("Busca en fecha, duración, relato, observación, análisis, plan y notas privadas");
        txtBuscarSesiones.addActionListener(e -> cargarSesiones());
        panelFiltroSesiones.add(txtBuscarSesiones);
        JButton btnBuscarSesiones = Tema.botonSecundario("Buscar", Icono.BUSCAR);
        btnBuscarSesiones.addActionListener(e -> cargarSesiones());
        panelFiltroSesiones.add(btnBuscarSesiones);

        JPanel panelTablaSesionesConFiltro = new JPanel(new BorderLayout(0, 4));
        panelTablaSesionesConFiltro.setOpaque(false);
        panelTablaSesionesConFiltro.add(panelFiltroSesiones, BorderLayout.NORTH);
        panelTablaSesionesConFiltro.add(scrollTabla, BorderLayout.CENTER);

        JPanel tarjetaSesiones = Tema.panelTarjeta("Atenciones registradas");
        tarjetaSesiones.add(panelTablaSesionesConFiltro, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelFormulario, tarjetaSesiones);
        split.setResizeWeight(0.72);

        panel.add(split, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);

        cargarTurnosDelPaciente();
        cargarSesiones();
        aplicarSoloLecturaSesion(false);

        // Mismo fallback que Seguimiento: si la ventana queda más chica que el contenido, se
        // puede scrollear en vez de perder acceso a parte del formulario o la tabla.
        JScrollPane scrollPanel = new JScrollPane(panel);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder());
        scrollPanel.getVerticalScrollBar().setUnitIncrement(16);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPanel, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Una vez guardada, una atención queda de solo lectura: solo se puede exportar, no
     * reeditar (pedido explícito para conservar la nota SOAP tal como quedó redactada).
     */
    private void aplicarSoloLecturaSesion(boolean soloLectura) {
        txtSubjetivo.setEditable(!soloLectura);
        txtObjetivo.setEditable(!soloLectura);
        txtAnalisis.setEditable(!soloLectura);
        txtPlan.setEditable(!soloLectura);
        txtNotasPrivadas.setEditable(!soloLectura);
        cmbTurnoSesion.setEnabled(!soloLectura);
        btnGuardarSesion.setEnabled(!soloLectura);
        btnIniciarCronometro.setEnabled(!soloLectura);
        if (soloLectura) {
            btnDetenerCronometro.setEnabled(false);
        }
        lblEstadoSesion.setText(soloLectura
            ? "Esta atención ya fue guardada y no se puede editar. Solo podés exportarla."
            : " ");
    }

    private JPanel campoSoap(String etiqueta, JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(new JLabel(etiqueta), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    /** Separador vertical fino para agrupar visualmente botones relacionados en una misma fila. */
    private JSeparator separadorVertical() {
        JSeparator separador = new JSeparator(SwingConstants.VERTICAL);
        separador.setPreferredSize(new Dimension(2, 28));
        return separador;
    }

    private void iniciarCronometroSesion() {
        btnIniciarCronometro.setEnabled(false);
        btnDetenerCronometro.setEnabled(true);
        timerSesion = new Timer(1000, evt -> {
            segundosTranscurridosSesion++;
            lblCronometro.setText(String.format("%02d:%02d", segundosTranscurridosSesion / 60, segundosTranscurridosSesion % 60));
        });
        timerSesion.start();
    }

    /** @param actualizarBoton si es false, no toca los botones (uso al cerrar/editar la ventana). */
    private void detenerCronometroSesion(boolean actualizarBoton) {
        if (timerSesion != null) {
            timerSesion.stop();
        }
        if (actualizarBoton) {
            btnIniciarCronometro.setEnabled(true);
            btnDetenerCronometro.setEnabled(false);
        }
        duracionMinutosRegistrada = segundosTranscurridosSesion / 60;
        duracionSegundosRegistrada = segundosTranscurridosSesion % 60;
    }

    private void cargarTurnosDelPaciente() {
        if (pacienteActual == null) {
            cmbTurnoSesion.removeAllItems();
            cmbTurnoSesion.addItem(new TurnoItem(null, "(Sin cita asociada)"));
            return;
        }
        int pacienteId = pacienteActual.getId();

        Tema.enSegundoPlano(
            this,
            () -> cargarTurnoItemsDesdeBaseDeDatos(pacienteId),
            items -> {
                cmbTurnoSesion.removeAllItems();
                cmbTurnoSesion.addItem(new TurnoItem(null, "(Sin cita asociada)"));
                for (TurnoItem item : items) {
                    cmbTurnoSesion.addItem(item);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar citas: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private List<TurnoItem> cargarTurnoItemsDesdeBaseDeDatos(int pacienteId) throws Exception {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String sql = "SELECT id, fecha_hora, estado FROM turnos WHERE paciente_id = ? ";
        if (Sesion.esPsicologo()) {
            sql += "AND psicologo_id = ? ";
        }
        sql += "ORDER BY fecha_hora DESC";

        List<TurnoItem> items = new java.util.ArrayList<>();
        try (Connection cn = new conexion.Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, pacienteId);
            if (Sesion.esPsicologo()) {
                ps.setInt(2, Sesion.getUsuarioId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String etiqueta = rs.getTimestamp("fecha_hora").toLocalDateTime().format(formato)
                        + " (" + Tema.etiquetaEstadoCita(rs.getString("estado")) + ")";
                    items.add(new TurnoItem(rs.getInt("id"), etiqueta));
                }
            }
        }
        return items;
    }

    private void seleccionarTurno(Integer turnoId) {
        for (int i = 0; i < cmbTurnoSesion.getItemCount(); i++) {
            TurnoItem item = cmbTurnoSesion.getItemAt(i);
            if ((turnoId == null && item.id == null) || (turnoId != null && turnoId.equals(item.id))) {
                cmbTurnoSesion.setSelectedIndex(i);
                return;
            }
        }
    }

    private void cargarSesiones() {
        if (pacienteActual == null) {
            modeloTablaSesiones.setRowCount(0);
            return;
        }
        int pacienteId = pacienteActual.getId();
        String filtro = txtBuscarSesiones != null ? txtBuscarSesiones.getText().trim().toLowerCase() : "";

        Tema.enSegundoPlano(
            this,
            () -> cargarFilasSesiones(pacienteId, filtro),
            filas -> {
                modeloTablaSesiones.setRowCount(0);
                for (Object[] fila : filas) {
                    modeloTablaSesiones.addRow(fila);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar sesiones: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private List<Object[]> cargarFilasSesiones(int pacienteId, String filtro) throws Exception {
        String sql = "SELECT * FROM sesiones WHERE paciente_id=? ORDER BY fecha DESC";
        List<Object[]> filas = new java.util.ArrayList<>();

        try (Connection cn = new conexion.Conexion().conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, pacienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object minutos = rs.getObject("duracion_minutos");
                    Object segundos = rs.getObject("duracion_segundos");
                    String duracionTexto = formatoDuracion(minutos != null ? (Integer) minutos : null,
                        segundos != null ? (Integer) segundos : null);
                    Timestamp fecha = rs.getTimestamp("fecha");
                    String fechaTexto = fecha != null
                        ? fecha.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";

                    if (!filtro.isEmpty() && !coincideFiltroSesion(rs, fechaTexto, duracionTexto, filtro)) {
                        continue;
                    }

                    filas.add(new Object[]{rs.getInt("id"), fecha, duracionTexto});
                }
            }
        }
        return filas;
    }

    private void onSeleccionarSesion(ListSelectionEvent evt) {

        if (evt.getValueIsAdjusting()) {
            return;
        }

        int fila = tablaSesionesEditable.getSelectedRow();
        if (fila < 0) {
            return;
        }

        int id = (int) modeloTablaSesiones.getValueAt(tablaSesionesEditable.convertRowIndexToModel(fila), 0);
        if (hayBorradorAtencion()) {
            tablaSesionesEditable.clearSelection();
            confirmarDescarte("La atención que estás registrando todavía no se guardó. "
                + "Si abrís otra, se pierde lo escrito.", "Descartar y abrir", () -> {
                    limpiarFormularioSesion();
                    cargarSesion(id);
                });
            return;
        }
        cargarSesion(id);
    }

    private void cargarSesion(int id) {
        Tema.enSegundoPlano(
            this,
            () -> cargarDatosSesionDesdeBaseDeDatos(id),
            datos -> {
                if (datos == null) {
                    return;
                }

                // Si había un cronómetro corriendo (otra atención sin guardar), frenarlo: si no,
                // sigue sumando segundos en segundo plano y pisa el "(guardado)" que se muestra
                // abajo con un conteo en vivo que no corresponde a esta entrada.
                if (timerSesion != null) {
                    timerSesion.stop();
                }

                sesionIdActual = id;
                txtSubjetivo.setText(valorOVacio(datos.subjetivo));
                txtObjetivo.setText(valorOVacio(datos.objetivo));
                txtAnalisis.setText(valorOVacio(datos.analisis));
                txtPlan.setText(valorOVacio(datos.plan));
                txtNotasPrivadas.setText(valorOVacio(datos.notasPrivadas));
                seleccionarTurno(datos.turnoId);

                duracionMinutosRegistrada = datos.duracionMinutos;
                duracionSegundosRegistrada = datos.duracionSegundos;
                segundosTranscurridosSesion = 0;
                lblCronometro.setText(duracionMinutosRegistrada != null
                    ? formatoDuracion(duracionMinutosRegistrada, duracionSegundosRegistrada) + " (guardado)"
                    : "00:00");

                aplicarSoloLecturaSesion(true);
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar la sesión: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private static final class DatosSesion {
        String subjetivo;
        String objetivo;
        String analisis;
        String plan;
        String notasPrivadas;
        Integer turnoId;
        Integer duracionMinutos;
        Integer duracionSegundos;
    }

    private DatosSesion cargarDatosSesionDesdeBaseDeDatos(int id) throws Exception {
        String sql = "SELECT * FROM sesiones WHERE id=?";

        try (Connection cn = new conexion.Conexion().conectar()) {
            DatosSesion datos = null;

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        datos = new DatosSesion();
                        datos.subjetivo = rs.getString("subjetivo");
                        datos.objetivo = rs.getString("objetivo");
                        datos.analisis = rs.getString("analisis");
                        datos.plan = rs.getString("plan");
                        datos.notasPrivadas = rs.getString("notas_privadas");
                        Object turnoIdRaw = rs.getObject("turno_id");
                        datos.turnoId = turnoIdRaw != null ? (Integer) turnoIdRaw : null;
                        Object duracionMin = rs.getObject("duracion_minutos");
                        Object duracionSeg = rs.getObject("duracion_segundos");
                        datos.duracionMinutos = duracionMin != null ? (Integer) duracionMin : null;
                        datos.duracionSegundos = duracionSeg != null ? (Integer) duracionSeg : null;
                    }
                }
            }

            if (datos != null) {
                Auditoria.registrar(cn, "VER_SESION", "sesiones", id, null);
            }
            return datos;
        }
    }

    private void guardarSesionAtencion() {

        if (timerSesion != null && timerSesion.isRunning()) {
            detenerCronometroSesion(true);
        }

        if (pacienteActual == null) {
            JOptionPane.showMessageDialog(this, "Estudiante no encontrado", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean esEdicion = sesionIdActual != null;
        Integer idActual = sesionIdActual;
        TurnoItem turnoSeleccionado = (TurnoItem) cmbTurnoSesion.getSelectedItem();
        Integer turnoId = turnoSeleccionado != null ? turnoSeleccionado.id : null;
        int pacienteId = pacienteActual.getId();
        Integer psicologoId;
        try {
            psicologoId = Sesion.getUsuarioId();
        } catch (Exception e) {
            psicologoId = null;
        }
        Integer psicologoIdFinal = psicologoId;

        String subjetivo = txtSubjetivo.getText().trim();
        String objetivo = txtObjetivo.getText().trim();
        String analisis = txtAnalisis.getText().trim();
        String plan = txtPlan.getText().trim();
        String notasPrivadas = txtNotasPrivadas.getText().trim();
        Integer duracionMinutos = duracionMinutosRegistrada;
        Integer duracionSegundos = duracionSegundosRegistrada;

        // Deshabilitado mientras dura el guardado: evita crear dos atenciones de un doble clic.
        btnGuardarSesion.setEnabled(false);

        Tema.enSegundoPlano(
            this,
            () -> guardarSesionEnBaseDeDatos(
                esEdicion, idActual, pacienteId, psicologoIdFinal, turnoId, subjetivo, objetivo,
                analisis, plan, notasPrivadas, duracionMinutos, duracionSegundos),
            resultado -> {
                btnGuardarSesion.setEnabled(true);
                Tema.mostrarNotificacion(this, (esEdicion ? "Atención actualizada" : "Atención registrada")
                    + (resultado.turnoCompletado ? " · la cita se marcó como realizada" : ""));
                limpiarFormularioSesion();
                cargarTurnosDelPaciente();
                cargarSesiones();
            },
            error -> {
                btnGuardarSesion.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    private static final class ResultadoGuardadoSesion {
        boolean turnoCompletado;
    }

    /** Corre en el hilo de fondo de {@link Tema#enSegundoPlano} — sin tocar componentes Swing acá. */
    private ResultadoGuardadoSesion guardarSesionEnBaseDeDatos(
            boolean esEdicion, Integer idActual, int pacienteId, Integer psicologoId, Integer turnoId,
            String subjetivo, String objetivo, String analisis, String plan, String notasPrivadas,
            Integer duracionMinutos, Integer duracionSegundos) throws Exception {

        ResultadoGuardadoSesion resultado = new ResultadoGuardadoSesion();

        // fecha explícita con la hora de la PC en el alta (el servidor MySQL corre en UTC).
        String sql = esEdicion
            ? "UPDATE sesiones SET subjetivo=?, objetivo=?, analisis=?, plan=?, notas_privadas=?, duracion_minutos=?, duracion_segundos=?, turno_id=? WHERE id=?"
            : "INSERT INTO sesiones (paciente_id, psicologo_id, subjetivo, objetivo, analisis, plan, notas_privadas, duracion_minutos, duracion_segundos, turno_id, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cn = new conexion.Conexion().conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {

                if (esEdicion) {
                    setTextoONulo(ps, 1, subjetivo);
                    setTextoONulo(ps, 2, objetivo);
                    setTextoONulo(ps, 3, analisis);
                    setTextoONulo(ps, 4, plan);
                    setTextoONulo(ps, 5, notasPrivadas);
                    if (duracionMinutos != null) {
                        ps.setInt(6, duracionMinutos);
                    } else {
                        ps.setNull(6, Types.INTEGER);
                    }
                    if (duracionSegundos != null) {
                        ps.setInt(7, duracionSegundos);
                    } else {
                        ps.setNull(7, Types.INTEGER);
                    }
                    if (turnoId != null) {
                        ps.setInt(8, turnoId);
                    } else {
                        ps.setNull(8, Types.INTEGER);
                    }
                    ps.setInt(9, idActual);
                } else {
                    ps.setInt(1, pacienteId);
                    ps.setInt(2, psicologoId);
                    setTextoONulo(ps, 3, subjetivo);
                    setTextoONulo(ps, 4, objetivo);
                    setTextoONulo(ps, 5, analisis);
                    setTextoONulo(ps, 6, plan);
                    setTextoONulo(ps, 7, notasPrivadas);
                    if (duracionMinutos != null) {
                        ps.setInt(8, duracionMinutos);
                    } else {
                        ps.setNull(8, Types.INTEGER);
                    }
                    if (duracionSegundos != null) {
                        ps.setInt(9, duracionSegundos);
                    } else {
                        ps.setNull(9, Types.INTEGER);
                    }
                    if (turnoId != null) {
                        ps.setInt(10, turnoId);
                    } else {
                        ps.setNull(10, Types.INTEGER);
                    }
                    ps.setTimestamp(11, Timestamp.valueOf(java.time.LocalDateTime.now()));
                }

                ps.executeUpdate();
            }

            if (esEdicion) {
                Auditoria.registrar(cn, "EDITAR_SESION", "sesiones", idActual, null);
            } else {
                try (PreparedStatement ps = cn.prepareStatement("SELECT LAST_INSERT_ID() AS id");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    Auditoria.registrar(cn, "CREAR_SESION", "sesiones", rs.getInt("id"), null);
                }
            }

            // Si la nota queda vinculada a una cita que todavía estaba "programada", asumimos que
            // la atención se llevó a cabo y la pasamos a "completada" (no pisa cancelada/ausente,
            // por si se marcó así a propósito).
            if (turnoId != null) {
                try (PreparedStatement psTurno = cn.prepareStatement(
                        "UPDATE turnos SET estado='completado' WHERE id=? AND estado='programado'")) {
                    psTurno.setInt(1, turnoId);
                    resultado.turnoCompletado = psTurno.executeUpdate() > 0;
                }
                if (resultado.turnoCompletado) {
                    Auditoria.registrar(cn, "COMPLETAR_TURNO", "turnos", turnoId, "Completado automáticamente al guardar la atención");
                }
            }
        }

        return resultado;
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

    private String formatoDuracion(Integer minutos, Integer segundos) {
        if (minutos == null) {
            return "-";
        }
        int seg = segundos != null ? segundos : 0;
        return String.format("%d min %02d seg", minutos, seg);
    }

    private boolean coincideFiltroSesion(ResultSet rs, String fechaTexto, String duracionTexto, String filtro) throws java.sql.SQLException {
        return contieneFiltro(fechaTexto, filtro) || contieneFiltro(duracionTexto, filtro)
            || contieneFiltro(rs.getString("subjetivo"), filtro) || contieneFiltro(rs.getString("objetivo"), filtro)
            || contieneFiltro(rs.getString("analisis"), filtro) || contieneFiltro(rs.getString("plan"), filtro)
            || contieneFiltro(rs.getString("notas_privadas"), filtro);
    }

    private void exportarNotaSesion() {
        if (pacienteActual == null) {
            JOptionPane.showMessageDialog(this, "Estudiante no encontrado", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (txtSubjetivo.getText().trim().isEmpty() && txtObjetivo.getText().trim().isEmpty()
                && txtAnalisis.getText().trim().isEmpty() && txtPlan.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos cargados en la nota para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String contenido = util.GeneradorReportes.generarNotaAtencion(pacienteActual, sesionIdActual != null,
            txtSubjetivo.getText(), txtObjetivo.getText(), txtAnalisis.getText(), txtPlan.getText());
        exportarArchivoEstudiante("NotaAtencion", contenido, "Nota exportada", "EXPORTAR_NOTA",
            sesionIdActual != null ? "sesiones" : "pacientes",
            sesionIdActual != null ? sesionIdActual : pacienteActual.getId());
    }

    private void limpiarFormularioSesion() {
        sesionIdActual = null;
        tablaSesionesEditable.clearSelection();
        if (cmbTurnoSesion.getItemCount() > 0) {
            cmbTurnoSesion.setSelectedIndex(0);
        }
        txtSubjetivo.setText("");
        txtObjetivo.setText("");
        txtAnalisis.setText("");
        txtPlan.setText("");
        txtNotasPrivadas.setText("");
        segundosTranscurridosSesion = 0;
        duracionMinutosRegistrada = null;
        duracionSegundosRegistrada = null;
        lblCronometro.setText("00:00");
        if (timerSesion != null) {
            timerSesion.stop();
        }
        aplicarSoloLecturaSesion(false);
        btnDetenerCronometro.setEnabled(false);
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

    // ==================== Común ====================

    private JPanel crearSeccion(String titulo, String[] etiquetas, String[] valores) {
        // Etiqueta con su ancho natural y el valor ocupando el resto: antes era una grilla mitad y
        // mitad, y en media pantalla los valores largos (el curso) quedaban cortados sin aviso.
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 0, 8, 16);

        for (int i = 0; i < etiquetas.length; i++) {
            gbc.gridy = i;
            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            JLabel lblEtiqueta = new JLabel(etiquetas[i] + ":");
            lblEtiqueta.setFont(Tema.BOTON);
            lblEtiqueta.setForeground(Tema.PRIMARIO);
            panel.add(lblEtiqueta, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JLabel lblValor = new JLabel(valores[i]);
            lblValor.setFont(Tema.TEXTO_CHICO);
            lblValor.setToolTipText(valores[i]);
            lblValor.setMinimumSize(new Dimension(10, lblValor.getPreferredSize().height));
            lblValor.setPreferredSize(new Dimension(10, lblValor.getPreferredSize().height));
            panel.add(lblValor, gbc);
        }

        JPanel tarjeta = Tema.panelTarjeta(titulo);
        tarjeta.add(panel, BorderLayout.CENTER);
        return tarjeta;
    }

    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Ficha del estudiante (prueba aislada)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1150, 850);
            f.setContentPane(new DetallePaciente(1, () -> { }, () -> System.exit(0)));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
