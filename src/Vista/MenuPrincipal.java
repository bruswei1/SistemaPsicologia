package Vista;

import javax.swing.*;
import java.awt.*;

/**
 * Shell de la aplicación: una única ventana con un CardLayout que va mostrando
 * cada pantalla (Pacientes, Sesiones, Historia, Dashboard, Agenda) en vez de
 * abrir un JFrame nuevo por cada una. Las pantallas se crean una sola vez
 * (perezosamente, al navegar a ellas por primera vez) y se refrescan cada vez
 * que se vuelve a mostrar esa tarjeta.
 */
public class MenuPrincipal extends javax.swing.JFrame {

    private static final String CARTA_INICIO = "inicio";

    private CardLayout cardLayout;
    private JPanel contenedor;

    private GestionPacientes panelPacientes;
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

        setTitle("Departamento de Psicología · Colegio San Roque González — " + usuario
            + " (" + Tema.etiquetaRol(rol) + ")");
        setIconImage(Tema.iconoApp());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        panelPacientes = null;
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
        String usuarioInfo = "Usuario";
        String rolInfo = "Admin";
        try {
            usuarioInfo = util.Sesion.getNombre();
            rolInfo = util.Sesion.getRol();
        } catch (Exception e) {
            // Default
        }

        return Tema.panelEncabezado(
            "Departamento de Psicología · Colegio San Roque González",
            "Conectado como: " + usuarioInfo + " (" + Tema.etiquetaRol(rolInfo) + ")");
    }

    private JPanel crearGridOpciones() {
        JPanel panelContent = new JPanel();
        panelContent.setBackground(Tema.FONDO);
        panelContent.setLayout(new GridLayout(2, 3, 30, 30));
        panelContent.setBorder(BorderFactory.createEmptyBorder(
            Tema.ESPACIADO_GRANDE, 50, Tema.ESPACIADO_GRANDE, 50));

        panelContent.add(new TarjetaMenu(Icono.PACIENTES, Tema.ACENTO_AZUL,
            "Estudiantes", "Registrar y gestionar\nestudiantes",
            this::mostrarPacientes));

        panelContent.add(new TarjetaMenu(Icono.SESIONES, Tema.ACENTO_MORADO,
            "Atenciones", "Registrar atenciones\npsicológicas",
            this::mostrarSesiones));

        panelContent.add(new TarjetaMenu(Icono.HISTORIA, Tema.ACENTO_AMBAR,
            "Historia", "Ver y editar historia\npsicológica",
            this::mostrarHistoria));

        panelContent.add(new TarjetaMenu(Icono.DASHBOARD, Tema.ACENTO_CELESTE,
            "Panel General", "Estadísticas y\nreportes",
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

    private void mostrarSesiones() {
        boolean esSecretaria;
        try {
            esSecretaria = util.Sesion.esSecretaria();
        } catch (Exception e) {
            esSecretaria = false;
        }

        if (esSecretaria) {
            JOptionPane.showMessageDialog(this,
                "Las notas de atención no están disponibles para usuarios autorizados",
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
        boolean esSecretaria;
        try {
            esSecretaria = util.Sesion.esSecretaria();
        } catch (Exception e) {
            esSecretaria = false;
        }

        if (esSecretaria) {
            JOptionPane.showMessageDialog(this,
                "La historia psicológica no está disponible para usuarios autorizados",
                "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
            panelConfiguracion = new Configuracion(this::mostrarInicio, this::cerrarSesion, this::construirVentana);
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
