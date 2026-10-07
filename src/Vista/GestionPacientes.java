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
    private final boolean construirLayoutPropio;
    private PacienteDAO pacienteDAO;
    private HistoriaPsicologicaDAO historiaDAO;
    private JTable tablaPacientes;
    private DefaultTableModel modeloTabla;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtCi;
    private JTextField txtTelefono;
    private JTextField txtNombreTutor;
    private JTextField txtCiTutor;
    private JCheckBox chkConsentimientoTutor;
    private JTextField txtBusqueda;
    private JComboBox<String> comboGenero;
    /** Profesional asignado (psicologo_id): lo elige administración / usuario autorizado; un profesional queda fijo en sí mismo. */
    private JComboBox<ItemProfesional> comboProfesional;
    private JComboBox<String> comboFiltroCurso;
    private SelectorCursoSeccion selectorCurso;
    private Integer pacienteIdActual;
    private JButton btnGuardar;
    private JButton btnVerFicha;
    private JButton btnEditar;
    private JButton btnEliminar;
    private JPanel tarjetaTabla;
    private JScrollPane scrollFormulario;

    // Panel lateral "Nuevo / Editar estudiante"
    private JPanel panelLateral;
    private JPanel contenedorLateral;
    private int anchoLateral;
    private boolean animandoCierre;
    private Timer timerPanelLateral;
    private JLabel lblTituloFormulario;
    private JLabel lblSubtituloFormulario;
    private JLabel lblErrorFormulario;
    private JLabel lblCompletitud;
    private JComponent barraCompletitud;
    private float completitud;
    private JButton btnGuardarYOtro;
    /** true si el usuario cambió algo desde que se abrió/limpió el formulario (para avisar antes de descartar). */
    private boolean formularioModificado;
    /** true mientras el código (no el usuario) llena los campos, para no marcar el formulario como modificado. */
    private boolean cargandoFormulario;
    private java.util.function.IntConsumer alAbrirFicha;
    /** Estudiante a resaltar en la lista la próxima vez que se recargue (el recién guardado). */
    private Integer idASeleccionar;
    /** Estudiantes que se ven ahora en la tabla (con búsqueda y filtro de curso aplicados), para "Exportar lista". */
    private List<Paciente> pacientesVisibles = new java.util.ArrayList<>();

    public GestionPacientes(Runnable alVolver) {
        this(alVolver, true);
    }

    /**
     * @param construirLayoutPropio si es false, no arma su propio encabezado ni el botón
     *                              "Volver" — se usa cuando este panel se embebe como pestaña
     *                              dentro de otro contenedor (ver {@link EstudiantesPanel}).
     */
    public GestionPacientes(Runnable alVolver, boolean construirLayoutPropio) {
        this.alVolver = alVolver;
        this.construirLayoutPropio = construirLayoutPropio;
        this.pacienteDAO = new PacienteDAO();
        this.historiaDAO = new HistoriaPsicologicaDAO();
        initComponents();
        cargarPacientes();
    }

    /** Recarga los datos; MenuPrincipal la llama cada vez que se navega a esta pantalla. */
    public void refrescar() {
        cargarPacientes();
        if (!util.Sesion.esPsicologo() && !formularioAbierto()) {
            cargarProfesionales();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        if (construirLayoutPropio) {
            add(Tema.panelEncabezado("Gestión de Estudiantes", "Registrar, editar y consultar estudiantes"),
                BorderLayout.NORTH);
        }

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de búsqueda — dos filas explícitas (no una sola FlowLayout) porque el combo de
        // curso es demasiado ancho para entrar junto al resto: si se envuelve solo dentro de un
        // único FlowLayout, Swing calcula mal el alto preferido del panel (FlowLayout no lo
        // recalcula al ajustar líneas) y la fila que se envuelve termina superpuesta con la
        // sección de abajo en vez de empujarla.
        JPanel panelBusqueda = Tema.panelTarjeta("Buscar Estudiante");
        JPanel contenidoBusqueda = new JPanel();
        contenidoBusqueda.setOpaque(false);
        contenidoBusqueda.setLayout(new BoxLayout(contenidoBusqueda, BoxLayout.Y_AXIS));
        panelBusqueda.add(contenidoBusqueda, BorderLayout.CENTER);

        JPanel filaNombre = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filaNombre.setBackground(Tema.SUPERFICIE);
        filaNombre.add(new JLabel("Buscar:"));
        // Sin botón "Buscar": la lista ya se filtra mientras se escribe (y con eso la fila entra
        // entera aunque el panel lateral esté abierto, en vez de partirse y encimarse).
        txtBusqueda = new JTextField(24);
        txtBusqueda.setBorder(Tema.bordeCampo());
        txtBusqueda.setToolTipText("Nombre, apellido, teléfono o CI — la lista se filtra mientras escribís (Ctrl+F)");
        txtBusqueda.putClientProperty("JTextField.placeholderText", "Nombre, apellido, teléfono o CI");
        txtBusqueda.putClientProperty("JTextField.leadingIcon", new IconoSwing(Icono.BUSCAR, Tema.TEXTO_SECUNDARIO, 14));
        txtBusqueda.putClientProperty("JTextField.showClearButton", true);
        txtBusqueda.addActionListener(e -> buscarPacientes());
        Tema.alEscribir(txtBusqueda, 350, this::buscarPacientes);
        filaNombre.add(txtBusqueda);

        filaNombre.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        contenidoBusqueda.add(filaNombre);

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

        JButton btnLimpiar = Tema.botonSecundario("Limpiar filtros", Icono.LIMPIAR);
        btnLimpiar.setToolTipText("Borra la búsqueda y el filtro de curso");
        btnLimpiar.addActionListener(e -> {
            txtBusqueda.setText("");
            comboFiltroCurso.setSelectedIndex(0);
            cargarPacientes();
        });
        filaCurso.add(btnLimpiar);

        filaCurso.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        contenidoBusqueda.add(filaCurso);

        JPanel columnaLista = new JPanel(new BorderLayout(10, 10));
        columnaLista.setOpaque(false);
        columnaLista.add(panelBusqueda, BorderLayout.NORTH);

        // Tabla de estudiantes
        String[] columnas = {"ID", "Nombre", "Apellido", "CI", "Teléfono", "Género", "Curso", "Consent.", "Alerta"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaPacientes = Tema.crearTablaConVacio(modeloTabla, "No hay estudiantes para mostrar");
        Tema.estilizarTabla(tablaPacientes);
        tablaPacientes.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt)) {
                    verDetallesPaciente();
                }
            }
        });
        tablaPacientes.getSelectionModel().addListSelectionListener(e -> actualizarBotonesSeleccion());
        tablaPacientes.setComponentPopupMenu(crearMenuContextual());
        Tema.seleccionarFilaConClicDerecho(tablaPacientes);
        tablaPacientes.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "verFicha");
        tablaPacientes.getActionMap().put("verFicha", accion(this::verDetallesPaciente));
        tablaPacientes.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "eliminar");
        tablaPacientes.getActionMap().put("eliminar", accion(this::eliminarPaciente));

        JScrollPane scrollPane = new JScrollPane(tablaPacientes);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel contenidoTabla = new JPanel(new BorderLayout(0, 8));
        contenidoTabla.setOpaque(false);
        contenidoTabla.add(crearBarraAccionesTabla(), BorderLayout.NORTH);
        contenidoTabla.add(scrollPane, BorderLayout.CENTER);

        tarjetaTabla = Tema.panelTarjeta("Estudiantes Registrados");
        tarjetaTabla.add(contenidoTabla, BorderLayout.CENTER);

        columnaLista.add(tarjetaTabla, BorderLayout.CENTER);
        contenedorLateral = crearContenedorLateral();
        mainPanel.add(columnaLista, BorderLayout.CENTER);
        mainPanel.add(contenedorLateral, BorderLayout.EAST);

        if (construirLayoutPropio) {
            JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
            footerPanel.setBackground(Tema.FONDO);
            JButton btnVolver = Tema.botonSecundario("Volver", Icono.VOLVER);
            btnVolver.addActionListener(e -> alVolver.run());
            footerPanel.add(btnVolver);
            Tema.atajoEscape(this, btnVolver, () -> {
                if (formularioAbierto()) {
                    cerrarFormulario();
                } else {
                    alVolver.run();
                }
            });
            add(footerPanel, BorderLayout.SOUTH);
        }

        add(mainPanel, BorderLayout.CENTER);

        // Ctrl+F: ir a la búsqueda. Ctrl+N: formulario vacío para un estudiante nuevo.
        Tema.atajo(this, KeyStroke.getKeyStroke("control F"), () -> {
            txtBusqueda.requestFocusInWindow();
            txtBusqueda.selectAll();
        });
        Tema.atajo(this, KeyStroke.getKeyStroke("control N"), this::nuevoEstudiante);

        actualizarBotonesSeleccion();
    }

    /**
     * Acciones sobre la lista, arriba de la tabla (antes Editar/Eliminar estaban al pie, debajo
     * del formulario, y "Ver ficha" solo existía como doble clic). Las que necesitan un
     * estudiante seleccionado se habilitan recién al seleccionar uno, así se entiende por qué no
     * hacen nada.
     */
    private JPanel crearBarraAccionesTabla() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izquierda.setOpaque(false);

        btnVerFicha = Tema.botonPrimario("Ver ficha", Icono.PACIENTES);
        btnVerFicha.setToolTipText("Abre la ficha completa: seguimiento, atenciones y adjuntos (doble clic o Enter)");
        btnVerFicha.addActionListener(e -> verDetallesPaciente());
        izquierda.add(btnVerFicha);

        btnEditar = Tema.botonSecundario("Editar datos", Icono.EDITAR);
        btnEditar.setToolTipText("Abre el panel lateral con los datos del estudiante para modificarlos");
        btnEditar.addActionListener(e -> editarPacienteSeleccionado());
        izquierda.add(btnEditar);

        btnEliminar = Tema.botonPeligro("Eliminar", Icono.ELIMINAR);
        btnEliminar.setToolTipText("Elimina el estudiante seleccionado (Supr)");
        btnEliminar.addActionListener(e -> eliminarPaciente());
        izquierda.add(btnEliminar);

        JButton btnNuevo = Tema.botonExito("Nuevo estudiante", Icono.NUEVO);
        btnNuevo.setToolTipText("Abre el panel lateral para registrar un estudiante nuevo (Ctrl+N)");
        btnNuevo.addActionListener(e -> nuevoEstudiante());
        izquierda.add(btnNuevo);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);

        JButton btnExportarLista = Tema.botonSecundario("Exportar lista (Excel)", Icono.EXPORTAR);
        btnExportarLista.setToolTipText("Guarda en una planilla exactamente los estudiantes que se ven en la tabla (con la búsqueda y el filtro aplicados)");
        btnExportarLista.addActionListener(e -> exportarListaVisible());
        derecha.add(btnExportarLista);

        JButton btnImportar = Tema.botonSecundario("Importar desde Excel", Icono.NUEVO);
        btnImportar.setToolTipText("Cargar muchos estudiantes de una vez desde una planilla (por ejemplo, un curso entero)");
        btnImportar.addActionListener(e -> ImportarEstudiantes.iniciar(this, importados -> {
            recargarLista();
            Tema.mostrarNotificacion(this, importados == 1 ? "Se importó 1 estudiante" : "Se importaron " + importados + " estudiantes");
        }));
        derecha.add(btnImportar);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        // Con el panel lateral abierto o la ventana chica, quedan solo los íconos (no se superponen).
        Tema.compactarAlAchicar(barra, btnVerFicha, btnEditar, btnEliminar, btnExportarLista, btnImportar);
        return barra;
    }

    private JPopupMenu crearMenuContextual() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem verFicha = new JMenuItem("Ver ficha", new IconoSwing(Icono.PACIENTES, Tema.PRIMARIO, 14));
        verFicha.setFont(verFicha.getFont().deriveFont(Font.BOLD));
        verFicha.addActionListener(e -> verDetallesPaciente());
        menu.add(verFicha);

        JMenuItem editar = new JMenuItem("Editar datos", new IconoSwing(Icono.EDITAR, Tema.PRIMARIO, 14));
        editar.addActionListener(e -> editarPacienteSeleccionado());
        menu.add(editar);

        // Adjuntos y reportes del estudiante son material clínico: no para "Usuario autorizado".
        if (!util.Sesion.esSecretaria()) {
            menu.addSeparator();
            JMenuItem carpetaAdjuntos = new JMenuItem("Abrir carpeta de adjuntos", new IconoSwing(Icono.CARPETA, Tema.PRIMARIO, 14));
            carpetaAdjuntos.addActionListener(e -> {
                Integer id = idSeleccionado();
                if (id != null) {
                    Tema.abrirCarpeta(this, util.Carpetas.adjuntosDe(id));
                }
            });
            menu.add(carpetaAdjuntos);

            JMenuItem carpetaReportes = new JMenuItem("Abrir carpeta de reportes", new IconoSwing(Icono.CARPETA, Tema.PRIMARIO, 14));
            carpetaReportes.addActionListener(e -> {
                int fila = tablaPacientes.getSelectedRow();
                if (fila >= 0) {
                    int filaModelo = tablaPacientes.convertRowIndexToModel(fila);
                    Tema.abrirCarpeta(this, util.Carpetas.reportesDe((int) modeloTabla.getValueAt(filaModelo, 0),
                        String.valueOf(modeloTabla.getValueAt(filaModelo, 1)), String.valueOf(modeloTabla.getValueAt(filaModelo, 2))));
                }
            });
            menu.add(carpetaReportes);
        }

        menu.addSeparator();
        JMenuItem eliminar = new JMenuItem("Eliminar", new IconoSwing(Icono.ELIMINAR, Tema.PELIGRO, 14));
        eliminar.addActionListener(e -> eliminarPaciente());
        menu.add(eliminar);
        return menu;
    }

    private static Action accion(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        };
    }

    // ==================== Panel lateral "Nuevo / Editar estudiante" ====================
    // Se desliza desde la derecha al tocar "Nuevo estudiante" o "Editar datos", al lado de la
    // lista (que sigue visible y usable), en vez de un formulario fijo debajo que le quitaba lugar.

    private static final int ANCHO_PANEL_LATERAL = 460;

    /** Contenedor que se anima de 0 a ANCHO_PANEL_LATERAL; el panel adentro tiene ancho fijo (no se deforma al animar). */
    private JPanel crearContenedorLateral() {
        panelLateral = crearPanelFormulario();
        JPanel contenedor = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(anchoLateral, 0);
            }

            @Override
            public void doLayout() {
                panelLateral.setBounds(0, 0, ANCHO_PANEL_LATERAL, getHeight());
            }
        };
        contenedor.setOpaque(false);
        contenedor.add(panelLateral);
        contenedor.setVisible(false);
        return contenedor;
    }

    private JPanel crearPanelFormulario() {
        JPanel lateral = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Sombra suave hacia la izquierda + tarjeta con una franja de color arriba.
                for (int i = 6; i >= 1; i--) {
                    g2.setColor(new Color(20, 20, 30, 4));
                    g2.fillRoundRect(i - 6 + 6, 0, getWidth() - 6, getHeight(), 18, 18);
                }
                g2.setColor(Tema.SUPERFICIE);
                g2.fillRoundRect(6, 0, getWidth() - 6, getHeight(), 18, 18);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(6, 0, getWidth() - 6, getHeight(), 18, 18));
                g2.setPaint(new GradientPaint(0, 0, Tema.SECUNDARIO, getWidth(), 0, Tema.PRIMARIO));
                g2.fillRect(6, 0, getWidth(), 5);
                g2.dispose();
            }
        };
        lateral.setOpaque(false);
        lateral.setBorder(BorderFactory.createEmptyBorder(5, 6, 0, 0));

        // --- Encabezado: título dinámico, subtítulo y X ---
        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);
        encabezado.setBorder(BorderFactory.createEmptyBorder(16, 22, 10, 12));
        lblTituloFormulario = new JLabel("Nuevo estudiante");
        lblTituloFormulario.setFont(Tema.fuente(Font.BOLD, 19));
        lblTituloFormulario.setForeground(Tema.TEXTO_PRIMARIO);
        lblSubtituloFormulario = new JLabel("Completá los datos y tocá Guardar");
        lblSubtituloFormulario.setFont(Tema.TEXTO_CHICO);
        lblSubtituloFormulario.setForeground(Tema.TEXTO_SECUNDARIO);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(lblTituloFormulario);
        textos.add(lblSubtituloFormulario);
        encabezado.add(textos, BorderLayout.CENTER);
        JButton btnX = Tema.botonIcono(Icono.CERRAR, "Cerrar el formulario (Esc)");
        btnX.addActionListener(e -> cerrarFormulario());
        JPanel envX = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        envX.setOpaque(false);
        envX.add(btnX);
        encabezado.add(envX, BorderLayout.EAST);

        barraCompletitud = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                g2.setColor(Tema.BORDE_SUAVE);
                g2.fillRoundRect(0, 0, w, 6, 6, 6);
                g2.setColor(completitud >= 1f ? Tema.EXITO : Tema.PRIMARIO);
                g2.fillRoundRect(0, 0, Math.round(w * completitud), 6, 6, 6);
                g2.dispose();
            }
        };
        barraCompletitud.setPreferredSize(new Dimension(10, 6));
        lblCompletitud = new JLabel(" ");
        lblCompletitud.setFont(Tema.TEXTO_CHICO);
        lblCompletitud.setForeground(Tema.TEXTO_SECUNDARIO);
        JPanel progreso = new JPanel(new BorderLayout(0, 4));
        progreso.setOpaque(false);
        progreso.setBorder(BorderFactory.createEmptyBorder(0, 22, 12, 22));
        progreso.add(barraCompletitud, BorderLayout.NORTH);
        progreso.add(lblCompletitud, BorderLayout.CENTER);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(encabezado, BorderLayout.NORTH);
        norte.add(progreso, BorderLayout.CENTER);
        lateral.add(norte, BorderLayout.NORTH);

        // --- Campos ---
        txtNombre = campoTexto(null);
        txtApellido = campoTexto(null);
        txtCi = campoTexto("Solo números");
        Tema.validarMientrasEscribe(txtCi, java.util.regex.Pattern.compile("[0-9]*"));
        txtTelefono = campoTexto("Números, espacios, \"+\" o \"-\"");
        Tema.validarMientrasEscribe(txtTelefono, java.util.regex.Pattern.compile("[0-9 +\\-]*"));
        comboGenero = new JComboBox<>(new String[]{"Masculino", "Femenino", "Otro"});
        comboProfesional = new JComboBox<>();
        comboProfesional.addActionListener(e -> alModificarFormulario());
        if (util.Sesion.esPsicologo()) {
            // Un profesional solo puede cargar estudiantes a su nombre (si no, dejaría de verlos).
            comboProfesional.addItem(new ItemProfesional(util.Sesion.getUsuarioId(), util.Sesion.getNombre()));
            comboProfesional.setEnabled(false);
            comboProfesional.setToolTipText("Los estudiantes que cargás quedan asignados a vos");
        } else {
            comboProfesional.setToolTipText("Solo el profesional asignado ve a este estudiante en su lista, agenda y búsqueda");
            cargarProfesionales();
        }
        comboGenero.addActionListener(e -> alModificarFormulario());
        txtNombreTutor = campoTexto(null);
        txtCiTutor = campoTexto("Solo números");
        Tema.validarMientrasEscribe(txtCiTutor, java.util.regex.Pattern.compile("[0-9]*"));
        chkConsentimientoTutor = new JCheckBox("<html>El padre, madre o tutor <b>autorizó</b> el seguimiento psicológico del estudiante</html>");
        chkConsentimientoTutor.setOpaque(false);
        chkConsentimientoTutor.addActionListener(e -> alModificarFormulario());
        selectorCurso = new SelectorCursoSeccion();

        JPanel cuerpo = new PanelAnchoDelScroll();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 18));

        cuerpo.add(tituloSeccionFormulario(1, "Datos del estudiante"));
        cuerpo.add(filaDoble(campo("Nombre *", txtNombre), campo("Apellido *", txtApellido)));
        cuerpo.add(filaDoble(campo("CI", txtCi), campo("Teléfono", txtTelefono)));
        cuerpo.add(filaDoble(campo("Género", comboGenero), campo("Profesional asignado", comboProfesional)));

        cuerpo.add(tituloSeccionFormulario(2, "Curso y sección"));
        JPanel envCurso = new JPanel(new BorderLayout());
        envCurso.setOpaque(false);
        envCurso.add(selectorCurso, BorderLayout.WEST);
        cuerpo.add(alinear(envCurso));

        cuerpo.add(tituloSeccionFormulario(3, "Padre, madre o tutor"));
        cuerpo.add(alinear(campo("Nombre completo", txtNombreTutor)));
        cuerpo.add(filaDoble(campo("CI", txtCiTutor), new JPanel() {{ setOpaque(false); }}));
        cuerpo.add(alinear(chkConsentimientoTutor));
        cuerpo.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(cuerpo);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scrollFormulario = scroll;
        lateral.add(scroll, BorderLayout.CENTER);

        // --- Pie fijo: error + botones (siempre visibles, aunque el formulario tenga scroll) ---
        lblErrorFormulario = new JLabel(" ");
        lblErrorFormulario.setFont(Tema.fuente(Font.BOLD, 12));
        lblErrorFormulario.setForeground(Tema.PELIGRO);

        btnGuardar = Tema.botonExito("Guardar", Icono.GUARDAR);
        btnGuardar.setToolTipText("Guarda y cierra el formulario (Ctrl+Enter)");
        btnGuardar.addActionListener(e -> guardarPaciente(false));

        btnGuardarYOtro = Tema.botonSecundario("Guardar y agregar otro", Icono.NUEVO);
        btnGuardarYOtro.setToolTipText("Guarda y deja el formulario vacío para cargar el siguiente estudiante");
        btnGuardarYOtro.addActionListener(e -> guardarPaciente(true));

        JButton btnCancelar = Tema.botonSecundario("Cancelar");
        btnCancelar.setToolTipText("Cierra el formulario sin guardar (Esc)");
        btnCancelar.addActionListener(e -> cerrarFormulario());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnGuardar);
        botones.add(btnGuardarYOtro);
        botones.add(btnCancelar);

        JPanel pie = new JPanel(new BorderLayout(0, 8));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
            BorderFactory.createEmptyBorder(10, 18, 14, 14)));
        pie.add(lblErrorFormulario, BorderLayout.NORTH);
        pie.add(botones, BorderLayout.CENTER);
        lateral.add(pie, BorderLayout.SOUTH);

        // Ctrl+Enter guarda desde cualquier campo del formulario.
        lateral.registerKeyboardAction(e -> guardarPaciente(false),
            KeyStroke.getKeyStroke("control ENTER"), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        actualizarCompletitud();
        return lateral;
    }

    /**
     * Panel que dentro de un JScrollPane toma siempre el ancho visible (solo scroll vertical):
     * sin esto, el ancho preferido de los campos empujaba la columna derecha fuera del panel.
     */
    private static class PanelAnchoDelScroll extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private JTextField campoTexto(String tooltip) {
        JTextField campo = new JTextField(1);
        campo.setBorder(BorderFactory.createCompoundBorder(Tema.bordeCampo(), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        if (tooltip != null) {
            campo.setToolTipText(tooltip);
        }
        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { alModificarFormulario(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { alModificarFormulario(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { alModificarFormulario(); }
        });
        return campo;
    }

    /** Etiqueta arriba del campo (más fácil de leer en un panel angosto que etiqueta a la izquierda). */
    private JPanel campo(String etiqueta, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(Tema.fuente(Font.BOLD, 12));
        lbl.setForeground(Tema.TEXTO_SECUNDARIO);
        p.add(lbl, BorderLayout.NORTH);
        p.add(componente, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return p;
    }

    private JPanel filaDoble(JComponent izquierda, JComponent derecha) {
        JPanel fila = new JPanel(new GridLayout(1, 2, 12, 0));
        fila.setOpaque(false);
        fila.add(izquierda);
        fila.add(derecha);
        return alinear(fila);
    }

    private <T extends JComponent> T alinear(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /** "① Datos del estudiante": número en un círculo de color + título. */
    private JPanel tituloSeccionFormulario(int numero, String texto) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(numero == 1 ? 4 : 14, 0, 10, 0));
        JComponent circulo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Tema.PRIMARIO);
                g2.fillOval(0, 0, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setFont(Tema.fuente(Font.BOLD, 12));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String n = String.valueOf(numero);
                g2.drawString(n, (22 - fm.stringWidth(n)) / 2, (22 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        circulo.setPreferredSize(new Dimension(22, 22));
        JLabel lbl = new JLabel(texto);
        lbl.setFont(Tema.fuente(Font.BOLD, 14));
        lbl.setForeground(Tema.TEXTO_PRIMARIO);
        p.add(circulo);
        p.add(lbl);
        return alinear(p);
    }

    private void alModificarFormulario() {
        if (!cargandoFormulario) {
            formularioModificado = true;
        }
        if (lblErrorFormulario != null && !txtNombre.getText().trim().isEmpty() && !txtApellido.getText().trim().isEmpty()) {
            lblErrorFormulario.setText(" ");
            Tema.marcarError(txtNombre, true);
            Tema.marcarError(txtApellido, true);
        }
        actualizarCompletitud();
    }

    /** Barra "Datos completos: 5 de 7" — muestra de un vistazo cuánto falta cargar. */
    private void actualizarCompletitud() {
        if (barraCompletitud == null || txtNombre == null) {
            return;
        }
        JTextField[] campos = {txtNombre, txtApellido, txtCi, txtTelefono, txtNombreTutor, txtCiTutor};
        int completos = 0;
        for (JTextField c : campos) {
            if (c != null && !c.getText().trim().isEmpty()) {
                completos++;
            }
        }
        if (chkConsentimientoTutor != null && chkConsentimientoTutor.isSelected()) {
            completos++;
        }
        int total = campos.length + 1;
        completitud = completos / (float) total;
        lblCompletitud.setText(completos == total ? "Todos los datos completos"
            : "Datos completos: " + completos + " de " + total + "  ·  obligatorios: nombre y apellido");
        barraCompletitud.repaint();
    }

    private void nuevoEstudiante() {
        if (formularioAbierto() && formularioModificado) {
            Tema.confirmar(this, "¿Empezar un estudiante nuevo?",
                "Hay datos cargados en el formulario que todavía no se guardaron. Si seguís, se pierden.",
                "Descartar y empezar de nuevo", "Seguir editando", () -> {
                    limpiarFormulario();
                    mostrarFormulario();
                });
            return;
        }
        limpiarFormulario();
        mostrarFormulario();
    }

    /** Llamado desde la ficha del estudiante ("Editar datos"). */
    void editarEstudiante(int id) {
        cargarPacienteParaEditar(id);
    }

    /** Para que {@link EstudiantesPanel} muestre la ficha dentro de la misma ventana. */
    void setAlAbrirFicha(java.util.function.IntConsumer alAbrirFicha) {
        this.alAbrirFicha = alAbrirFicha;
    }

    /** true si el panel "Nuevo / Editar estudiante" está abierto con datos sin guardar. */
    boolean tieneCambiosSinGuardar() {
        return formularioAbierto() && formularioModificado;
    }

    boolean formularioAbierto() {
        return contenedorLateral.isVisible() && !animandoCierre;
    }

    /** Cierra el panel lateral; si hay cambios sin guardar, pregunta antes. */
    void cerrarFormulario() {
        if (formularioModificado) {
            Tema.confirmar(this, "¿Descartar los cambios?",
                "Hay datos cargados en el formulario que todavía no se guardaron.",
                "Descartar", "Seguir editando", () -> {
                    limpiarFormulario();
                    ocultarFormulario();
                });
            return;
        }
        limpiarFormulario();
        ocultarFormulario();
    }

    /** Desliza el panel lateral hacia adentro y lleva el cursor al primer campo. */
    private void mostrarFormulario() {
        scrollFormulario.getVerticalScrollBar().setValue(0);
        SwingUtilities.invokeLater(() -> txtNombre.requestFocusInWindow());
        if (formularioAbierto()) {
            return;
        }
        animandoCierre = false;
        contenedorLateral.setVisible(true);
        animarPanelLateral(ANCHO_PANEL_LATERAL, null);
    }

    private void ocultarFormulario() {
        if (!contenedorLateral.isVisible()) {
            return;
        }
        animandoCierre = true;
        animarPanelLateral(0, () -> {
            contenedorLateral.setVisible(false);
            animandoCierre = false;
        });
    }

    private void animarPanelLateral(int anchoObjetivo, Runnable alTerminar) {
        if (timerPanelLateral != null) {
            timerPanelLateral.stop();
        }
        int anchoInicial = anchoLateral;
        long inicio = System.currentTimeMillis();
        timerPanelLateral = new Timer(12, null);
        timerPanelLateral.addActionListener(e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - inicio) / 220f);
            float avance = 1f - (float) Math.pow(1f - t, 3); // ease-out cúbico
            anchoLateral = Math.round(anchoInicial + (anchoObjetivo - anchoInicial) * avance);
            contenedorLateral.revalidate();
            contenedorLateral.getParent().repaint();
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                if (alTerminar != null) {
                    alTerminar.run();
                }
            }
        });
        timerPanelLateral.start();
    }

    /**
     * Deja la lista mostrando solo un curso (o todos, con null), sin texto de búsqueda. Si el
     * curso no está en el catálogo oficial del filtro, avisa y muestra todos.
     */
    void filtrarPorCurso(String curso) {
        txtBusqueda.setText("");
        if (curso == null) {
            comboFiltroCurso.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < comboFiltroCurso.getItemCount(); i++) {
            if (curso.equals(comboFiltroCurso.getItemAt(i))) {
                comboFiltroCurso.setSelectedIndex(i);
                Tema.mostrarNotificacion(this, "Mostrando: " + curso);
                return;
            }
        }
        comboFiltroCurso.setSelectedIndex(0);
        Tema.mostrarNotificacion(this, "Ese curso no está en el catálogo oficial: se muestran todos");
    }

    /** Ítem del combo "Profesional asignado"; id null = sin asignar. */
    private static final class ItemProfesional {
        final Integer id;
        final String nombre;

        ItemProfesional(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    /** Profesionales activos para el combo (administración / usuario autorizado). */
    private void cargarProfesionales() {
        Tema.enSegundoPlano(
            this,
            () -> new UsuarioDAO().obtenerPorRol("psicologo"),
            profesionales -> {
                cargandoFormulario = true;
                ItemProfesional elegido = (ItemProfesional) comboProfesional.getSelectedItem();
                comboProfesional.removeAllItems();
                comboProfesional.addItem(new ItemProfesional(null, "Sin asignar"));
                for (Usuario u : profesionales) {
                    comboProfesional.addItem(new ItemProfesional(u.getId(), u.getNombre()));
                }
                if (elegido != null) {
                    seleccionarProfesional(elegido.id != null ? elegido.id : 0);
                }
                cargandoFormulario = false;
            },
            error -> System.out.println("Error cargando profesionales: " + error.getMessage())
        );
    }

    /** Selecciona el profesional por id (0 = sin asignar). Si está dado de baja, lo agrega marcado así. */
    private void seleccionarProfesional(int psicologoId) {
        if (psicologoId <= 0) {
            if (comboProfesional.getItemCount() > 0) {
                comboProfesional.setSelectedIndex(0);
            }
            return;
        }
        for (int i = 0; i < comboProfesional.getItemCount(); i++) {
            ItemProfesional item = comboProfesional.getItemAt(i);
            if (item.id != null && item.id == psicologoId) {
                comboProfesional.setSelectedIndex(i);
                return;
            }
        }
        ItemProfesional inactivo = new ItemProfesional(psicologoId, "Profesional inactivo (#" + psicologoId + ")");
        comboProfesional.addItem(inactivo);
        comboProfesional.setSelectedItem(inactivo);
    }

    private Integer idSeleccionado() {
        int fila = tablaPacientes.getSelectedRow();
        return fila < 0 ? null : (Integer) modeloTabla.getValueAt(tablaPacientes.convertRowIndexToModel(fila), 0);
    }

    private void actualizarBotonesSeleccion() {
        boolean haySeleccion = tablaPacientes.getSelectedRow() >= 0;
        btnVerFicha.setEnabled(haySeleccion);
        btnEditar.setEnabled(haySeleccion);
        btnEliminar.setEnabled(haySeleccion);
    }

    private void exportarListaVisible() {
        if (pacientesVisibles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay estudiantes en la tabla para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Paciente> lista = new java.util.ArrayList<>(pacientesVisibles);
        String busqueda = txtBusqueda.getText().trim();
        String curso = (String) comboFiltroCurso.getSelectedItem();
        String descripcion;
        if (busqueda.isEmpty() && "Todos".equals(curso)) {
            descripcion = "todos";
        } else {
            descripcion = "filtro:" + (busqueda.isEmpty() ? "" : " búsqueda '" + busqueda + "'")
                + ("Todos".equals(curso) ? "" : " curso " + curso);
        }
        Tema.enSegundoPlano(
            this,
            () -> util.GeneradorReportes.exportarListadoEstudiantes(lista, descripcion),
            archivo -> Tema.mostrarArchivoGuardado(this, "Lista exportada", archivo),
            error -> JOptionPane.showMessageDialog(this, "No se pudo exportar: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void cargarPacientes() {
        Tema.enSegundoPlano(
            this,
            () -> {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())
                    : pacienteDAO.obtenerTodos();
                return armarResultadoLista(pacientes);
            },
            resultado -> poblarTabla(resultado.pacientes, resultado.conSeguimientoVencido),
            error -> System.out.println("Error cargando pacientes: " + error.getMessage())
        );
    }

    private void buscarPacientes() {
        String termino = txtBusqueda.getText().trim();
        if (termino.isEmpty()) {
            cargarPacientes();
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.buscarPorPsicologo(termino, util.Sesion.getUsuarioId())
                    : pacienteDAO.buscar(termino);
                return armarResultadoLista(pacientes);
            },
            resultado -> poblarTabla(resultado.pacientes, resultado.conSeguimientoVencido),
            error -> System.out.println("Error buscando pacientes: " + error.getMessage())
        );
    }

    private static final class ResultadoListaPacientes {
        final List<Paciente> pacientes;
        final java.util.Set<Integer> conSeguimientoVencido;
        ResultadoListaPacientes(List<Paciente> pacientes, java.util.Set<Integer> conSeguimientoVencido) {
            this.pacientes = pacientes;
            this.conSeguimientoVencido = conSeguimientoVencido;
        }
    }

    /**
     * Corre en el hilo de fondo de {@link Tema#enSegundoPlano}: junto con la lista, trae en la
     * misma pasada los ids con seguimiento vencido (un solo query, evita un N+1 al pintar la
     * tabla). "secretaria" no ve esta alerta: es la misma señal clínica (caso de acoso reiterado)
     * que ya está bloqueada para ese rol en Panel y en las pestañas de DetallePaciente.
     */
    private ResultadoListaPacientes armarResultadoLista(List<Paciente> pacientes) {
        boolean esSecretaria = util.Sesion.esSecretaria();
        Integer psicologoId = util.Sesion.esPsicologo() ? util.Sesion.getUsuarioId() : null;
        java.util.Set<Integer> conSeguimientoVencido = esSecretaria
            ? java.util.Collections.emptySet()
            : historiaDAO.obtenerIdsConSeguimientoVencido(psicologoId, HistoriaPsicologicaDAO.DIAS_UMBRAL_SEGUIMIENTO);
        return new ResultadoListaPacientes(pacientes, conSeguimientoVencido);
    }

    /** Puebla la tabla con la lista dada, aplicando el filtro de curso seleccionado (si hay uno). */
    private void poblarTabla(List<Paciente> pacientes, java.util.Set<Integer> conSeguimientoVencido) {
        modeloTabla.setRowCount(0);
        pacientesVisibles = new java.util.ArrayList<>();
        String filtroCurso = comboFiltroCurso != null ? (String) comboFiltroCurso.getSelectedItem() : "Todos";

        for (Paciente p : pacientes) {
            if (filtroCurso != null && !"Todos".equals(filtroCurso) && !filtroCurso.equals(p.getCurso())) {
                continue;
            }
            pacientesVisibles.add(p);
            modeloTabla.addRow(new Object[]{
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getCi() != null ? p.getCi() : "N/A",
                p.getTelefono() != null ? p.getTelefono() : "N/A",
                p.getGenero() != null ? p.getGenero() : "N/A",
                p.getCurso() != null ? p.getCurso() : "N/A",
                p.isConsentimientoTutor() ? "Sí" : "No",
                conSeguimientoVencido.contains(p.getId()) ? "Seguimiento pendiente"
                    : (!util.Sesion.esPsicologo() && p.getPsicologoId() <= 0) ? "Sin profesional asignado" : ""
            });
        }
        int n = pacientesVisibles.size();
        Tema.actualizarTituloTarjeta(tarjetaTabla, "Estudiantes registrados · " + n);

        if (idASeleccionar != null) {
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                if (idASeleccionar.equals(modeloTabla.getValueAt(i, 0))) {
                    int filaVista = tablaPacientes.convertRowIndexToView(i);
                    if (filaVista >= 0) {
                        tablaPacientes.setRowSelectionInterval(filaVista, filaVista);
                        tablaPacientes.scrollRectToVisible(tablaPacientes.getCellRect(filaVista, 0, true));
                    }
                    break;
                }
            }
            idASeleccionar = null;
        }
        actualizarBotonesSeleccion();
    }

    private void guardarPaciente(boolean seguirAgregando) {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String ci = txtCi.getText().trim();
        String telefono = txtTelefono.getText().trim();

        Tema.marcarError(txtNombre, !nombre.isEmpty());
        Tema.marcarError(txtApellido, !apellido.isEmpty());

        if (nombre.isEmpty() || apellido.isEmpty()) {
            lblErrorFormulario.setText("Falta completar " + (nombre.isEmpty() && apellido.isEmpty() ? "el nombre y el apellido"
                : nombre.isEmpty() ? "el nombre" : "el apellido") + ".");
            scrollFormulario.getVerticalScrollBar().setValue(0);
            (nombre.isEmpty() ? txtNombre : txtApellido).requestFocusInWindow();
            return;
        }
        lblErrorFormulario.setText(" ");

        String nombreTutor = vacioANull(txtNombreTutor.getText());
        String ciTutor = vacioANull(txtCiTutor.getText());
        String genero = (String) comboGenero.getSelectedItem();
        String curso = selectorCurso.getCursoSeleccionado();
        boolean consentimiento = chkConsentimientoTutor.isSelected();
        Integer idActual = pacienteIdActual;
        ItemProfesional profesional = (ItemProfesional) comboProfesional.getSelectedItem();
        Integer profesionalId = profesional != null ? profesional.id : null;

        // Deshabilitado mientras dura el guardado: al correr en segundo plano, la ventana ya no
        // queda congelada, así que sin esto un doble clic en "Guardar" podía disparar dos
        // inserciones y crear un estudiante duplicado.
        habilitarGuardado(false);

        Tema.enSegundoPlano(
            this,
            () -> guardarPacienteEnBaseDeDatos(
                idActual, nombre, apellido, ci, telefono, genero, nombreTutor, ciTutor, consentimiento, curso, profesionalId),
            resultado -> {
                habilitarGuardado(true);
                if (resultado.estudianteInexistente) {
                    JOptionPane.showMessageDialog(this, "El estudiante ya no existe", "Error", JOptionPane.ERROR_MESSAGE);
                    limpiarFormulario();
                    ocultarFormulario();
                    cargarPacientes();
                } else if (resultado.exito) {
                    Tema.mostrarNotificacion(this, resultado.mensaje);
                    idASeleccionar = resultado.id;
                    recargarLista();
                    limpiarFormulario();
                    if (seguirAgregando) {
                        lblSubtituloFormulario.setText("Guardado: " + nombre + " " + apellido + ". Cargá el siguiente.");
                        mostrarFormulario();
                    } else {
                        ocultarFormulario();
                    }
                } else {
                    lblErrorFormulario.setText("<html>" + resultado.mensaje + "</html>");
                }
            },
            error -> {
                habilitarGuardado(true);
                lblErrorFormulario.setText("<html>No se pudo guardar: " + error.getMessage() + "</html>");
            }
        );
    }

    private void habilitarGuardado(boolean habilitado) {
        btnGuardar.setEnabled(habilitado);
        btnGuardarYOtro.setEnabled(habilitado);
        btnGuardar.setText(habilitado ? "Guardar" : "Guardando...");
    }

    /** Recarga respetando la búsqueda escrita (si hay). */
    private void recargarLista() {
        if (txtBusqueda.getText().trim().isEmpty()) {
            cargarPacientes();
        } else {
            buscarPacientes();
        }
    }

    private static final class ResultadoGuardado {
        boolean exito;
        boolean estudianteInexistente;
        String mensaje;
        Integer id;
    }

    /** Corre en el hilo de fondo de {@link Tema#enSegundoPlano} — sin tocar componentes Swing acá. */
    private ResultadoGuardado guardarPacienteEnBaseDeDatos(
            Integer idActual, String nombre, String apellido, String ci, String telefono, String genero,
            String nombreTutor, String ciTutor, boolean consentimiento, String curso, Integer profesionalId) {

        ResultadoGuardado resultado = new ResultadoGuardado();

        if (idActual != null) {
            Paciente paciente = pacienteDAO.obtenerPorId(idActual);
            if (paciente == null) {
                resultado.estudianteInexistente = true;
                return resultado;
            }
            paciente.setNombre(nombre);
            paciente.setApellido(apellido);
            paciente.setCi(ci);
            paciente.setTelefono(telefono);
            paciente.setGenero(genero);
            paciente.setNombreTutor(nombreTutor);
            paciente.setCiTutor(ciTutor);
            paciente.setCurso(curso);
            if (!util.Sesion.esPsicologo()) {
                paciente.setPsicologoId(profesionalId != null ? profesionalId : 0);
            }
            if (consentimiento) {
                if (!paciente.isConsentimientoTutor()) {
                    paciente.setConsentimientoFecha(java.time.LocalDateTime.now());
                }
                paciente.setConsentimientoTutor(true);
            } else {
                paciente.setConsentimientoTutor(false);
                paciente.setConsentimientoFecha(null);
            }

            resultado.exito = pacienteDAO.actualizar(paciente);
            resultado.id = idActual;
            if (resultado.exito) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "EDITAR_PACIENTE", "pacientes", idActual, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                resultado.mensaje = "Estudiante actualizado exitosamente";
            } else {
                resultado.mensaje = "Error al actualizar el estudiante";
            }
        } else {
            Paciente paciente = new Paciente(nombre, apellido, ci, telefono);
            paciente.setGenero(genero);
            paciente.setNombreTutor(nombreTutor);
            paciente.setCiTutor(ciTutor);
            paciente.setCurso(curso);
            paciente.setConsentimientoTutor(consentimiento);
            if (util.Sesion.esPsicologo()) {
                paciente.setPsicologoId(util.Sesion.getUsuarioId());
            } else if (profesionalId != null) {
                // Antes, un estudiante cargado por administración o por el usuario autorizado quedaba
                // sin profesional y ningún profesional lo veía nunca.
                paciente.setPsicologoId(profesionalId);
            }

            int nuevoId = pacienteDAO.crear(paciente);
            resultado.exito = nuevoId > 0;
            resultado.id = nuevoId;
            if (resultado.exito) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "CREAR_PACIENTE", "pacientes", nuevoId, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                resultado.mensaje = "Estudiante guardado exitosamente";
            } else {
                resultado.mensaje =
                    "No se pudo guardar el estudiante. Revisá la consola de la aplicación para el detalle del "
                        + "error (por ejemplo, si falta aplicar la migración db-init/06_estructura_academica.sql "
                        + "en la base de datos).";
            }
        }

        return resultado;
    }

    private void editarPacienteSeleccionado() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(tablaPacientes.convertRowIndexToModel(fila), 0);
        if (formularioAbierto() && formularioModificado) {
            Tema.confirmar(this, "¿Editar otro estudiante?",
                "Hay datos cargados en el formulario que todavía no se guardaron. Si seguís, se pierden.",
                "Descartar y editar", "Seguir editando", () -> cargarPacienteParaEditar(id));
            return;
        }
        cargarPacienteParaEditar(id);
    }

    private void cargarPacienteParaEditar(int id) {
        Tema.enSegundoPlano(
            this,
            () -> pacienteDAO.obtenerPorId(id),
            paciente -> {
                if (paciente == null) {
                    JOptionPane.showMessageDialog(this, "No se encontró el estudiante", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                cargandoFormulario = true;
                pacienteIdActual = id;
                txtNombre.setText(paciente.getNombre());
                txtApellido.setText(paciente.getApellido());
                txtCi.setText(paciente.getCi());
                txtTelefono.setText(paciente.getTelefono());
                txtNombreTutor.setText(paciente.getNombreTutor() != null ? paciente.getNombreTutor() : "");
                txtCiTutor.setText(paciente.getCiTutor() != null ? paciente.getCiTutor() : "");
                chkConsentimientoTutor.setSelected(paciente.isConsentimientoTutor());
                String genero = paciente.getGenero();
                comboGenero.setSelectedItem(
                    ("Masculino".equals(genero) || "Femenino".equals(genero) || "Otro".equals(genero)) ? genero : "Otro");
                selectorCurso.setCursoSeleccionado(paciente.getCurso());
                if (!util.Sesion.esPsicologo()) {
                    seleccionarProfesional(paciente.getPsicologoId());
                }

                cargandoFormulario = false;
                formularioModificado = false;
                lblTituloFormulario.setText("Editar estudiante");
                lblSubtituloFormulario.setText(paciente.getNombre() + " " + paciente.getApellido()
                    + " · los cambios se aplican al tocar Guardar");
                btnGuardarYOtro.setVisible(false);
                lblErrorFormulario.setText(" ");
                actualizarCompletitud();
                mostrarFormulario();
                repaint();
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void eliminarPaciente() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(tablaPacientes.convertRowIndexToModel(fila), 0);
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea eliminar este estudiante?", "Eliminar estudiante",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                String errorEliminar = pacienteDAO.eliminar(id);
                if (errorEliminar == null) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "ELIMINAR_PACIENTE", "pacientes", id, null);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return errorEliminar;
            },
            errorEliminar -> {
                if (errorEliminar == null) {
                    Tema.mostrarNotificacion(this, "Estudiante eliminado");
                    if (id == (pacienteIdActual != null ? pacienteIdActual : -1)) {
                        limpiarFormulario();
                    }
                    cargarPacientes();
                } else {
                    JOptionPane.showMessageDialog(this, errorEliminar, "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void verDetallesPaciente() {
        int fila = tablaPacientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) modeloTabla.getValueAt(tablaPacientes.convertRowIndexToModel(fila), 0);

        // La ficha se muestra en la misma ventana (EstudiantesPanel la pone en lugar de la lista);
        // el registro de auditoría (un simple log de lectura) corre en segundo plano.
        if (alAbrirFicha != null) {
            alAbrirFicha.accept(id);
        } else {
            JFrame ventana = new JFrame("Ficha del estudiante");
            ventana.setContentPane(new DetallePaciente(id, () -> cargarPacienteParaEditar(id), ventana::dispose));
            ventana.setSize(1150, 850);
            ventana.setLocationRelativeTo(this);
            ventana.setVisible(true);
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "VER_PACIENTE", "pacientes", id, null);
                }
                return null;
            },
            resultado -> { },
            error -> System.out.println("Error registrando auditoría: " + error.getMessage())
        );
    }

    private void limpiarFormulario() {
        cargandoFormulario = true;
        pacienteIdActual = null;
        txtNombre.setText("");
        txtApellido.setText("");
        txtCi.setText("");
        txtTelefono.setText("");
        txtNombreTutor.setText("");
        txtCiTutor.setText("");
        chkConsentimientoTutor.setSelected(false);
        comboGenero.setSelectedIndex(0);
        selectorCurso.setCursoSeleccionado(null);
        if (!util.Sesion.esPsicologo() && comboProfesional.getItemCount() > 0) {
            comboProfesional.setSelectedIndex(0);
        }
        cargandoFormulario = false;
        formularioModificado = false;
        lblTituloFormulario.setText("Nuevo estudiante");
        lblSubtituloFormulario.setText("Completá los datos y tocá Guardar");
        lblErrorFormulario.setText(" ");
        btnGuardarYOtro.setVisible(true);
        actualizarCompletitud();
        Tema.marcarError(txtNombre, true);
        Tema.marcarError(txtApellido, true);
        repaint();
    }

    private String vacioANull(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
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
