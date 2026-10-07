package Vista;

import javax.swing.*;
import java.awt.*;

/**
 * Shell de la aplicación: una única ventana con un CardLayout que va mostrando
 * cada sección (Estudiantes, Panel, Configuración) en vez de abrir un JFrame
 * nuevo por cada una. Las pantallas se crean una sola vez (perezosamente, al
 * navegar a ellas por primera vez) y se refrescan cada vez que se vuelve a
 * mostrar esa tarjeta.
 */
public class MenuPrincipal extends javax.swing.JFrame {

    private static final String CARTA_INICIO = "inicio";

    private CardLayout cardLayout;
    private JPanel contenedor;

    private EstudiantesPanel panelEstudiantes;
    private Dashboard panelDashboard;
    private Configuracion panelConfiguracion;
    private ResumenDelDia resumenDelDia;

    /** Citas de hoy ya avisadas con el recordatorio de "en 10 minutos" (no se repite el aviso). */
    private final java.util.Set<Integer> citasAvisadas = new java.util.HashSet<>();
    private static final int MINUTOS_AVISO_PREVIO = 10;
    private Timer timerRecordatorios;
    private BloqueoPantalla bloqueo;

    public MenuPrincipal() {
        construirVentana();
        habilitarAtajoPantallaCompleta();
        Tema.atajo(getRootPane(), KeyStroke.getKeyStroke("control K"), this::abrirBusquedaRapida);
        // Bloqueo por inactividad (no mientras corre el cronómetro de una atención: el profesional
        // puede estar hablando con el estudiante sin tocar la PC) y a mano con Ctrl+L.
        bloqueo = new BloqueoPantalla(this,
            () -> panelEstudiantes == null || !panelEstudiantes.hayAtencionEnCurso(),
            this::hayCambiosSinGuardar,
            this::cerrarSesionSinPreguntar);
        bloqueo.iniciar();
        Tema.atajo(getRootPane(), BloqueoPantalla.atajo(), () -> bloqueo.bloquear(false));
        respaldarSiHaceFalta();
        iniciarRecordatoriosDeCitas();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                salirDelSistema();
            }
        });
    }

    /** true si en alguna sección quedó algo escrito sin guardar (atención, seguimiento, estudiante). */
    private boolean hayCambiosSinGuardar() {
        return panelEstudiantes != null && panelEstudiantes.hayCambiosSinGuardar();
    }

    /**
     * Antes de cerrar la ventana, cerrar sesión o recargar la ventana (tema / nombre), pregunta
     * si hay algo sin guardar. Antes todo eso descartaba en silencio, por ejemplo, una atención
     * a medio escribir.
     */
    private void siNoHayCambiosONoImporta(String titulo, String textoConfirmar, Runnable accion) {
        if (!hayCambiosSinGuardar()) {
            accion.run();
            return;
        }
        Tema.confirmar(this, titulo,
            "Hay datos escritos que todavía no se guardaron (una atención, una entrada de seguimiento o un "
                + "estudiante a medio cargar). Si seguís, se pierden.",
            textoConfirmar, "Volver", accion);
    }

    private void salirDelSistema() {
        siNoHayCambiosONoImporta("¿Cerrar el sistema?", "Cerrar sin guardar", () -> System.exit(0));
    }

    /** Recarga la ventana (p. ej. para cambiar el tema o mostrar el nombre nuevo) sin perder cambios sin avisar. */
    private void reconstruirVentanaSiSePuede(Runnable antes) {
        siNoHayCambiosONoImporta("¿Recargar la ventana?", "Recargar sin guardar", () -> {
            antes.run();
            construirVentana();
        });
    }

    /**
     * Al entrar, un aviso con las citas de hoy; después, cada minuto (esté en la pantalla que
     * esté), un recordatorio {@value #MINUTOS_AVISO_PREVIO} minutos antes de cada cita
     * programada. Una sola consulta liviana por minuto, que también mantiene al día la franja
     * "Hoy" del menú.
     */
    private void iniciarRecordatoriosDeCitas() {
        Timer bienvenida = new Timer(1500, e -> revisarCitasDeHoy(true));
        bienvenida.setRepeats(false);
        bienvenida.start();
        timerRecordatorios = new Timer(60_000, e -> revisarCitasDeHoy(false));
        timerRecordatorios.start();
    }

    private void revisarCitasDeHoy(boolean esBienvenida) {
        if (!util.Sesion.estaActiva()) {
            return;
        }
        Integer psicologoId = util.Sesion.esPsicologo() ? util.Sesion.getUsuarioId() : null;
        // SwingWorker directo (no Tema.enSegundoPlano): esto corre solo cada minuto y no debe
        // poner el cursor de espera mientras el usuario está trabajando.
        new SwingWorker<java.util.List<dao.TurnoDAO.CitaDelDia>, Void>() {
            @Override
            protected java.util.List<dao.TurnoDAO.CitaDelDia> doInBackground() {
                return new dao.TurnoDAO().obtenerDelDia(java.time.LocalDate.now(), psicologoId);
            }

            @Override
            protected void done() {
                try {
                    alRecibirCitasDeHoy(get(), esBienvenida);
                } catch (Exception e) {
                    System.out.println("Error revisando citas de hoy: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void alRecibirCitasDeHoy(java.util.List<dao.TurnoDAO.CitaDelDia> citas, boolean esBienvenida) {
        if (resumenDelDia != null) {
            resumenDelDia.setCitas(citas);
        }
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter hora = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        if (esBienvenida) {
            long pendientes = citas.stream()
                .filter(c -> "programado".equals(c.estado) && c.fechaHora.isAfter(ahora)).count();
            citas.stream().filter(c -> "programado".equals(c.estado) && c.fechaHora.isAfter(ahora)).findFirst()
                .ifPresent(p -> Tema.mostrarNotificacion(this, (pendientes == 1 ? "Hoy tenés 1 cita" : "Hoy tenés " + pendientes + " citas")
                    + " · la próxima a las " + p.fechaHora.format(hora) + " con " + p.estudiante));
            return;
        }
        for (dao.TurnoDAO.CitaDelDia c : citas) {
            long faltan = java.time.Duration.between(ahora, c.fechaHora).toMinutes();
            if ("programado".equals(c.estado) && faltan >= 0 && faltan <= MINUTOS_AVISO_PREVIO
                    && citasAvisadas.add(c.turnoId)) {
                // Con la pantalla bloqueada el aviso no dice el nombre del estudiante
                // (quien esté frente a la PC puede no ser el profesional).
                Tema.mostrarNotificacion(this, "Cita " + ResumenDelDia.cuandoFalta(c.fechaHora, ahora)
                    + " (" + c.fechaHora.format(hora) + ")"
                    + (bloqueo != null && bloqueo.estaBloqueada() ? "" : ": " + c.estudiante));
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    /** Abre la ficha de un estudiante desde cualquier lugar (Panel, franja "Hoy", Ctrl+K). */
    private void abrirFichaEstudiante(int pacienteId) {
        mostrarEstudiantes();
        panelEstudiantes.abrirFicha(pacienteId);
    }

    private void verAgenda() {
        mostrarEstudiantes();
        panelEstudiantes.mostrarPestanaAgenda();
    }

    /**
     * Respaldo automático de la base de datos: se corre una vez por sesión (acá en el
     * constructor, no en construirVentana(), que también se llama al alternar modo oscuro) y solo
     * hace algo si el último respaldo tiene más de 24 horas. Corre en un hilo aparte, sin usar
     * Swing, porque no hace falta avisarle nada al usuario si todo sale bien.
     */
    private void respaldarSiHaceFalta() {
        if (!util.RespaldoBaseDatos.requiereRespaldo(24)) {
            return;
        }
        new Thread(() -> {
            String ruta = util.RespaldoBaseDatos.generarRespaldo();
            if (ruta != null) {
                System.out.println("Respaldo automático generado: " + ruta);
            }
        }, "respaldo-automatico").start();
    }

    /** F11 alterna pantalla completa (maximizada) — funciona sin importar qué pestaña esté activa. */
    private void habilitarAtajoPantallaCompleta() {
        JRootPane rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("F11"), "alternarPantallaCompleta");
        rootPane.getActionMap().put("alternarPantallaCompleta", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setExtendedState(getExtendedState() == MAXIMIZED_BOTH ? NORMAL : MAXIMIZED_BOTH);
            }
        });
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

        // Se preserva el estado de pantalla completa/maximizada si esto se llama para reconstruir
        // el shell (p. ej. al alternar modo oscuro) en vez de perderlo al reaplicar el tamaño base.
        int estadoVentana = getExtendedState();

        setTitle("Departamento de Psicología · Colegio San Roque González — " + usuario
            + " (" + Tema.etiquetaRol(rol) + ")");
        setIconImage(Tema.iconoApp());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(estadoVentana);
        // La X no cierra directo: primero se revisa si hay algo sin guardar (ver salirDelSistema).
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        panelEstudiantes = null;
        panelDashboard = null;
        panelConfiguracion = null;

        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);
        contenedor.add(crearInicio(), CARTA_INICIO);
        programarPrecarga();

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
        // GridLayout estira cada celda para llenar todo el espacio disponible (ancho y alto) —
        // acá es justo lo que se quiere: que las 3 tarjetas ocupen toda la pantalla. TarjetaMenu
        // centra su ícono/título/descripción dentro de su alto real (no en coordenadas fijas
        // desde arriba), así que una tarjeta enorme no queda con el contenido pegado arriba y
        // un hueco vacío abajo.
        JPanel panelContent = new JPanel(new GridLayout(1, 3, 24, 0));
        panelContent.setBackground(Tema.FONDO);
        panelContent.setBorder(BorderFactory.createEmptyBorder(24, 30, 8, 30));

        agregarTarjeta(panelContent, new TarjetaMenu(Icono.PACIENTES, Tema.ACENTO_AZUL,
            "Estudiantes", "Fichas, atenciones,\nagenda y reportes",
            this::mostrarEstudiantes, 0), 1);

        agregarTarjeta(panelContent, new TarjetaMenu(Icono.DASHBOARD, Tema.ACENTO_CELESTE,
            "Panel", "Estadísticas del\nDepartamento",
            this::mostrarPanel, 1), 2);

        agregarTarjeta(panelContent, new TarjetaMenu(Icono.CONFIGURACION, Tema.ACENTO_GRIS,
            "Configuración", "Cuenta, contraseña,\nusuarios y actividad",
            this::mostrarConfiguracion, 2), 3);

        JLabel lblAtajos = new JLabel("Atajos: Ctrl+K buscar estudiante · Ctrl+L bloquear · Alt+1 Estudiantes · Alt+2 Panel · Alt+3 Configuración · Esc vuelve atrás · F11 pantalla completa",
            SwingConstants.CENTER);
        lblAtajos.setFont(Tema.TEXTO_CHICO);
        lblAtajos.setForeground(Tema.TEXTO_SECUNDARIO);
        lblAtajos.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        // Arriba de las tarjetas: buscador + franja "Hoy" con las citas del día.
        resumenDelDia = new ResumenDelDia(this::abrirFichaEstudiante, this::verAgenda);
        resumenDelDia.refrescar(null);
        JPanel envResumen = new JPanel(new BorderLayout());
        envResumen.setBackground(Tema.FONDO);
        envResumen.setBorder(BorderFactory.createEmptyBorder(14, 30, 0, 30));
        envResumen.add(resumenDelDia, BorderLayout.CENTER);

        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setBackground(Tema.FONDO);
        arriba.add(crearBarraBusqueda(), BorderLayout.NORTH);
        arriba.add(envResumen, BorderLayout.CENTER);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setBackground(Tema.FONDO);
        envoltorio.add(arriba, BorderLayout.NORTH);
        envoltorio.add(panelContent, BorderLayout.CENTER);
        envoltorio.add(lblAtajos, BorderLayout.SOUTH);
        return envoltorio;
    }

    /**
     * Agrega la tarjeta y le asigna Alt+número, que funciona desde cualquier sección (no solo
     * desde el menú), para saltar directo a otra sin pasar por "Volver al menú".
     */
    private void agregarTarjeta(JPanel contenedorTarjetas, TarjetaMenu tarjeta, int numero) {
        contenedorTarjetas.add(tarjeta);
        tarjeta.setToolTipText("Alt+" + numero);
        // Tema.atajo: no navega por detrás de una hoja abierta (confirmación, búsqueda, etc.).
        Tema.atajo(getRootPane(), KeyStroke.getKeyStroke("alt " + numero), () ->
            tarjeta.dispatchEvent(new java.awt.event.MouseEvent(tarjeta, java.awt.event.MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false)));
    }

    /** Ctrl+K: buscar un estudiante desde cualquier pantalla y abrir su ficha. */
    private void abrirBusquedaRapida() {
        BusquedaRapida.mostrar(this, this::abrirFichaEstudiante);
    }

    /**
     * Barra con aspecto de buscador en el menú principal: hace visible la búsqueda rápida (que
     * si no, solo se descubre sabiendo el atajo Ctrl+K).
     */
    private JComponent crearBarraBusqueda() {
        JPanel barra = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                g2.setColor(Tema.SUPERFICIE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(hover ? Tema.PRIMARIO : Tema.BORDE);
                g2.setStroke(new BasicStroke(hover ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
            }
        };
        barra.setOpaque(false);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
        barra.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        barra.setToolTipText("Buscar un estudiante por nombre, CI o teléfono y abrir su ficha (Ctrl+K)");

        JLabel lblTexto = new JLabel("Buscar un estudiante…", new IconoSwing(Icono.BUSCAR, Tema.TEXTO_SECUNDARIO, 18), SwingConstants.LEFT);
        lblTexto.setIconTextGap(10);
        lblTexto.setFont(Tema.fuente(Font.PLAIN, 15));
        lblTexto.setForeground(Tema.TEXTO_SECUNDARIO);
        barra.add(lblTexto, BorderLayout.CENTER);

        JLabel lblAtajo = new JLabel("Ctrl + K");
        lblAtajo.setFont(Tema.fuente(Font.BOLD, 11));
        lblAtajo.setForeground(Tema.TEXTO_SECUNDARIO);
        lblAtajo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDE, 1, true),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        barra.add(lblAtajo, BorderLayout.EAST);

        java.awt.event.MouseAdapter mouse = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                barra.putClientProperty("hover", true);
                barra.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                barra.putClientProperty("hover", false);
                barra.repaint();
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                abrirBusquedaRapida();
            }
        };
        barra.addMouseListener(mouse);

        JPanel envoltorio = new JPanel(new GridBagLayout());
        envoltorio.setBackground(Tema.FONDO);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(22, 30, 0, 30));
        barra.setPreferredSize(new Dimension(560, 46));
        envoltorio.add(barra);
        return envoltorio;
    }

    private void mostrarInicio() {
        if (resumenDelDia != null) {
            resumenDelDia.refrescar(null);
        }
        Tema.cambiarConFundido(this, contenedor, cardLayout, CARTA_INICIO, true);
    }

    /**
     * Precarga: unos segundos después de mostrar el menú, mientras el usuario todavía está ahí,
     * se arman por adelantado las secciones (una por vez, cada 2 s). Armar Estudiantes la
     * primera vez tarda ~0,6 s (tres pantallas con sus tablas); sin esto, esa demora caía justo
     * en el primer clic y la transición arrancaba con un tirón.
     */
    private void programarPrecarga() {
        Timer precarga = new Timer(2000, null);
        precarga.addActionListener(e -> {
            if (!util.Sesion.estaActiva() || !isShowing()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            if (contenedor.getComponentCount() == 0 || !contenedor.getComponent(0).isShowing()) {
                return; // el usuario ya está en otra sección: se reintenta cuando vuelva al menú
            }
            if (asegurarEstudiantes() || asegurarPanel() || asegurarConfiguracion()) {
                return; // una sección por vez, para no congelar el menú de un saque
            }
            ((Timer) e.getSource()).stop();
        });
        precarga.start();
    }

    /** Crea la sección si todavía no existe; devuelve true si la acaba de crear. */
    private boolean asegurarEstudiantes() {
        if (panelEstudiantes != null) {
            return false;
        }
        panelEstudiantes = new EstudiantesPanel(this::mostrarInicio);
        contenedor.add(panelEstudiantes, "estudiantes");
        return true;
    }

    private void mostrarEstudiantes() {
        if (!asegurarEstudiantes()) {
            panelEstudiantes.refrescar();
        }
        Tema.cambiarConFundido(this, contenedor, cardLayout, "estudiantes");
    }

    private void mostrarPanel() {
        if (!asegurarPanel()) {
            panelDashboard.refrescar();
        }
        Tema.cambiarConFundido(this, contenedor, cardLayout, "panel");
    }

    private boolean asegurarPanel() {
        if (panelDashboard == null) {
            panelDashboard = new Dashboard(this::mostrarInicio);
            // Números y filas del Panel llevan a la ficha, la lista (filtrada) o la agenda.
            panelDashboard.setNavegacion(new Dashboard.Navegacion() {
                @Override
                public void abrirFicha(int pacienteId) {
                    abrirFichaEstudiante(pacienteId);
                }

                @Override
                public void verEstudiantes(String curso) {
                    mostrarEstudiantes();
                    panelEstudiantes.mostrarListaFiltrada(curso);
                }

                @Override
                public void verAgenda() {
                    MenuPrincipal.this.verAgenda();
                }
            });
            contenedor.add(panelDashboard, "panel");
            return true;
        }
        return false;
    }

    private boolean asegurarConfiguracion() {
        if (panelConfiguracion != null) {
            return false;
        }
        panelConfiguracion = new Configuracion(this::mostrarInicio, this::cerrarSesion,
            () -> reconstruirVentanaSiSePuede(() -> { }));
        panelConfiguracion.setAlCambiarTema(() -> reconstruirVentanaSiSePuede(Tema::alternarModoOscuro));
        contenedor.add(panelConfiguracion, "configuracion");
        return true;
    }

    private void mostrarConfiguracion() {
        if (!asegurarConfiguracion()) {
            panelConfiguracion.refrescar();
        }
        Tema.cambiarConFundido(this, contenedor, cardLayout, "configuracion");
    }

    private void cerrarSesion() {
        siNoHayCambiosONoImporta("¿Cerrar sesión?", "Cerrar sesión sin guardar", this::cerrarSesionSinPreguntar);
    }

    private void cerrarSesionSinPreguntar() {
        if (bloqueo != null) {
            bloqueo.detener();
        }
        if (timerRecordatorios != null) {
            timerRecordatorios.stop();
        }
        BusquedaRapida.olvidarRecientes();
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
