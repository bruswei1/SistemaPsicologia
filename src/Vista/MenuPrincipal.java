package Vista;

import javax.swing.*;
import java.awt.*;

/**
 * Shell de la aplicación: una única ventana con un CardLayout que va mostrando
 * cada pantalla (Pacientes, Turnos, Sesiones, Historia, Dashboard, Agenda) en
 * vez de abrir un JFrame nuevo por cada una. Las pantallas se crean una sola
 * vez (perezosamente, al navegar a ellas por primera vez) y se refrescan cada
 * vez que se vuelve a mostrar esa tarjeta.
 */
public class MenuPrincipal extends javax.swing.JFrame {

    private static final String CARTA_INICIO = "inicio";

    private CardLayout cardLayout;
    private JPanel contenedor;

    private GestionPacientes panelPacientes;
    private GestionTurnos panelTurnos;
    private Sesiones panelSesiones;
    private HistoriaPsicologicaView panelHistoria;
    private Dashboard panelDashboard;
    private Agenda panelAgenda;
    private Configuracion panelConfiguracion;

    public MenuPrincipal() {
        construirVentana();
    }

    /**
     * (Re)construye todo el contenido de la ventana desde cero. Se usa tanto en el
     * constructor como al alternar modo oscuro: como los paneles ya creados quedan
     * pintados con la paleta vieja (setBackground copia el color, no queda "atado"
     * a Tema), la única forma de que TODO el shell adopte el tema nuevo sin abrir
     * una ventana distinta es tirar el contenido viejo y recrearlo en la misma
     * ventana.
     */
    private void construirVentana() {
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
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        panelPacientes = null;
        panelTurnos = null;
        panelSesiones = null;
        panelHistoria = null;
        panelDashboard = null;
        panelAgenda = null;
        panelConfiguracion = null;

        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);
        contenedor.add(crearInicio(), CARTA_INICIO);

        getContentPane().removeAll();
        getContentPane().add(contenedor);
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private JPanel crearInicio() {
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setBackground(Tema.FONDO);
        panelPrincipal.setLayout(new BorderLayout());

        panelPrincipal.add(crearHeader(), BorderLayout.NORTH);
        panelPrincipal.add(crearGridOpciones(), BorderLayout.CENTER);

        return panelPrincipal;
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
            construirVentana();
        });
        panelAcciones.add(btnModoOscuro);
        panelHeader.add(panelAcciones, BorderLayout.EAST);

        return panelHeader;
    }

    private JPanel crearGridOpciones() {
        JPanel panelContent = new JPanel();
        panelContent.setBackground(Tema.FONDO);
        panelContent.setLayout(new GridLayout(2, 4, 30, 30));
        panelContent.setBorder(BorderFactory.createEmptyBorder(
            Tema.ESPACIADO_GRANDE, 50, Tema.ESPACIADO_GRANDE, 50));

        panelContent.add(new TarjetaMenu(Icono.PACIENTES, Tema.ACENTO_AZUL,
            "Pacientes", "Registrar y gestionar\npacientes",
            this::mostrarPacientes));

        panelContent.add(new TarjetaMenu(Icono.TURNOS, Tema.ACENTO_VERDE,
            "Turnos", "Crear y gestionar\nturnos",
            this::mostrarTurnos));

        panelContent.add(new TarjetaMenu(Icono.SESIONES, Tema.ACENTO_MORADO,
            "Sesiones", "Registrar notas clínicas\nde sesiones",
            this::mostrarSesiones));

        panelContent.add(new TarjetaMenu(Icono.HISTORIA, Tema.ACENTO_AMBAR,
            "Historia", "Ver y editar historia\npsicológica",
            this::mostrarHistoria));

        panelContent.add(new TarjetaMenu(Icono.DASHBOARD, Tema.ACENTO_CELESTE,
            "Dashboard", "Estadísticas y\nreportes",
            this::mostrarDashboard));

        panelContent.add(new TarjetaMenu(Icono.AGENDA, Tema.ACENTO_ROSA,
            "Agenda", "Ver agenda de citas\npróximas",
            this::mostrarAgenda));

        panelContent.add(new TarjetaMenu(Icono.CONFIGURACION, Tema.ACENTO_GRIS,
            "Configuración", "Cuenta, contraseña\ny usuarios",
            this::mostrarConfiguracion));

        return panelContent;
    }

    private void mostrarInicio() {
        cardLayout.show(contenedor, CARTA_INICIO);
    }

    private void mostrarPacientes() {
        if (panelPacientes == null) {
            panelPacientes = new GestionPacientes(this::mostrarInicio);
            contenedor.add(panelPacientes, "pacientes");
        } else {
            panelPacientes.refrescar();
        }
        cardLayout.show(contenedor, "pacientes");
    }

    private void mostrarTurnos() {
        if (panelTurnos == null) {
            panelTurnos = new GestionTurnos(this::mostrarInicio);
            contenedor.add(panelTurnos, "turnos");
        } else {
            panelTurnos.refrescar();
        }
        cardLayout.show(contenedor, "turnos");
    }

    private void mostrarSesiones() {
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

        if (panelSesiones == null) {
            panelSesiones = new Sesiones(this::mostrarInicio);
            contenedor.add(panelSesiones, "sesiones");
        } else {
            panelSesiones.refrescar();
        }
        cardLayout.show(contenedor, "sesiones");
    }

    private void mostrarHistoria() {
        if (panelHistoria == null) {
            panelHistoria = new HistoriaPsicologicaView(this::mostrarInicio);
            contenedor.add(panelHistoria, "historia");
        } else {
            panelHistoria.refrescar();
        }
        cardLayout.show(contenedor, "historia");
    }

    private void mostrarDashboard() {
        if (panelDashboard == null) {
            panelDashboard = new Dashboard(this::mostrarInicio);
            contenedor.add(panelDashboard, "dashboard");
        } else {
            panelDashboard.refrescar();
        }
        cardLayout.show(contenedor, "dashboard");
    }

    private void mostrarAgenda() {
        if (panelAgenda == null) {
            panelAgenda = new Agenda(this::mostrarInicio);
            contenedor.add(panelAgenda, "agenda");
        } else {
            panelAgenda.refrescar();
        }
        cardLayout.show(contenedor, "agenda");
    }

    private void mostrarConfiguracion() {
        if (panelConfiguracion == null) {
            panelConfiguracion = new Configuracion(this::mostrarInicio, this::cerrarSesion);
            contenedor.add(panelConfiguracion, "configuracion");
        } else {
            panelConfiguracion.refrescar();
        }
        cardLayout.show(contenedor, "configuracion");
    }

    private void cerrarSesion() {
        util.Sesion.cerrar();
        dispose();
        new Login().setVisible(true);
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
