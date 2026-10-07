package Vista;

import dao.AuditoriaDAO;
import dao.UsuarioDAO;
import modelos.Usuario;
import util.Auditoria;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pantalla de Ajustes: cambio de la propia contraseña (cualquier rol) y, para admin,
 * alta de cuentas / reseteo de contraseña / activar-desactivar usuarios. "Cambiar de
 * cuenta" se resuelve con Cerrar sesión: vuelve al Login para entrar con otro usuario.
 */
public class Configuracion extends javax.swing.JPanel {

    private static final String[] ROLES = {"admin", "psicologo", "secretaria"};

    /**
     * Largo mínimo de una contraseña nueva (antes 4). Las cuentas guardan datos clínicos de
     * menores: 8 es el mínimo razonable. Solo se exige al crear o cambiar una contraseña; las
     * existentes más cortas siguen funcionando hasta que se cambien.
     */
    private static final int MIN_LARGO_PASSWORD = 8;

    private final Runnable alVolver;
    private final Runnable alCerrarSesion;
    private final Runnable alReconstruirShell;
    /** Si está, el botón de tema lo usa (quien lo provee decide si se puede recargar la ventana). */
    private Runnable alCambiarTema;
    private final UsuarioDAO usuarioDAO;
    private final AuditoriaDAO auditoriaDAO;

    private JTextField txtMiNombre;
    private JPasswordField txtPasswordActual;
    private JPasswordField txtPasswordNueva;
    private JPasswordField txtPasswordConfirmar;

    private JTable tablaUsuarios;
    private DefaultTableModel modeloTabla;
    private JTextField txtNuevoUsuario;
    private JTextField txtNuevoNombre;
    private JPasswordField txtNuevaPasswordUsuario;
    private JComboBox<String> comboNuevoRol;
    private JComboBox<String> comboCambiarRol;
    private JButton btnCrear;

    private JTable tablaAuditoria;
    private DefaultTableModel modeloAuditoria;
    private JTextField txtFiltroAuditoria;
    private JSpinner spinnerDesde;
    private JSpinner spinnerHasta;
    private JLabel lblCantidadAuditoria;
    private int limiteAuditoria = 200;

    private JButton btnModoOscuro;

    public Configuracion(Runnable alVolver, Runnable alCerrarSesion, Runnable alReconstruirShell) {
        this(alVolver, alCerrarSesion, alReconstruirShell, true);
    }

    /**
     * @param construirLayoutPropio si es false, no arma su propio encabezado/scroll/footer —
     *                              pensado para embeber esta pantalla como pestañas dentro de
     *                              otro contenedor, reutilizando directamente
     *                              {@link #crearPanelMiCuenta()}, {@link #crearPanelPreferencias()},
     *                              {@link #crearPanelUsuarios()}, {@link #crearPanelAuditoria()}
     *                              y {@link #crearFooter()}.
     */
    public Configuracion(Runnable alVolver, Runnable alCerrarSesion, Runnable alReconstruirShell,
            boolean construirLayoutPropio) {
        this.alVolver = alVolver;
        this.alCerrarSesion = alCerrarSesion;
        this.alReconstruirShell = alReconstruirShell;
        this.usuarioDAO = new UsuarioDAO();
        this.auditoriaDAO = new AuditoriaDAO();
        if (construirLayoutPropio) {
            initComponents();
        }
    }

    public void setAlCambiarTema(Runnable alCambiarTema) {
        this.alCambiarTema = alCambiarTema;
    }

