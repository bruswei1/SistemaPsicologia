package Vista;

import util.Auditoria;
import util.Sesion;
import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BooleanSupplier;

/**
 * Bloqueo de pantalla: tras unos minutos sin usar el mouse ni el teclado (o al tocar Ctrl+L), la
 * ventana se tapa con una pantalla que pide la contraseña. No se cierra nada: al desbloquear
 * todo sigue exactamente como estaba (incluida una atención a medio escribir).
 *
 * Pensado para la PC compartida del Departamento: si alguien se levanta del escritorio, las
 * fichas con datos clínicos de estudiantes no quedan a la vista.
 */
public final class BloqueoPantalla {

    public static final String PREFERENCIA_MINUTOS = "bloqueo.minutos";
    public static final int[] OPCIONES_MINUTOS = {5, 10, 15, 30, 0};
    private static final int MINUTOS_POR_DEFECTO = 10;
    private static final int MAX_INTENTOS = 5;

    private final JFrame ventana;
    private final BooleanSupplier puedeBloquearPorInactividad;
    private final BooleanSupplier hayCambiosSinGuardar;
    private final Runnable alCerrarSesion;

    private volatile long ultimaActividad = System.currentTimeMillis();
    private AWTEventListener oyenteActividad;
    private Timer vigilante;
    private JComponent capa;
    private int intentosFallidos;

    /**
     * @param puedeBloquearPorInactividad false mientras no conviene bloquear solo (p. ej. con el
     *                                    cronómetro de una atención en marcha)
     * @param alCerrarSesion              cierra la sesión sin preguntar (el aviso lo da esta pantalla)
     */
    public BloqueoPantalla(JFrame ventana, BooleanSupplier puedeBloquearPorInactividad,
                           BooleanSupplier hayCambiosSinGuardar, Runnable alCerrarSesion) {
        this.ventana = ventana;
        this.puedeBloquearPorInactividad = puedeBloquearPorInactividad;
        this.hayCambiosSinGuardar = hayCambiosSinGuardar;
        this.alCerrarSesion = alCerrarSesion;
    }

    /** Minutos de inactividad configurados en esta PC (0 = no bloquear nunca solo). */
    public static int minutosConfigurados() {
        try {
            return Integer.parseInt(Tema.leerPreferencia(PREFERENCIA_MINUTOS, String.valueOf(MINUTOS_POR_DEFECTO)));
        } catch (NumberFormatException e) {
            return MINUTOS_POR_DEFECTO;
        }
    }

    public static void guardarMinutos(int minutos) {
        Tema.guardarPreferencia(PREFERENCIA_MINUTOS, String.valueOf(minutos));
    }

