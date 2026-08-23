package Vista;

import javax.swing.*;
import java.awt.*;

public class MenuPrincipal extends javax.swing.JFrame {

    public MenuPrincipal() {
        initComponents();

        String usuario = "Usuario";
        String rol = "Admin";
        try {
            usuario = util.Sesion.getNombre();
            rol = util.Sesion.getRol();
        } catch (Exception e) {
            System.out.println("Sesión no disponible");
        }

        setTitle("Sistema de Psicología — " + usuario + " (" + rol + ")");
        setIconImage(Tema.iconoApp());
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initComponents() {
        setSize(1200, 800);

        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setBackground(Tema.FONDO);
        panelPrincipal.setLayout(new BorderLayout());

        panelPrincipal.add(crearHeader(), BorderLayout.NORTH);
        panelPrincipal.add(crearGridOpciones(), BorderLayout.CENTER);

        getContentPane().add(panelPrincipal);
    }

    private JPanel crearHeader() {
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(Tema.SUPERFICIE);
        panelHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE));
        panelHeader.setPreferredSize(new Dimension(0, 100));

        JPanel panelTextos = new JPanel();
        panelTextos.setOpaque(false);
        panelTextos.setLayout(new BoxLayout(panelTextos, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Sistema de Psicología");
        lblTitulo.setFont(Tema.TITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 0));
        panelTextos.add(lblTitulo);

        String usuarioInfo = "Usuario";
        String rolInfo = "Admin";
        try {
            usuarioInfo = util.Sesion.getNombre();
            rolInfo = util.Sesion.getRol();
        } catch (Exception e) {
            // Default
        }

        JLabel lblUsuario = new JLabel("Conectado como: " + usuarioInfo + " (" + rolInfo + ")");
        lblUsuario.setFont(Tema.TEXTO_CHICO);
        lblUsuario.setForeground(Tema.TEXTO_SECUNDARIO);
        lblUsuario.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 0));
        panelTextos.add(lblUsuario);

        panelHeader.add(panelTextos, BorderLayout.WEST);

        JPanel panelAcciones = new JPanel();
        panelAcciones.setOpaque(false);
        JButton btnModoOscuro = Tema.botonSecundario(Tema.esOscuro() ? "Modo claro" : "Modo oscuro");
        btnModoOscuro.addActionListener(e -> {
            Tema.alternarModoOscuro();
            abrirPantalla(new MenuPrincipal());
        });
        panelAcciones.add(btnModoOscuro);
        panelHeader.add(panelAcciones, BorderLayout.EAST);

        return panelHeader;
    }

    private JPanel crearGridOpciones() {
        JPanel panelContent = new JPanel();
        panelContent.setBackground(Tema.FONDO);
        panelContent.setLayout(new GridLayout(2, 3, 30, 30));
        panelContent.setBorder(BorderFactory.createEmptyBorder(
            Tema.ESPACIADO_GRANDE, 50, Tema.ESPACIADO_GRANDE, 50));

        panelContent.add(new TarjetaMenu(Icono.PACIENTES, Tema.ACENTO_AZUL,
            "Pacientes", "Registrar y gestionar\npacientes",
            () -> abrirPantalla(new GestionPacientes())));

        panelContent.add(new TarjetaMenu(Icono.TURNOS, Tema.ACENTO_VERDE,
            "Turnos", "Crear y gestionar\nturnos",
            () -> abrirPantalla(new GestionTurnos())));

        panelContent.add(new TarjetaMenu(Icono.SESIONES, Tema.ACENTO_MORADO,
            "Sesiones", "Registrar notas clínicas\nde sesiones",
            this::abrirSesiones));

        panelContent.add(new TarjetaMenu(Icono.HISTORIA, Tema.ACENTO_AMBAR,
            "Historia", "Ver y editar historia\npsicológica",
            () -> abrirPantalla(new HistoriaPsicologicaView())));

        panelContent.add(new TarjetaMenu(Icono.DASHBOARD, Tema.ACENTO_CELESTE,
            "Dashboard", "Estadísticas y\nreportes",
            () -> abrirPantalla(new Dashboard())));

        panelContent.add(new TarjetaMenu(Icono.AGENDA, Tema.ACENTO_ROSA,
            "Agenda", "Ver agenda de citas\npróximas",
            () -> abrirPantalla(new Agenda())));

        return panelContent;
    }

    private void abrirSesiones() {
        boolean esSecretaria;
        try {
            esSecretaria = util.Sesion.esSecretaria();
        } catch (Exception e) {
            esSecretaria = false;
        }

        if (esSecretaria) {
            JOptionPane.showMessageDialog(this,
                "Las notas clínicas no están disponibles para secretarias",
                "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        abrirPantalla(new Sesiones());
    }

    private void abrirPantalla(JFrame pantalla) {
        pantalla.setVisible(true);
        dispose();
    }

    public static void main(String args[]) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MenuPrincipal().setVisible(true);
            }
        });
    }
}