    public void refrescar() {
        if (tablaUsuarios != null) {
            cargarUsuarios();
        }
        if (tablaAuditoria != null) {
            cargarAuditoria();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(Tema.panelEncabezado("Configuración", "Cuenta, preferencias, marco legal, usuarios y actividad"),
            BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Antes todo (Mi cuenta + Preferencias + Marco Legal + Usuarios + Actividad) vivía
        // apilado en un solo scroll larguísimo; con pestañas, cada sección tiene su propio
        // espacio y no hay que scrollear varias pantallas para llegar a "Actividad reciente".
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Tema.SUPERFICIE);
        tabs.setFont(Tema.TEXTO_CHICO);

        JPanel panelCuenta = new JPanel();
        panelCuenta.setBackground(Tema.FONDO);
        panelCuenta.setLayout(new BoxLayout(panelCuenta, BoxLayout.Y_AXIS));
        panelCuenta.add(crearPanelMiCuenta());
        panelCuenta.add(Box.createVerticalStrut(15));
        panelCuenta.add(crearPanelPreferencias());
        tabs.addTab("Mi cuenta", envolverScroll(panelCuenta));

        tabs.addTab("Marco Legal", envolverScroll(crearPanelMarcoLegal()));

        if (util.Sesion.esAdmin()) {
            tabs.addTab("Usuarios", envolverScroll(crearPanelUsuarios()));
            tabs.addTab("Actividad", crearPanelAuditoria());
        }

        mainPanel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(crearFooter(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JScrollPane envolverScroll(JPanel contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    JPanel crearPanelMiCuenta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        String usuario = "N/A";
        String rol = "N/A";
        String nombreActual = "";
        try {
            usuario = util.Sesion.getUsuario();
            rol = util.Sesion.getRol();
            nombreActual = util.Sesion.getNombre();
        } catch (Exception ignored) {
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel lblInfo = new JLabel("Conectado como " + usuario + " (" + Tema.etiquetaRol(rol) + ")");
        lblInfo.setFont(Tema.TEXTO);
        lblInfo.setForeground(Tema.TEXTO_SECUNDARIO);
        panel.add(lblInfo, gbc);

        txtMiNombre = new JTextField(nombreActual, 18);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Nombre para mostrar:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtMiNombre, gbc);

        JButton btnGuardarNombre = Tema.botonPrimario("Guardar nombre", Icono.GUARDAR);
        btnGuardarNombre.addActionListener(e -> guardarMiNombre());
        gbc.gridy = 2;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(btnGuardarNombre, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        txtPasswordActual = new JPasswordField(18);
        txtPasswordNueva = new JPasswordField(18);
        txtPasswordConfirmar = new JPasswordField(18);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Contraseña actual:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtPasswordActual, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Nueva contraseña:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtPasswordNueva, gbc);

        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Confirmar nueva contraseña:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtPasswordConfirmar, gbc);

        JButton btnCambiar = Tema.botonPrimario("Cambiar contraseña", Icono.GUARDAR);
        btnCambiar.addActionListener(e -> cambiarMiPassword());
        gbc.gridy = 7;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(btnCambiar, gbc);

        JPanel tarjeta = Tema.panelTarjeta("Mi cuenta");
        tarjeta.add(panel, BorderLayout.CENTER);
        return tarjeta;
    }

    JPanel crearPanelPreferencias() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setOpaque(false);

        btnModoOscuro = Tema.botonSecundario(Tema.esOscuro() ? "Modo claro" : "Modo oscuro");
        btnModoOscuro.addActionListener(e -> {
            if (alCambiarTema != null) {
                alCambiarTema.run();
            } else {
                Tema.alternarModoOscuro();
                alReconstruirShell.run();
            }
        });
        panel.add(btnModoOscuro);

        // Bloqueo automático de pantalla (preferencia de esta PC).
        JLabel lblBloqueo = new JLabel("Bloquear la pantalla tras");
        panel.add(lblBloqueo);
        String[] opciones = new String[BloqueoPantalla.OPCIONES_MINUTOS.length];
        int seleccion = 1;
        for (int i = 0; i < opciones.length; i++) {
            int m = BloqueoPantalla.OPCIONES_MINUTOS[i];
            opciones[i] = m == 0 ? "Nunca (solo con Ctrl+L)" : m + " minutos sin uso";
            if (m == BloqueoPantalla.minutosConfigurados()) {
                seleccion = i;
            }
        }
        JComboBox<String> cmbBloqueo = new JComboBox<>(opciones);
        cmbBloqueo.setSelectedIndex(seleccion);
        cmbBloqueo.setToolTipText("Protege las fichas si alguien se levanta del escritorio. Ctrl+L bloquea al instante.");
        cmbBloqueo.addActionListener(e -> {
            BloqueoPantalla.guardarMinutos(BloqueoPantalla.OPCIONES_MINUTOS[cmbBloqueo.getSelectedIndex()]);
            Tema.mostrarNotificacion(this, "Preferencia de bloqueo guardada");
        });
        panel.add(cmbBloqueo);

        // Exportar/abrir carpetas vivía antes acá; quien lo busque en este lugar encuentra la pista.
        JLabel lblReportes = new JLabel("Exportar estudiantes, respaldos y carpetas de archivos: Estudiantes → pestaña \"Reportes y archivos\"");
        lblReportes.setFont(Tema.TEXTO_CHICO);
        lblReportes.setForeground(Tema.TEXTO_SECUNDARIO);
        panel.add(lblReportes);

        JPanel tarjeta = Tema.panelTarjeta("Preferencias");
        tarjeta.add(panel, BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * Referencia rápida (solo lectura) de las normas paraguayas relacionadas con la atención
     * psicológica escolar y el manejo de estos datos — visible para cualquier rol, ya que es
     * información pública/educativa, no un dato clínico. Contenido resumido a partir de
     * investigación externa (no reemplaza asesoría legal formal).
     */
    JPanel crearPanelMarcoLegal() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel lblIntro = new JLabel(
            "<html>Referencia rápida de las normas paraguayas relacionadas con la atención "
                + "psicológica escolar y con los datos que registra este sistema. No reemplaza "
                + "asesoría legal formal.</html>");
        lblIntro.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblIntro.setForeground(Tema.TEXTO_SECUNDARIO);
        lblIntro.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblIntro);
        panel.add(Box.createVerticalStrut(12));

        panel.add(bloqueLegal("Ley N° 4633/2012 — Contra el Acoso Escolar",
            "Define el acoso u hostigamiento escolar como violencia física, verbal, psicológica o "
                + "social entre estudiantes que se da de forma REITERADA y genera un agravio en el "
                + "desarrollo del afectado — un hecho aislado no alcanza este umbral legal. Clasifica "
                + "el acoso en tres tipos: Directo (daño físico), Indirecto (daño a bienes o "
                + "pertenencias) y Verbal (insultos, ofensas). Aplica a instituciones educativas "
                + "públicas, privadas y subvencionadas en todos los niveles. Esta es la clasificación "
                + "que usan los campos \"Tipo de acoso\" y \"¿Es reiterado?\" en Seguimiento del "
                + "Estudiante."));
        panel.add(Box.createVerticalStrut(12));

        panel.add(bloqueLegal("Resolución MEC N° 8353/2012 — Protocolo de Atención",
            "Aprueba el protocolo oficial que el colegio debe activar ante cualquier sospecha de "
                + "violencia o acoso escolar: detección temprana, medidas urgentes, y un plan de "
                + "intervención a nivel individual, de aula y familiar. Cualquier miembro de la "
                + "comunidad educativa (familia, docente, estudiante) puede activar el protocolo."));
        panel.add(Box.createVerticalStrut(12));

        panel.add(bloqueLegal("CIE-10 — Clasificación Internacional de Enfermedades",
            "Estándar de la OMS que también usa el Ministerio de Salud Pública de Paraguay (MSPBS) "
                + "para codificar diagnósticos y motivos de consulta en salud mental. El capítulo F "
                + "(F00-F99) cubre trastornos mentales reales (ej. F90 TDAH, F91 trastornos de "
                + "conducta) — asignar un código F es un acto clínico. El capítulo Z (Z00-Z99) cubre "
                + "situaciones de vida sin ser un trastorno (ej. Z55 problemas educativos, Z63 "
                + "problemas familiares) y suele ajustarse mejor a una atención escolar común. El "
                + "campo \"Código CIE-10\" de Seguimiento del Estudiante es opcional."));
        panel.add(Box.createVerticalStrut(12));

        panel.add(bloqueLegal("Ley N° 1680/01 — Código de la Niñez y la Adolescencia",
            "Establece que los expedientes de niños y adolescentes son estrictamente confidenciales: "
                + "solo pueden acceder el propio estudiante, sus padres/tutores, representantes "
                + "legales, y quienes demuestren un interés legítimo. Refuerza el control de acceso "
                + "por roles que ya usa este sistema (por ejemplo, que el rol \"secretaria\" no vea "
                + "las pestañas clínicas)."));
        panel.add(Box.createVerticalStrut(12));

        panel.add(bloqueLegal("Ley N° 7.593/2025 — Protección de Datos Personales",
            "Primera ley general de protección de datos de Paraguay (entra en vigencia en noviembre "
                + "de 2027). Clasifica la salud como \"dato sensible\" y fija sanciones más altas "
                + "cuando el titular del dato es un niño o adolescente — el caso de este sistema. "
                + "Exige poder responder pedidos de acceso/rectificación/eliminación en 30 días y "
                + "notificar brechas de seguridad en 72 horas. El consentimiento del padre/madre/"
                + "tutor que se registra en la ficha del estudiante responde a este marco."));
        panel.add(Box.createVerticalStrut(12));

        panel.add(bloqueLegal("Código de Ética del Psicólogo (Sociedad Paraguaya de Psicología)",
            "Establece el secreto profesional: toda la información que maneja el psicólogo es "
                + "confidencial y solo puede revelarse con consentimiento expreso del usuario o por "
                + "orden judicial."));

        JPanel tarjeta = Tema.panelTarjeta("Marco Legal y Normativo");
        tarjeta.add(panel, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel bloqueLegal(String titulo, String cuerpo) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Tema.BOTON);
        lblTitulo.setForeground(Tema.PRIMARIO);
        panel.add(lblTitulo, BorderLayout.NORTH);

        JTextArea txtCuerpo = new JTextArea(cuerpo);
        txtCuerpo.setEditable(false);
        txtCuerpo.setFocusable(false);
        txtCuerpo.setLineWrap(true);
        txtCuerpo.setWrapStyleWord(true);
        txtCuerpo.setFont(Tema.TEXTO_CHICO);
        txtCuerpo.setForeground(Tema.TEXTO_SECUNDARIO);
        txtCuerpo.setBackground(Tema.SUPERFICIE);
        txtCuerpo.setBorder(null);
        panel.add(txtCuerpo, BorderLayout.CENTER);

        return panel;
    }

    JPanel crearPanelAuditoria() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelFiltro.setBackground(Tema.SUPERFICIE);
        panelFiltro.add(new JLabel("Texto (usuario/acción/entidad):"));
        txtFiltroAuditoria = new JTextField(16);
        txtFiltroAuditoria.setBorder(Tema.bordeCampo());
        txtFiltroAuditoria.addActionListener(e -> cargarAuditoria());
        panelFiltro.add(txtFiltroAuditoria);

        panelFiltro.add(new JLabel("Desde:"));
        spinnerDesde = new JSpinner(new SpinnerDateModel());
        spinnerDesde.setEditor(new JSpinner.DateEditor(spinnerDesde, "dd/MM/yyyy"));
        spinnerDesde.setValue(java.sql.Date.valueOf(java.time.LocalDate.now().minusMonths(1)));
        panelFiltro.add(spinnerDesde);

        panelFiltro.add(new JLabel("Hasta:"));
        spinnerHasta = new JSpinner(new SpinnerDateModel());
        spinnerHasta.setEditor(new JSpinner.DateEditor(spinnerHasta, "dd/MM/yyyy"));
        panelFiltro.add(spinnerHasta);

        JButton btnRefrescar = Tema.botonSecundario("Actualizar", Icono.LIMPIAR);
        btnRefrescar.addActionListener(e -> {
            limiteAuditoria = 200;
            cargarAuditoria();
        });
        panelFiltro.add(btnRefrescar);
        panel.add(panelFiltro, BorderLayout.NORTH);

        String[] columnas = {"Fecha", "Usuario", "Acción", "Entidad", "ID", "Detalle"};
        modeloAuditoria = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaAuditoria = Tema.crearTablaConVacio(modeloAuditoria, "No hay actividad registrada en este rango de fechas");
        Tema.estilizarTabla(tablaAuditoria);
        tablaAuditoria.setPreferredScrollableViewportSize(new Dimension(0, 200));
        panel.add(new JScrollPane(tablaAuditoria), BorderLayout.CENTER);

        JPanel panelPie = new JPanel(new BorderLayout());
        panelPie.setBackground(Tema.SUPERFICIE);
        lblCantidadAuditoria = new JLabel();
        lblCantidadAuditoria.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblCantidadAuditoria.setForeground(Tema.TEXTO_SECUNDARIO);
        panelPie.add(lblCantidadAuditoria, BorderLayout.WEST);
        JButton btnCargarMas = Tema.botonSecundario("Cargar más antiguos");
        btnCargarMas.addActionListener(e -> {
            limiteAuditoria += 200;
            cargarAuditoria();
        });
        JPanel wrapperBtnMas = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wrapperBtnMas.setBackground(Tema.SUPERFICIE);
        wrapperBtnMas.add(btnCargarMas);
        panelPie.add(wrapperBtnMas, BorderLayout.EAST);
        panel.add(panelPie, BorderLayout.SOUTH);

        cargarAuditoria();

        JPanel tarjeta = Tema.panelTarjeta("Actividad reciente");
        tarjeta.add(panel, BorderLayout.CENTER);
        return tarjeta;
    }

    JPanel crearPanelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        String[] columnas = {"ID", "Usuario", "Nombre", "Rol", "Activo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaUsuarios = Tema.crearTablaConVacio(modeloTabla, "No hay usuarios para mostrar");
        Tema.estilizarTabla(tablaUsuarios);
        tablaUsuarios.getColumnModel().getColumn(3).setCellRenderer(Tema.rendererRol());
        tablaUsuarios.setPreferredScrollableViewportSize(new Dimension(0, 160));
        JScrollPane scrollTabla = new JScrollPane(tablaUsuarios);
        panel.add(scrollTabla, BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelAcciones.setBackground(Tema.SUPERFICIE);

        JButton btnResetPassword = Tema.botonSecundario("Restablecer contraseña");
        btnResetPassword.addActionListener(e -> restablecerPasswordSeleccionado());
        panelAcciones.add(btnResetPassword);

        JButton btnActivar = Tema.botonExito("Activar");
        btnActivar.addActionListener(e -> cambiarActivoSeleccionado(true));
        panelAcciones.add(btnActivar);

        JButton btnDesactivar = Tema.botonPeligro("Desactivar");
        btnDesactivar.addActionListener(e -> cambiarActivoSeleccionado(false));
        panelAcciones.add(btnDesactivar);

        JButton btnEliminar = Tema.botonPeligro("Eliminar cuenta", Icono.ELIMINAR);
        btnEliminar.addActionListener(e -> eliminarUsuarioSeleccionado());
        panelAcciones.add(btnEliminar);

        panelAcciones.add(new JLabel("Nuevo rol:"));
        comboCambiarRol = new JComboBox<>(ROLES);
        comboCambiarRol.setRenderer(Tema.rendererRolCombo());
        panelAcciones.add(comboCambiarRol);
        JButton btnCambiarRol = Tema.botonPrimario("Cambiar rol", Icono.EDITAR);
        btnCambiarRol.addActionListener(e -> cambiarRolSeleccionado());
        panelAcciones.add(btnCambiarRol);

        JLabel lblNuevoUsuario = new JLabel("Nuevo usuario");
        lblNuevoUsuario.setFont(Tema.fuente(Font.BOLD, 13));
        lblNuevoUsuario.setForeground(Tema.PRIMARIO);
        lblNuevoUsuario.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel panelFormNuevo = new JPanel(new GridLayout(2, 4, 10, 10));
        panelFormNuevo.setOpaque(false);

        panelFormNuevo.add(new JLabel("Usuario:"));
        txtNuevoUsuario = new JTextField();
        txtNuevoUsuario.setBorder(Tema.bordeCampo());
        panelFormNuevo.add(txtNuevoUsuario);

        panelFormNuevo.add(new JLabel("Nombre:"));
        txtNuevoNombre = new JTextField();
        txtNuevoNombre.setBorder(Tema.bordeCampo());
        panelFormNuevo.add(txtNuevoNombre);

        panelFormNuevo.add(new JLabel("Contraseña:"));
        txtNuevaPasswordUsuario = new JPasswordField();
        panelFormNuevo.add(txtNuevaPasswordUsuario);

        panelFormNuevo.add(new JLabel("Rol:"));
        comboNuevoRol = new JComboBox<>(ROLES);
        comboNuevoRol.setRenderer(Tema.rendererRolCombo());
        panelFormNuevo.add(comboNuevoRol);

        btnCrear = Tema.botonExito("Crear usuario", Icono.NUEVO);
        btnCrear.addActionListener(e -> crearUsuario());

        JPanel panelSur = new JPanel(new BorderLayout(0, 10));
        panelSur.setOpaque(false);
        panelSur.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panelSur.add(panelAcciones, BorderLayout.NORTH);
        JPanel wrapperForm = new JPanel(new BorderLayout());
        wrapperForm.setOpaque(false);
        wrapperForm.add(lblNuevoUsuario, BorderLayout.NORTH);
        wrapperForm.add(panelFormNuevo, BorderLayout.CENTER);
        JPanel wrapperBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wrapperBtn.setOpaque(false);
        wrapperBtn.add(btnCrear);
        wrapperForm.add(wrapperBtn, BorderLayout.SOUTH);
        panelSur.add(wrapperForm, BorderLayout.SOUTH);

        panel.add(panelSur, BorderLayout.SOUTH);

        cargarUsuarios();

        JPanel tarjeta = Tema.panelTarjeta("Usuarios del sistema");
        tarjeta.add(panel, BorderLayout.CENTER);
        return tarjeta;
    }

    JPanel crearFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        footer.setBackground(Tema.FONDO);

        JButton btnCerrarSesion = Tema.botonPeligro("Cerrar sesión");
        btnCerrarSesion.addActionListener(e -> {
            int opcion = JOptionPane.showConfirmDialog(this,
                "¿Cerrar la sesión actual y volver a la pantalla de inicio de sesión?",
                "Cerrar sesión", JOptionPane.YES_NO_OPTION);
            if (opcion == JOptionPane.YES_OPTION) {
                alCerrarSesion.run();
            }
        });
        footer.add(btnCerrarSesion);

        JButton btnVolver = Tema.botonSecundario("Volver", Icono.VOLVER);
        btnVolver.addActionListener(e -> alVolver.run());
        footer.add(btnVolver);
        Tema.atajoEscape(this, btnVolver, alVolver);

        return footer;
    }

    private void cargarUsuarios() {
        Tema.enSegundoPlano(
            this,
            usuarioDAO::obtenerTodos,
            usuarios -> {
                modeloTabla.setRowCount(0);
                for (Usuario u : usuarios) {
                    modeloTabla.addRow(new Object[]{
                        u.getId(), u.getUsuario(), u.getNombre(), u.getRol(),
                        u.isActivo() ? "Sí" : "No"
                    });
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void guardarMiNombre() {
        String nuevoNombre = txtMiNombre.getText().trim();
        if (nuevoNombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre no puede quedar vacío", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer idActual;
        try {
            idActual = util.Sesion.getUsuarioId();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No hay una sesión activa", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                boolean exito = usuarioDAO.actualizarNombre(idActual, nuevoNombre);
                if (exito) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "EDITAR_NOMBRE_PROPIO", "usuarios", idActual, nuevoNombre);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return exito;
            },
            exito -> {
                if (exito) {
                    util.Sesion.actualizarNombre(nuevoNombre);
                    Tema.mostrarNotificacion(this, "Nombre actualizado");
                    alReconstruirShell.run();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo actualizar el nombre", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void cargarAuditoria() {
        String filtro = txtFiltroAuditoria != null ? txtFiltroAuditoria.getText().trim().toLowerCase() : "";
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        java.time.LocalDate desde = ((java.util.Date) spinnerDesde.getValue()).toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate hasta = ((java.util.Date) spinnerHasta.getValue()).toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        int limite = limiteAuditoria;

        Tema.enSegundoPlano(
            this,
            () -> auditoriaDAO.obtenerRecientes(limite),
            registros -> {
                modeloAuditoria.setRowCount(0);
                int mostrados = 0;
                for (AuditoriaDAO.Registro r : registros) {
                    java.time.LocalDate fechaRegistro = r.fecha.toLocalDate();
                    if (fechaRegistro.isBefore(desde) || fechaRegistro.isAfter(hasta)) {
                        continue;
                    }

                    boolean coincide = filtro.isEmpty()
                        || (r.usuarioNombre != null && r.usuarioNombre.toLowerCase().contains(filtro))
                        || (r.accion != null && r.accion.toLowerCase().contains(filtro))
                        || (r.entidad != null && r.entidad.toLowerCase().contains(filtro));

                    if (coincide) {
                        modeloAuditoria.addRow(new Object[]{
                            r.fecha.format(formato), r.usuarioNombre, r.accion, r.entidad,
                            r.entidadId != null ? r.entidadId : "",
                            r.detalle != null ? r.detalle : ""
                        });
                        mostrados++;
                    }
                }
                lblCantidadAuditoria.setText(mostrados + " de " + registros.size() + " registros consultados"
                    + (registros.size() >= limite ? " (puede haber más antiguos, usá \"Cargar más antiguos\")" : ""));
            },
            error -> JOptionPane.showMessageDialog(this, "Error al cargar la actividad: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void cambiarRolSeleccionado() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }
        String nuevoRol = (String) comboCambiarRol.getSelectedItem();

        try {
            if (util.Sesion.getUsuarioId() != null && util.Sesion.getUsuarioId() == id) {
                JOptionPane.showMessageDialog(this, "No podés cambiar el rol de tu propia cuenta", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ignored) {
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                boolean exito = usuarioDAO.actualizarRol(id, nuevoRol);
                if (exito) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "EDITAR_ROL_USUARIO", "usuarios", id, nuevoRol);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return exito;
            },
            exito -> {
                if (exito) {
                    Tema.mostrarNotificacion(this, "Rol actualizado");
                    cargarUsuarios();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo actualizar el rol", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void cambiarMiPassword() {
        String actual = new String(txtPasswordActual.getPassword());
        String nueva = new String(txtPasswordNueva.getPassword());
        String confirmar = new String(txtPasswordConfirmar.getPassword());

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete los tres campos", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nueva.length() < MIN_LARGO_PASSWORD) {
            JOptionPane.showMessageDialog(this, "La nueva contraseña debe tener al menos " + MIN_LARGO_PASSWORD + " caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!nueva.equals(confirmar)) {
            JOptionPane.showMessageDialog(this, "La confirmación no coincide con la nueva contraseña", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String usuarioActual;
        Integer idActual;
        try {
            usuarioActual = util.Sesion.getUsuario();
            idActual = util.Sesion.getUsuarioId();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No hay una sesión activa", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> cambiarMiPasswordEnBaseDeDatos(usuarioActual, idActual, actual, nueva),
            resultado -> {
                switch (resultado) {
                    case CONTRASENA_ACTUAL_INCORRECTA:
                        JOptionPane.showMessageDialog(this, "La contraseña actual es incorrecta", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case EXITO:
                        Tema.mostrarNotificacion(this, "Contraseña actualizada correctamente");
                        txtPasswordActual.setText("");
                        txtPasswordNueva.setText("");
                        txtPasswordConfirmar.setText("");
                        break;
                    default:
                        JOptionPane.showMessageDialog(this, "No se pudo cambiar la contraseña", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private enum ResultadoCambioPassword { EXITO, CONTRASENA_ACTUAL_INCORRECTA, ERROR }

    /** Corre en el hilo de fondo de {@link Tema#enSegundoPlano} — sin tocar componentes Swing acá. */
    private ResultadoCambioPassword cambiarMiPasswordEnBaseDeDatos(
            String usuarioActual, Integer idActual, String actual, String nueva) {

        if (usuarioDAO.autenticar(usuarioActual, actual) == null) {
            return ResultadoCambioPassword.CONTRASENA_ACTUAL_INCORRECTA;
        }

        if (usuarioDAO.cambiarPassword(idActual, nueva)) {
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, "CAMBIAR_PASSWORD_PROPIA", "usuarios", idActual, null);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            return ResultadoCambioPassword.EXITO;
        }
        return ResultadoCambioPassword.ERROR;
    }

    private void crearUsuario() {
        String usuario = txtNuevoUsuario.getText().trim();
        String nombre = txtNuevoNombre.getText().trim();
        String password = new String(txtNuevaPasswordUsuario.getPassword());
        String rol = (String) comboNuevoRol.getSelectedItem();

        if (usuario.isEmpty() || nombre.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete usuario, nombre y contraseña", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.length() < MIN_LARGO_PASSWORD) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos " + MIN_LARGO_PASSWORD + " caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Deshabilitado mientras dura la creación: evita crear dos cuentas iguales de un doble clic.
        btnCrear.setEnabled(false);

        Tema.enSegundoPlano(
            this,
            () -> {
                if (usuarioDAO.existeUsuario(usuario)) {
                    return -1;
                }
                int nuevoId = usuarioDAO.crear(usuario, nombre, password, rol);
                if (nuevoId > 0) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "CREAR_USUARIO", "usuarios", nuevoId, usuario);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return nuevoId;
            },
            nuevoId -> {
                btnCrear.setEnabled(true);
                if (nuevoId == -1) {
                    JOptionPane.showMessageDialog(this, "Ya existe un usuario con ese nombre", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (nuevoId > 0) {
                    Tema.mostrarNotificacion(this, "Usuario creado exitosamente");
                    txtNuevoUsuario.setText("");
                    txtNuevoNombre.setText("");
                    txtNuevaPasswordUsuario.setText("");
                    cargarUsuarios();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al crear el usuario", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> {
                btnCrear.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    private int idSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        return (int) modeloTabla.getValueAt(tablaUsuarios.convertRowIndexToModel(fila), 0);
    }

    private void restablecerPasswordSeleccionado() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }
        String usuarioNombre = String.valueOf(modeloTabla.getValueAt(tablaUsuarios.convertRowIndexToModel(tablaUsuarios.getSelectedRow()), 1));

        JPasswordField campo = new JPasswordField();
        int opcion = JOptionPane.showConfirmDialog(this, campo,
            "Nueva contraseña para \"" + usuarioNombre + "\"", JOptionPane.OK_CANCEL_OPTION);

        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String nueva = new String(campo.getPassword());
        if (nueva.length() < MIN_LARGO_PASSWORD) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos " + MIN_LARGO_PASSWORD + " caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                boolean exito = usuarioDAO.cambiarPassword(id, nueva);
                if (exito) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "RESET_PASSWORD_USUARIO", "usuarios", id, usuarioNombre);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return exito;
            },
            exito -> {
                if (exito) {
                    Tema.mostrarNotificacion(this, "Contraseña restablecida");
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo restablecer la contraseña", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void cambiarActivoSeleccionado(boolean activo) {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }

        try {
            if (util.Sesion.getUsuarioId() != null && util.Sesion.getUsuarioId() == id && !activo) {
                JOptionPane.showMessageDialog(this, "No podés desactivar tu propia cuenta", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ignored) {
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                boolean exito = usuarioDAO.actualizarActivo(id, activo);
                if (exito) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", "usuarios", id, null);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return exito;
            },
            exito -> {
                if (exito) {
                    cargarUsuarios();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo actualizar el usuario", "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void eliminarUsuarioSeleccionado() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }

        try {
            if (util.Sesion.getUsuarioId() != null && util.Sesion.getUsuarioId() == id) {
                JOptionPane.showMessageDialog(this, "No podés eliminar tu propia cuenta", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ignored) {
        }

        String usuarioNombre = String.valueOf(modeloTabla.getValueAt(tablaUsuarios.convertRowIndexToModel(tablaUsuarios.getSelectedRow()), 1));
        int opcion = JOptionPane.showConfirmDialog(this,
            "¿Eliminar definitivamente la cuenta \"" + usuarioNombre + "\"? Esta acción no se puede deshacer.",
            "Eliminar cuenta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        Tema.enSegundoPlano(
            this,
            () -> {
                String errorEliminar = usuarioDAO.eliminar(id);
                if (errorEliminar == null) {
                    try (Connection cn = new conexion.Conexion().conectar()) {
                        Auditoria.registrar(cn, "ELIMINAR_USUARIO", "usuarios", id, usuarioNombre);
                    } catch (Exception ex) {
                        System.out.println("Error registrando auditoría: " + ex.getMessage());
                    }
                }
                return errorEliminar;
            },
            errorEliminar -> {
                if (errorEliminar == null) {
                    Tema.mostrarNotificacion(this, "Usuario eliminado");
                    cargarUsuarios();
                } else {
                    JOptionPane.showMessageDialog(this, errorEliminar, "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    /** Solo para probar esta pantalla de forma aislada, fuera del shell de MenuPrincipal. */
    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Configuración (prueba aislada)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setIconImage(Tema.iconoApp());
            f.getContentPane().add(new Configuracion(() -> System.exit(0), () -> System.exit(0), () -> {}));
            f.setSize(900, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