    public void iniciar() {
        oyenteActividad = evento -> {
            if (capa == null) {
                Object origen = evento.getSource();
                Window w = origen instanceof Component ? SwingUtilities.getWindowAncestor((Component) origen) : null;
                if (origen == ventana || w == ventana) {
                    ultimaActividad = System.currentTimeMillis();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(oyenteActividad,
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        vigilante = new Timer(15_000, e -> {
            int minutos = minutosConfigurados();
            boolean inactivo = System.currentTimeMillis() - ultimaActividad > minutos * 60_000L;
            if (minutos > 0 && inactivo && capa == null && ventana.isShowing() && puedeBloquearPorInactividad.getAsBoolean()) {
                bloquear(true);
            }
        });
        vigilante.start();
    }

    public void detener() {
        if (vigilante != null) {
            vigilante.stop();
        }
        if (oyenteActividad != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(oyenteActividad);
        }
    }

    public boolean estaBloqueada() {
        return capa != null;
    }

    /** Muestra la pantalla de bloqueo (si no está ya). */
    public void bloquear(boolean porInactividad) {
        if (capa != null || !Sesion.estaActiva()) {
            return;
        }
        intentosFallidos = 0;
        JRootPane raiz = ventana.getRootPane();
        JLayeredPane capas = raiz.getLayeredPane();

        JPanel fondo = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Tema.SECUNDARIO, getWidth(), getHeight(), Tema.PRIMARIO));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        fondo.setOpaque(true);
        // Bloquea clics y movimiento del mouse hacia lo de abajo.
        fondo.addMouseListener(new java.awt.event.MouseAdapter() { });
        fondo.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });

        JPanel tarjeta = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(4, 8, getWidth() - 8, getHeight() - 8, 22, 22);
                g2.setColor(Tema.SUPERFICIE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 8, 22, 22);
                g2.dispose();
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(30, 38, 34, 42));
        tarjeta.setFocusCycleRoot(true); // Tab no se escapa a los componentes de abajo

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 6, 0);

        if (Tema.hayLogo()) {
            gbc.fill = GridBagConstraints.NONE;
            tarjeta.add(Tema.etiquetaLogo(72), gbc);
            gbc.fill = GridBagConstraints.HORIZONTAL;
        }

        JLabel lblTitulo = new JLabel("Sesión bloqueada", SwingConstants.CENTER);
        lblTitulo.setFont(Tema.fuente(Font.BOLD, 22));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        gbc.insets = new Insets(10, 0, 2, 0);
        tarjeta.add(lblTitulo, gbc);

        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        JLabel lblDetalle = new JLabel("<html><div style='text-align:center'>" + Sesion.getNombre() + " · "
            + Tema.etiquetaRol(Sesion.getRol()) + "<br>"
            + (porInactividad ? "Se bloqueó por inactividad a las " : "Bloqueada a las ") + hora + "</div></html>",
            SwingConstants.CENTER);
        lblDetalle.setFont(Tema.TEXTO_CHICO);
        lblDetalle.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.insets = new Insets(0, 0, 18, 0);
        tarjeta.add(lblDetalle, gbc);

        JLabel lblPassword = new JLabel("Contraseña");
        lblPassword.setFont(Tema.fuente(Font.BOLD, 12));
        lblPassword.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.insets = new Insets(0, 0, 4, 0);
        tarjeta.add(lblPassword, gbc);

        JPasswordField txtPassword = new JPasswordField(22);
        txtPassword.putClientProperty("JTextField.placeholderText", "Ingresá tu contraseña para seguir");
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);
        txtPassword.setFont(Tema.fuente(Font.PLAIN, 15));
        gbc.insets = new Insets(0, 0, 6, 0);
        tarjeta.add(txtPassword, gbc);

        JLabel lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setFont(Tema.fuente(Font.BOLD, 12));
        lblError.setForeground(Tema.PELIGRO);
        tarjeta.add(lblError, gbc);

        JButton btnDesbloquear = Tema.botonPrimario("Desbloquear");
        gbc.insets = new Insets(6, 0, 10, 0);
        tarjeta.add(btnDesbloquear, gbc);

        boolean conCambios = hayCambiosSinGuardar.getAsBoolean();
        if (conCambios) {
            JLabel lblCambios = new JLabel("<html><div style='text-align:center'>Tenés datos sin guardar: "
                + "se conservan al desbloquear.</div></html>", SwingConstants.CENTER);
            lblCambios.setFont(Tema.TEXTO_CHICO);
            lblCambios.setForeground(Tema.TEXTO_SECUNDARIO);
            gbc.insets = new Insets(0, 0, 8, 0);
            tarjeta.add(lblCambios, gbc);
        }

        JButton btnCambiarUsuario = Tema.botonSecundario("Cerrar sesión / entrar con otro usuario");
        btnCambiarUsuario.setToolTipText("Vuelve a la pantalla de ingreso");
        gbc.insets = new Insets(0, 0, 0, 0);
        tarjeta.add(btnCambiarUsuario, gbc);

        fondo.add(tarjeta);

        Runnable ajustar = () -> fondo.setBounds(0, 0, capas.getWidth(), capas.getHeight());
        java.awt.event.ComponentAdapter alRedimensionar = new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ajustar.run();
                fondo.revalidate();
            }
        };
        ajustar.run();
        capas.addComponentListener(alRedimensionar);
        // Por encima de todo: pantallas, transiciones (DRAG_LAYER) y hojas (MODAL_LAYER).
        capas.add(fondo, Integer.valueOf(JLayeredPane.DRAG_LAYER + 100));
        Tema.registrarCapaModal(raiz, fondo);
        fondo.putClientProperty("alRedimensionar", alRedimensionar);
        capa = fondo;
        capas.revalidate();
        capas.repaint();
        registrarAuditoria("BLOQUEO_SESION", porInactividad ? "Por inactividad" : "Manual");

        // --- Acciones ---
        Runnable desbloquear = () -> {
            char[] ingresada = txtPassword.getPassword();
            if (ingresada.length == 0) {
                lblError.setText("Ingresá tu contraseña");
                return;
            }
            String password = new String(ingresada);
            java.util.Arrays.fill(ingresada, '\0');
            btnDesbloquear.setEnabled(false);
            Tema.enSegundoPlano(
                fondo,
                () -> new dao.UsuarioDAO().autenticar(Sesion.getUsuario(), password) != null,
                correcta -> {
                    btnDesbloquear.setEnabled(true);
                    if (correcta) {
                        registrarAuditoria("DESBLOQUEO_SESION", null);
                        quitar();
                        return;
                    }
                    intentosFallidos++;
                    txtPassword.setText("");
                    if (intentosFallidos >= MAX_INTENTOS) {
                        registrarAuditoria("BLOQUEO_SESION", MAX_INTENTOS + " contraseñas incorrectas: sesión cerrada");
                        quitar();
                        alCerrarSesion.run();
                        return;
                    }
                    int restantes = MAX_INTENTOS - intentosFallidos;
                    lblError.setText("Contraseña incorrecta" + (restantes <= 2
                        ? " · " + restantes + (restantes == 1 ? " intento más" : " intentos más") + " y se cierra la sesión" : ""));
                    txtPassword.requestFocusInWindow();
                },
                error -> {
                    btnDesbloquear.setEnabled(true);
                    lblError.setText("No se pudo verificar: " + error.getMessage());
                }
            );
        };
        btnDesbloquear.addActionListener(e -> desbloquear.run());
        txtPassword.addActionListener(e -> desbloquear.run());

        final boolean[] confirmando = {false};
        btnCambiarUsuario.addActionListener(e -> {
            if (conCambios && !confirmando[0]) {
                // Doble paso: la persona que está frente a la PC puede no ser quien dejó los cambios.
                confirmando[0] = true;
                btnCambiarUsuario.setText("Confirmar: cerrar sesión y descartar lo no guardado");
                lblError.setText("Se van a perder los datos sin guardar");
                return;
            }
            quitar();
            alCerrarSesion.run();
        });

        SwingUtilities.invokeLater(txtPassword::requestFocusInWindow);
    }

    private void quitar() {
        if (capa == null) {
            return;
        }
        JRootPane raiz = ventana.getRootPane();
        JLayeredPane capas = raiz.getLayeredPane();
        Object oyente = capa.getClientProperty("alRedimensionar");
        if (oyente instanceof java.awt.event.ComponentListener) {
            capas.removeComponentListener((java.awt.event.ComponentListener) oyente);
        }
        Tema.quitarCapaModal(raiz, capa);
        capas.remove(capa);
        capas.revalidate();
        capas.repaint();
        capa = null;
        ultimaActividad = System.currentTimeMillis();
    }

    /** En el momento (no en un hilo): si la sesión se cierra enseguida, el autor tiene que quedar registrado. */
    private static void registrarAuditoria(String accion, String detalle) {
        try (Connection cn = new conexion.Conexion().conectar()) {
            Auditoria.registrar(cn, accion, "usuarios", Sesion.getUsuarioId(), detalle);
        } catch (Exception e) {
            System.out.println("Error registrando auditoría: " + e.getMessage());
        }
    }

    /** Ctrl+L: registrar en la ventana para bloquear a mano. */
    public static KeyStroke atajo() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK);
    }
}
