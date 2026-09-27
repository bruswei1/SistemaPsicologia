package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import util.Auditoria;
import util.PasswordUtil;
import util.Sesion;

public class Login extends javax.swing.JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public Login() {
        setTitle("Iniciar sesión — Departamento de Psicología · Colegio San Roque González");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setIconImage(Tema.iconoApp());
        initComponents();

        getRootPane().setDefaultButton(btnLogin);
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JPanel panelHero = crearPanelHero();
        JPanel panelForm = crearPanelForm();

        // Tarjeta = hero + form apilados, con sombra y esquinas redondeadas propias. Antes el
        // formulario iba a todo el ancho de la ventana, sin ningún fondo que lo diferencie del
        // resto del escritorio — ahora "flota" como una tarjeta más, igual que el resto de la app.
        JPanel tarjeta = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // El fondo/relleno de la tarjeta se pinta en paint() (recortado a la forma
                // redondeada); acá no hay nada que pintar antes de los hijos.
            }

            @Override
            public void paint(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Sombra sin recortar (tiene que sobresalir de la forma de la tarjeta) antes de
                // fijar el clip a la forma redondeada.
                for (int i = 16; i >= 1; i--) {
                    g2.setColor(new Color(20, 18, 35, 2));
                    g2.fillRoundRect(-i, -i + 10, w + i * 2, h + i * 2, 30, 30);
                }

                // A partir de acá todo (fondo + hero + formulario) se recorta a la forma
                // redondeada, así el degradé rectangular del hero no se sale por las esquinas.
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));
                g2.setColor(Tema.SUPERFICIE);
                g2.fillRect(0, 0, w, h);
                super.paint(g2);
                g2.dispose();
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.add(panelHero, BorderLayout.NORTH);
        tarjeta.add(panelForm, BorderLayout.CENTER);

        // Fondo de toda la ventana: un degradé tenue de la misma familia de color en vez de
        // blanco/gris liso, para que la tarjeta se note como un elemento flotante y no como "todo
        // el contenido de la ventana".
        JPanel fondo = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = Math.max(1, getWidth());
                int h = Math.max(1, getHeight());
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new java.awt.GradientPaint(
                    0, 0, Tema.FONDO, w, h, Tema.aclarar(Tema.SECUNDARIO, 0.85f)));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        fondo.setBorder(BorderFactory.createEmptyBorder(56, 64, 56, 64));
        fondo.add(tarjeta);

        getContentPane().add(fondo, BorderLayout.CENTER);
    }

    private JPanel crearPanelForm() {
        JPanel panelForm = new JPanel();
        panelForm.setOpaque(false);
        panelForm.setLayout(new BoxLayout(panelForm, BoxLayout.Y_AXIS));
        panelForm.setBorder(BorderFactory.createEmptyBorder(30, 40, 34, 40));

        txtUsuario = new JTextField(20);
        txtPassword = new JPasswordField(20);

        panelForm.add(crearCampoEtiquetado("Usuario", txtUsuario));
        panelForm.add(Box.createVerticalStrut(16));
        panelForm.add(crearCampoEtiquetado("Contraseña", txtPassword));
        panelForm.add(Box.createVerticalStrut(24));

        btnLogin = Tema.botonPrimario("Iniciar sesión");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setFont(Tema.fuente(java.awt.Font.BOLD, 14));
        btnLogin.addActionListener(evt -> autenticar());
        txtPassword.addActionListener(evt -> autenticar());
        panelForm.add(btnLogin);

        return panelForm;
    }

    /**
     * Campo de formulario con la etiqueta arriba (en vez de a la izquierda) a todo el ancho de
     * la tarjeta. Usa BorderLayout (NORTH=etiqueta, CENTER=campo) en vez de BoxLayout: un panel
     * con BoxLayout anidado dentro de otro BoxLayout no siempre hereda el "soy elástico" de sus
     * hijos hacia arriba (quedaba angosto y corrido a la derecha aunque el campo interno ya
     * tuviera maximumSize infinito) — BorderLayout.CENTER siempre ocupa el ancho completo, sin
     * ambigüedad de alineación.
     */
    private JPanel crearCampoEtiquetado(String etiqueta, javax.swing.JTextField campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(Tema.fuente(java.awt.Font.BOLD, 12));
        lbl.setForeground(Tema.TEXTO_SECUNDARIO);

        campo.setFont(Tema.TEXTO);
        campo.setBorder(BorderFactory.createCompoundBorder(
            Tema.bordeCampo(),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        panel.add(lbl, BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    /** Franja superior con degradé de marca (mismos colores del cartel real del Departamento). */
    private JPanel crearPanelHero() {
        JPanel panel = Tema.panelDegradado();
        panel.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));

        if (Tema.hayLogo()) {
            javax.swing.JComponent insignia = Tema.insigniaLogo(96);
            insignia.setAlignmentX(Component.CENTER_ALIGNMENT);
            columna.add(insignia);
            columna.add(Box.createVerticalStrut(16));
        }

        JLabel lblTitulo = new JLabel("Departamento de Psicología", SwingConstants.CENTER);
        lblTitulo.setFont(Tema.TITULO);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblInstitucion = new JLabel(
            "Colegio Nacional E.M.D. San Roque González de Santacruz", SwingConstants.CENTER);
        lblInstitucion.setFont(Tema.TEXTO_CHICO);
        lblInstitucion.setForeground(new Color(255, 255, 255, 215));
        lblInstitucion.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Iniciar sesión", SwingConstants.CENTER);
        lblSubtitulo.setFont(Tema.TEXTO);
        lblSubtitulo.setForeground(new Color(255, 255, 255, 215));
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        columna.add(lblTitulo);
        columna.add(Box.createVerticalStrut(6));
        columna.add(lblInstitucion);
        columna.add(Box.createVerticalStrut(6));
        columna.add(lblSubtitulo);

        panel.add(columna, BorderLayout.CENTER);
        return panel;
    }

    private void autenticar() {

        String usuario = txtUsuario.getText().trim();
        String password = String.valueOf(txtPassword.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Ingresa usuario y contraseña",
                "Advertencia",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btnLogin.setEnabled(false);

        Tema.enSegundoPlano(
            this,
            () -> autenticarEnBaseDeDatos(usuario, password),
            resultado -> {
                btnLogin.setEnabled(true);
                if (resultado.autenticado) {
                    Sesion.iniciar(resultado.id, usuario, resultado.nombre, resultado.rol);

                    MenuPrincipal menu = new MenuPrincipal();
                    menu.setVisible(true);
                    this.dispose();
                } else if (resultado.bloqueada) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Esta cuenta quedó bloqueada temporalmente por varios intentos fallidos seguidos.\n"
                            + "Volvé a intentarlo en " + resultado.minutosRestantesBloqueo + " minuto(s).",
                        "Cuenta bloqueada",
                        JOptionPane.WARNING_MESSAGE
                    );
                    txtPassword.setText("");
                } else {
                    String mensaje = "Usuario o contraseña incorrectos";
                    if (resultado.intentosRestantes >= 0) {
                        mensaje += "\n(" + resultado.intentosRestantes + " intento(s) más antes de bloquear la cuenta)";
                    }
                    JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
                    txtPassword.setText("");
                }
            },
            error -> {
                btnLogin.setEnabled(true);
                JOptionPane.showMessageDialog(
                    this,
                    "Error: " + error.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        );
    }

    /**
     * Después de esta cantidad de intentos fallidos SEGUIDOS, la cuenta se bloquea por
     * {@link #MINUTOS_BLOQUEO} minutos — antes no había ningún límite, se podía probar
     * contraseñas sin parar.
     */
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    /**
     * Intentos fallidos contra nombres de usuario que NO existen, llevados en memoria (no hay fila
     * en la base donde guardarlos): {intentos, bloqueado hasta (ms)}. Solo sirve para que la
     * respuesta sea indistinguible de la de una cuenta real.
     */
    private static final java.util.Map<String, long[]> INTENTOS_USUARIOS_INEXISTENTES = new java.util.HashMap<>();

    private static ResultadoLogin simularIntentoFallido(String usuario) {
        ResultadoLogin resultado = new ResultadoLogin();
        long ahora = System.currentTimeMillis();
        synchronized (INTENTOS_USUARIOS_INEXISTENTES) {
            long[] estado = INTENTOS_USUARIOS_INEXISTENTES.computeIfAbsent(usuario.toLowerCase(), k -> new long[2]);
            if (estado[1] > ahora) {
                resultado.bloqueada = true;
                resultado.minutosRestantesBloqueo = Math.max(1, (estado[1] - ahora + 59_999) / 60_000);
                return resultado;
            }
            estado[0]++;
            if (estado[0] >= MAX_INTENTOS_FALLIDOS) {
                estado[0] = 0;
                estado[1] = ahora + MINUTOS_BLOQUEO * 60_000L;
                resultado.bloqueada = true;
                resultado.minutosRestantesBloqueo = MINUTOS_BLOQUEO;
            } else {
                int restantes = MAX_INTENTOS_FALLIDOS - (int) estado[0];
                if (restantes <= 2) {
                    resultado.intentosRestantes = restantes;
                }
            }
        }
        return resultado;
    }

    private static final class ResultadoLogin {
        boolean autenticado;
        boolean bloqueada;
        long minutosRestantesBloqueo;
        /** -1 = no corresponde avisar; solo se completa en los últimos intentos antes de bloquear. */
        int intentosRestantes = -1;
        int id;
        String nombre;
        String rol;
    }

    /**
     * Corre en el hilo de fondo de {@link Tema#enSegundoPlano} — sin tocar componentes Swing acá.
     * El conteo de intentos fallidos y el bloqueo se guardan en la base (no en memoria del
     * proceso): varios usuarios inician sesión desde computadoras distintas contra la misma base,
     * así que el conteo tiene que ser compartido y sobrevivir a un reinicio de la aplicación.
     */
    private ResultadoLogin autenticarEnBaseDeDatos(String usuario, String password) throws Exception {
        ResultadoLogin resultado = new ResultadoLogin();

        Conexion c = new Conexion();

        try (Connection cn = c.conectar()) {

            if (cn == null) {
                throw new IllegalStateException("No se pudo conectar a la base de datos");
            }

            Integer id = null;
            String nombre = null;
            String rol = null;
            String passwordHash = null;
            String salt = null;
            boolean activo = false;
            int intentosFallidos = 0;
            Timestamp bloqueadoHasta = null;

            String sql = "SELECT id, nombre, rol, password_hash, salt, activo, intentos_fallidos, bloqueado_hasta "
                + "FROM usuarios WHERE usuario=?";

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, usuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt("id");
                        nombre = rs.getString("nombre");
                        rol = rs.getString("rol");
                        passwordHash = rs.getString("password_hash");
                        salt = rs.getString("salt");
                        activo = rs.getBoolean("activo");
                        intentosFallidos = rs.getInt("intentos_fallidos");
                        bloqueadoHasta = rs.getTimestamp("bloqueado_hasta");
                    }
                }
            }

            if (id == null) {
                // Usuario inexistente: se responde EXACTAMENTE igual que a una cuenta real con
                // contraseña incorrecta (aviso de intentos restantes y bloqueo incluidos). Antes el
                // aviso "te quedan N intentos" solo aparecía para cuentas reales, y eso permitía
                // averiguar qué nombres de usuario existen.
                return simularIntentoFallido(usuario);
            }

            if (bloqueadoHasta != null && bloqueadoHasta.getTime() > System.currentTimeMillis()) {
                resultado.bloqueada = true;
                resultado.minutosRestantesBloqueo =
                    Math.max(1, (bloqueadoHasta.getTime() - System.currentTimeMillis() + 59_999) / 60_000);
                return resultado;
            }

            boolean passwordCorrecta = activo && PasswordUtil.verificar(password, salt, passwordHash);

            if (passwordCorrecta) {
                resultado.autenticado = true;
                resultado.id = id;
                resultado.nombre = nombre;
                resultado.rol = rol;

                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                Auditoria.registrar(cn, "LOGIN", "usuarios", id, null);

            } else {
                int nuevosIntentos = intentosFallidos + 1;

                if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
                    // Hora de la PC, no NOW() de MySQL: el contenedor de MySQL corre en UTC y la
                    // PC en hora local de Paraguay — mismo desfase ya resuelto en Agenda/Auditoria
                    // seteando la marca de tiempo desde Java en vez de dejarla del lado del server.
                    Timestamp bloqueoHastaNuevo =
                        Timestamp.valueOf(java.time.LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                    try (PreparedStatement ps = cn.prepareStatement(
                            "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = ? WHERE id = ?")) {
                        ps.setTimestamp(1, bloqueoHastaNuevo);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }
                    Auditoria.registrar(cn, "CUENTA_BLOQUEADA", "usuarios", id,
                        MAX_INTENTOS_FALLIDOS + " intentos fallidos seguidos");
                    resultado.bloqueada = true;
                    resultado.minutosRestantesBloqueo = MINUTOS_BLOQUEO;
                } else {
                    try (PreparedStatement ps = cn.prepareStatement(
                            "UPDATE usuarios SET intentos_fallidos = ? WHERE id = ?")) {
                        ps.setInt(1, nuevosIntentos);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }
                    int restantes = MAX_INTENTOS_FALLIDOS - nuevosIntentos;
                    if (restantes <= 2) {
                        resultado.intentosRestantes = restantes;
                    }
                }
            }
        }

        return resultado;
    }

    public static void main(String args[]) {
        Tema.instalarLookAndFeelGuardado();

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            }
        });
    }
}
