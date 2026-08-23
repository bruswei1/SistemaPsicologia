package Vista;

import dao.AuditoriaDAO;
import dao.PacienteDAO;
import dao.UsuarioDAO;
import modelos.Paciente;
import modelos.Usuario;
import util.Auditoria;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
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

    private final Runnable alVolver;
    private final Runnable alCerrarSesion;
    private final Runnable alReconstruirShell;
    private final UsuarioDAO usuarioDAO;
    private final PacienteDAO pacienteDAO;
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

    private JTable tablaAuditoria;
    private DefaultTableModel modeloAuditoria;
    private JTextField txtFiltroAuditoria;
    private JSpinner spinnerDesde;
    private JSpinner spinnerHasta;
    private JLabel lblCantidadAuditoria;
    private int limiteAuditoria = 200;

    private JButton btnModoOscuro;

    public Configuracion(Runnable alVolver, Runnable alCerrarSesion, Runnable alReconstruirShell) {
        this.alVolver = alVolver;
        this.alCerrarSesion = alCerrarSesion;
        this.alReconstruirShell = alReconstruirShell;
        this.usuarioDAO = new UsuarioDAO();
        this.pacienteDAO = new PacienteDAO();
        this.auditoriaDAO = new AuditoriaDAO();
        initComponents();
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

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitulo = new JLabel("Configuración");
        lblTitulo.setFont(Tema.SUBTITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        mainPanel.add(lblTitulo, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setBackground(Tema.FONDO);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(crearPanelMiCuenta());
        centro.add(Box.createVerticalStrut(15));
        centro.add(crearPanelPreferencias());

        if (util.Sesion.esAdmin()) {
            centro.add(Box.createVerticalStrut(15));
            centro.add(crearPanelUsuarios());
            centro.add(Box.createVerticalStrut(15));
            centro.add(crearPanelAuditoria());
        }

        JScrollPane scroll = new JScrollPane(centro);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scroll, BorderLayout.CENTER);

        mainPanel.add(crearFooter(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel crearPanelMiCuenta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Mi cuenta"),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

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
        JLabel lblInfo = new JLabel("Conectado como " + usuario + " (" + rol + ")");
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

        return panel;
    }

    private JPanel crearPanelPreferencias() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Preferencias"),
            BorderFactory.createEmptyBorder(10, 15, 15, 15)
        ));

        btnModoOscuro = Tema.botonSecundario(Tema.esOscuro() ? "Modo claro" : "Modo oscuro");
        btnModoOscuro.addActionListener(e -> {
            Tema.alternarModoOscuro();
            alReconstruirShell.run();
        });
        panel.add(btnModoOscuro);

        JButton btnAdjuntos = Tema.botonSecundario("Abrir carpeta de adjuntos");
        btnAdjuntos.addActionListener(e -> abrirCarpeta("adjuntos"));
        panel.add(btnAdjuntos);

        JButton btnReportes = Tema.botonSecundario("Abrir carpeta de reportes");
        btnReportes.addActionListener(e -> abrirCarpeta("reportes"));
        panel.add(btnReportes);

        JButton btnExportar = Tema.botonPrimario("Exportar pacientes (CSV)", Icono.EXPORTAR);
        btnExportar.addActionListener(e -> exportarPacientes());
        panel.add(btnExportar);

        return panel;
    }

    private JPanel crearPanelAuditoria() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Actividad reciente"),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

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
        tablaAuditoria = new JTable(modeloAuditoria);
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

        return panel;
    }

    private JPanel crearPanelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Usuarios del sistema"),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        String[] columnas = {"ID", "Usuario", "Nombre", "Rol", "Activo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaUsuarios = new JTable(modeloTabla);
        Tema.estilizarTabla(tablaUsuarios);
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
        panelAcciones.add(comboCambiarRol);
        JButton btnCambiarRol = Tema.botonPrimario("Cambiar rol", Icono.EDITAR);
        btnCambiarRol.addActionListener(e -> cambiarRolSeleccionado());
        panelAcciones.add(btnCambiarRol);

        JPanel panelFormNuevo = new JPanel(new GridLayout(2, 4, 10, 10));
        panelFormNuevo.setBackground(Tema.SUPERFICIE);
        panelFormNuevo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Nuevo usuario"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

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
        panelFormNuevo.add(comboNuevoRol);

        JButton btnCrear = Tema.botonExito("Crear usuario", Icono.NUEVO);
        btnCrear.addActionListener(e -> crearUsuario());

        JPanel panelSur = new JPanel(new BorderLayout(0, 10));
        panelSur.setBackground(Tema.SUPERFICIE);
        panelSur.add(panelAcciones, BorderLayout.NORTH);
        JPanel wrapperForm = new JPanel(new BorderLayout());
        wrapperForm.setBackground(Tema.SUPERFICIE);
        wrapperForm.add(panelFormNuevo, BorderLayout.CENTER);
        JPanel wrapperBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wrapperBtn.setBackground(Tema.SUPERFICIE);
        wrapperBtn.add(btnCrear);
        wrapperForm.add(wrapperBtn, BorderLayout.SOUTH);
        panelSur.add(wrapperForm, BorderLayout.SOUTH);

        panel.add(panelSur, BorderLayout.SOUTH);

        cargarUsuarios();

        return panel;
    }

    private JPanel crearFooter() {
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
        modeloTabla.setRowCount(0);
        Tema.conCursorEspera(this, () -> {
            List<Usuario> usuarios = usuarioDAO.obtenerTodos();
            for (Usuario u : usuarios) {
                modeloTabla.addRow(new Object[]{
                    u.getId(), u.getUsuario(), u.getNombre(), u.getRol(),
                    u.isActivo() ? "Sí" : "No"
                });
            }
        });
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

        if (usuarioDAO.actualizarNombre(idActual, nuevoNombre)) {
            util.Sesion.actualizarNombre(nuevoNombre);
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, "EDITAR_NOMBRE_PROPIO", "usuarios", idActual, nuevoNombre);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Nombre actualizado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            alReconstruirShell.run();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el nombre", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirCarpeta(String nombreCarpeta) {
        try {
            File carpeta = new File(nombreCarpeta);
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(this, "Esta plataforma no permite abrir carpetas del explorador", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(carpeta);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir la carpeta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarPacientes() {
        Tema.conCursorEspera(this, () -> {
            List<Paciente> pacientes = util.Sesion.esPsicologo()
                ? pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())
                : pacienteDAO.obtenerTodos();

            if (pacientes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay pacientes para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String csv = util.GeneradorReportes.generarCSVPacientes(pacientes);
            String nombreArchivo = util.GeneradorReportes.obtenerNombreArchivoRespaldo();

            File carpetaReportes = new File("reportes");
            if (!carpetaReportes.exists()) {
                carpetaReportes.mkdirs();
            }
            String rutaArchivo = new File(carpetaReportes, nombreArchivo).getPath();

            try {
                if (util.GeneradorReportes.guardarReportePDF(csv, rutaArchivo)) {
                    JOptionPane.showMessageDialog(this, "Respaldo guardado en: " + rutaArchivo, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error al guardar el respaldo", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void cargarAuditoria() {
        modeloAuditoria.setRowCount(0);
        String filtro = txtFiltroAuditoria != null ? txtFiltroAuditoria.getText().trim().toLowerCase() : "";
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        java.time.LocalDate desde = ((java.util.Date) spinnerDesde.getValue()).toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate hasta = ((java.util.Date) spinnerHasta.getValue()).toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        Tema.conCursorEspera(this, () -> {
            List<AuditoriaDAO.Registro> registros = auditoriaDAO.obtenerRecientes(limiteAuditoria);
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
                + (registros.size() >= limiteAuditoria ? " (puede haber más antiguos, usá \"Cargar más antiguos\")" : ""));
        });
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

        if (usuarioDAO.actualizarRol(id, nuevoRol)) {
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, "EDITAR_ROL_USUARIO", "usuarios", id, nuevoRol);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Rol actualizado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarUsuarios();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el rol", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarMiPassword() {
        String actual = new String(txtPasswordActual.getPassword());
        String nueva = new String(txtPasswordNueva.getPassword());
        String confirmar = new String(txtPasswordConfirmar.getPassword());

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete los tres campos", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nueva.length() < 4) {
            JOptionPane.showMessageDialog(this, "La nueva contraseña debe tener al menos 4 caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!nueva.equals(confirmar)) {
            JOptionPane.showMessageDialog(this, "La confirmación no coincide con la nueva contraseña", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Tema.conCursorEspera(this, () -> {
            String usuarioActual;
            Integer idActual;
            try {
                usuarioActual = util.Sesion.getUsuario();
                idActual = util.Sesion.getUsuarioId();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "No hay una sesión activa", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (usuarioDAO.autenticar(usuarioActual, actual) == null) {
                JOptionPane.showMessageDialog(this, "La contraseña actual es incorrecta", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (usuarioDAO.cambiarPassword(idActual, nueva)) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "CAMBIAR_PASSWORD_PROPIA", "usuarios", idActual, null);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                JOptionPane.showMessageDialog(this, "Contraseña actualizada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                txtPasswordActual.setText("");
                txtPasswordNueva.setText("");
                txtPasswordConfirmar.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo cambiar la contraseña", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
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
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 4 caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (usuarioDAO.existeUsuario(usuario)) {
            JOptionPane.showMessageDialog(this, "Ya existe un usuario con ese nombre", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Tema.conCursorEspera(this, () -> {
            int nuevoId = usuarioDAO.crear(usuario, nombre, password, rol);
            if (nuevoId > 0) {
                try (Connection cn = new conexion.Conexion().conectar()) {
                    Auditoria.registrar(cn, "CREAR_USUARIO", "usuarios", nuevoId, usuario);
                } catch (Exception ex) {
                    System.out.println("Error registrando auditoría: " + ex.getMessage());
                }
                JOptionPane.showMessageDialog(this, "Usuario creado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                txtNuevoUsuario.setText("");
                txtNuevoNombre.setText("");
                txtNuevaPasswordUsuario.setText("");
                cargarUsuarios();
            } else {
                JOptionPane.showMessageDialog(this, "Error al crear el usuario", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private int idSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        return (int) modeloTabla.getValueAt(fila, 0);
    }

    private void restablecerPasswordSeleccionado() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }
        String usuarioNombre = String.valueOf(modeloTabla.getValueAt(tablaUsuarios.getSelectedRow(), 1));

        JPasswordField campo = new JPasswordField();
        int opcion = JOptionPane.showConfirmDialog(this, campo,
            "Nueva contraseña para \"" + usuarioNombre + "\"", JOptionPane.OK_CANCEL_OPTION);

        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String nueva = new String(campo.getPassword());
        if (nueva.length() < 4) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 4 caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (usuarioDAO.cambiarPassword(id, nueva)) {
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, "RESET_PASSWORD_USUARIO", "usuarios", id, usuarioNombre);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Contraseña restablecida", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo restablecer la contraseña", "Error", JOptionPane.ERROR_MESSAGE);
        }
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

        if (usuarioDAO.actualizarActivo(id, activo)) {
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", "usuarios", id, null);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            cargarUsuarios();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el usuario", "Error", JOptionPane.ERROR_MESSAGE);
        }
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

        String usuarioNombre = String.valueOf(modeloTabla.getValueAt(tablaUsuarios.getSelectedRow(), 1));
        int opcion = JOptionPane.showConfirmDialog(this,
            "¿Eliminar definitivamente la cuenta \"" + usuarioNombre + "\"? Esta acción no se puede deshacer.",
            "Eliminar cuenta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        String error = usuarioDAO.eliminar(id);
        if (error == null) {
            try (Connection cn = new conexion.Conexion().conectar()) {
                Auditoria.registrar(cn, "ELIMINAR_USUARIO", "usuarios", id, usuarioNombre);
            } catch (Exception ex) {
                System.out.println("Error registrando auditoría: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Usuario eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarUsuarios();
        } else {
            JOptionPane.showMessageDialog(this, error, "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
        }
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
