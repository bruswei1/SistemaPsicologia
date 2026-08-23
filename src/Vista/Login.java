package Vista;

import conexion.Conexion;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
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
        setTitle("Iniciar sesión — Sistema de Psicología");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setIconImage(Tema.iconoApp());
        initComponents();

        getRootPane().setDefaultButton(btnLogin);
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JLabel lblTitulo = new JLabel("Sistema de Psicología", SwingConstants.CENTER);
        lblTitulo.setFont(Tema.TITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);

        JLabel lblSubtitulo = new JLabel("Iniciar sesión", SwingConstants.CENTER);
        lblSubtitulo.setFont(Tema.TEXTO);
        lblSubtitulo.setForeground(Tema.TEXTO_SECUNDARIO);

        JPanel panelTitulo = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        panelTitulo.setBackground(Tema.SUPERFICIE);
        panelTitulo.add(lblTitulo);
        panelTitulo.add(lblSubtitulo);
        panelTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Tema.SUPERFICIE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsuario = new JTextField(18);
        txtPassword = new JPasswordField(18);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panelForm.add(new JLabel("Usuario"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panelForm.add(txtUsuario, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panelForm.add(new JLabel("Contraseña"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panelForm.add(txtPassword, gbc);

        btnLogin = Tema.botonPrimario("Iniciar sesión");
        btnLogin.addActionListener(evt -> autenticar());
        txtPassword.addActionListener(evt -> autenticar());

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 6, 6, 6);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(btnLogin, gbc);

        JPanel panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBackground(Tema.SUPERFICIE);
        panelContenido.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));
        panelContenido.add(panelTitulo, BorderLayout.NORTH);
        panelContenido.add(panelForm, BorderLayout.CENTER);

        getContentPane().add(panelContenido, BorderLayout.CENTER);
    }

    private void autenticar() {

        String usuario = txtUsuario.getText().trim();
        String password = String.valueOf(txtPassword.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Ingresa usuario y contraseña"
            );
            return;
        }

        btnLogin.setEnabled(false);

        try {

            Conexion c = new Conexion();

            try (Connection cn = c.conectar()) {

                if (cn == null) {
                    throw new IllegalStateException("No se pudo conectar a la base de datos");
                }

                String sql =
                    "SELECT id, nombre, rol, password_hash, salt, activo FROM usuarios WHERE usuario=?";

                try (PreparedStatement ps = cn.prepareStatement(sql)) {

                    ps.setString(1, usuario);

                    try (ResultSet rs = ps.executeQuery()) {

                        boolean autenticado = false;
                        int id = 0;
                        String nombre = null;
                        String rol = null;

                        if (rs.next()) {
                            boolean activo = rs.getBoolean("activo");
                            autenticado = activo && PasswordUtil.verificar(
                                password,
                                rs.getString("salt"),
                                rs.getString("password_hash")
                            );
                            id = rs.getInt("id");
                            nombre = rs.getString("nombre");
                            rol = rs.getString("rol");
                        }

                        if (autenticado) {
                            Sesion.iniciar(id, usuario, nombre, rol);
                            Auditoria.registrar(cn, "LOGIN", "usuarios", id, null);

                            MenuPrincipal menu = new MenuPrincipal();
                            menu.setVisible(true);
                            this.dispose();
                        } else {
                            JOptionPane.showMessageDialog(
                                this,
                                "Usuario o contraseña incorrectos"
                            );
                            txtPassword.setText("");
                        }
                    }
                }
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Error: " + e.getMessage()
            );

        } finally {
            btnLogin.setEnabled(true);
        }
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
