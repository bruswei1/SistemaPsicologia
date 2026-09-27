package Vista;

import dao.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Dashboard extends javax.swing.JPanel {

    private static final String[] PERIODOS = {"Todo", "Este mes"};

    private final Runnable alVolver;
    private final boolean construirLayoutPropio;
    private TarjetaEstadistica lblPacientes;
    private TarjetaEstadistica lblSesiones;
    private TarjetaEstadistica lblTurnos;
    private TarjetaEstadistica lblPsicologos;
    private TarjetaEstadistica lblPendientes;
    private TarjetaEstadistica lblSeguimientosPendientes;
    private GraficoBarras grafico;
    private GraficoBarras graficoTendencia;
    private JComboBox<String> comboPeriodo;
    private DefaultTableModel modeloPorCurso;
    private DefaultTableModel modeloProximasCitas;
    private DefaultTableModel modeloPorMotivo;
    private DefaultTableModel modeloSeguimientosPendientes;
    private int solicitudActual;

    /**
     * Lo que se puede hacer desde el Panel al tocar un número o una fila: el Panel no conoce las
     * otras pantallas, se lo indica {@link MenuPrincipal}. Si no se configuró (prueba aislada), el
     * Panel sigue funcionando pero sin navegación.
     */
    public interface Navegacion {
        void abrirFicha(int pacienteId);

        /** @param curso etiqueta exacta del curso para filtrar la lista, o null para ver todos */
        void verEstudiantes(String curso);

        void verAgenda();
    }

    private Navegacion navegacion;
    private final java.util.List<Integer> idsProximasCitas = new java.util.ArrayList<>();
    private final java.util.List<Integer> idsSeguimientos = new java.util.ArrayList<>();
    private JPanel panelSeguimientosVisible;

    public void setNavegacion(Navegacion navegacion) {
        this.navegacion = navegacion;
    }

    public Dashboard(Runnable alVolver) {
        this(alVolver, true);
    }

    /**
     * @param construirLayoutPropio si es false, no arma su propio encabezado ni el botón
     *                              "Volver" — pensado para embeber este panel como pestaña
     *                              dentro de otro contenedor (mismo patrón que
     *                              {@link GestionPacientes#GestionPacientes(Runnable, boolean)}
     *                              y {@link Agenda#Agenda(Runnable, boolean)}).
     */
    public Dashboard(Runnable alVolver, boolean construirLayoutPropio) {
        this.alVolver = alVolver;
        this.construirLayoutPropio = construirLayoutPropio;
        initComponents();
        cargarDatos();
    }

    public void refrescar() {
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Tema.SUPERFICIE);
        if (construirLayoutPropio) {
            headerPanel.add(Tema.panelEncabezado("Panel General", util.Sesion.esPsicologo()
                ? "Resumen de tus estudiantes y tus citas"
                : "Resumen general del Departamento de Psicología"),
                BorderLayout.NORTH);
        }

        JPanel headerAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        headerAcciones.setBackground(Tema.SUPERFICIE);
        headerAcciones.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE),
            BorderFactory.createEmptyBorder(4, 16, 4, 16)
        ));
        headerAcciones.add(new JLabel("Período:"));
        comboPeriodo = new JComboBox<>(PERIODOS);
        comboPeriodo.addActionListener(e -> cargarDatos());
        headerAcciones.add(comboPeriodo);
        JButton btnActualizar = Tema.botonSecundario("Actualizar", Icono.LIMPIAR);
        btnActualizar.addActionListener(e -> cargarDatos());
        headerAcciones.add(btnActualizar);
        headerPanel.add(headerAcciones, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Centro: tarjetas + gráfico
        JPanel centro = new JPanel(new BorderLayout(0, 20));
        centro.setBackground(Tema.FONDO);
        centro.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // "secretaria" está bloqueada de las pestañas clínicas (Seguimiento/Atenciones) en
        // DetallePaciente; "Seguimientos vencidos" nombra estudiantes concretos junto a un caso
        // de acoso reiterado (Ley 4633/2012), así que es información clínica identificable y
        // debe quedar afuera del Panel para ese rol, igual que el resto de lo clínico.
        boolean esSecretaria = util.Sesion.esSecretaria();

        JPanel panelTiles = new JPanel(new GridLayout(1, 4, 20, 0));
        panelTiles.setBackground(Tema.FONDO);

        lblPacientes = new TarjetaEstadistica(Icono.PACIENTES, "Estudiantes", "0", Tema.ACENTO_AZUL.icono);
        lblPacientes.setAccion("Ver la lista de estudiantes", () -> {
            if (navegacion != null) {
                navegacion.verEstudiantes(null);
            }
        });
        panelTiles.add(lblPacientes);

        lblSesiones = new TarjetaEstadistica(Icono.SESIONES, "Atenciones", "0", Tema.ACENTO_MORADO.icono);
        panelTiles.add(lblSesiones);

        lblTurnos = new TarjetaEstadistica(Icono.TURNOS, "Citas", "0", Tema.ACENTO_VERDE.icono);
        lblTurnos.setAccion("Abrir la agenda de citas", () -> {
            if (navegacion != null) {
                navegacion.verAgenda();
            }
        });
        panelTiles.add(lblTurnos);

        lblPsicologos = new TarjetaEstadistica(Icono.PACIENTES, "Profesionales", "0", Tema.ACENTO_CELESTE.icono);
        panelTiles.add(lblPsicologos);

        // Las alertas van en su propia fila, separadas de las tarjetas informativas: mismo ancho
        // de celda (misma cantidad de columnas en el GridLayout) para que se vean del mismo
        // tamaño, pero agrupadas bajo su propio título en vez de competir por atención con el
        // resto de las tarjetas.
        JLabel lblTituloAlertas = new JLabel("Alertas");
        lblTituloAlertas.setFont(Tema.fuente(Font.BOLD, 13));
        lblTituloAlertas.setForeground(Tema.PELIGRO);
        lblTituloAlertas.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 0));

        JPanel panelAlertas = new JPanel(new GridLayout(1, 4, 20, 0));
        panelAlertas.setBackground(Tema.FONDO);

        lblPendientes = new TarjetaEstadistica(Icono.ALERTA, "Casos pendientes", "0", Tema.PELIGRO);
        lblPendientes.setToolTipText("Citas programadas cuya fecha/hora ya pasó y todavía no se actualizó el estado ni se cargó una atención");
        lblPendientes.setAccion("Abrir la agenda para actualizarlas", () -> {
            if (navegacion != null) {
                navegacion.verAgenda();
            }
        });
        panelAlertas.add(lblPendientes);

        lblSeguimientosPendientes = new TarjetaEstadistica(Icono.ALERTA, "Seguimientos vencidos", "0", Tema.PELIGRO);
        lblSeguimientosPendientes.setToolTipText("Casos marcados como reiterados (Ley 4633/2012) sin una nueva entrada de "
            + "Seguimiento en los últimos " + dao.HistoriaPsicologicaDAO.DIAS_UMBRAL_SEGUIMIENTO + " días");
        lblSeguimientosPendientes.setAccion("Ver la lista de casos (más abajo)", () -> {
            if (panelSeguimientosVisible != null) {
                panelSeguimientosVisible.scrollRectToVisible(new Rectangle(0, 0,
                    panelSeguimientosVisible.getWidth(), panelSeguimientosVisible.getHeight()));
            }
        });
        if (!esSecretaria) {
            panelAlertas.add(lblSeguimientosPendientes);
        }

        JPanel panelAlertasConTitulo = new JPanel(new BorderLayout());
        panelAlertasConTitulo.setBackground(Tema.FONDO);
        panelAlertasConTitulo.add(lblTituloAlertas, BorderLayout.NORTH);
        panelAlertasConTitulo.add(panelAlertas, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBackground(Tema.FONDO);
        contentPanel.add(panelTiles, BorderLayout.NORTH);
        contentPanel.add(panelAlertasConTitulo, BorderLayout.CENTER);

        centro.add(contentPanel, BorderLayout.NORTH);

        grafico = new GraficoBarras(
            new String[]{"Estudiantes", "Atenciones", "Citas", "Profesionales"},
            new Color[]{Tema.ACENTO_AZUL.icono, Tema.ACENTO_MORADO.icono, Tema.ACENTO_VERDE.icono, Tema.ACENTO_CELESTE.icono}
        );
        JPanel panelGrafico = Tema.panelTarjeta("Comparativa");
        panelGrafico.add(grafico, BorderLayout.CENTER);

        String[] mesesEtiqueta = new String[6];
        Color[] coloresTendencia = new Color[6];
        java.time.YearMonth actual = java.time.YearMonth.now();
        java.time.format.DateTimeFormatter formatoMes = java.time.format.DateTimeFormatter.ofPattern("MMM");
        for (int i = 0; i < 6; i++) {
            mesesEtiqueta[i] = actual.minusMonths(5 - i).format(formatoMes);
            coloresTendencia[i] = Tema.ACENTO_AZUL.icono;
        }
        graficoTendencia = new GraficoBarras(mesesEtiqueta, coloresTendencia);
        JPanel panelTendencia = Tema.panelTarjeta("Estudiantes nuevos por mes");
        panelTendencia.add(graficoTendencia, BorderLayout.CENTER);

        JPanel panelGraficos = new JPanel(new GridLayout(2, 1, 0, 20));
        panelGraficos.setBackground(Tema.FONDO);
        panelGraficos.add(panelGrafico);
        panelGraficos.add(panelTendencia);

        centro.add(panelGraficos, BorderLayout.CENTER);

        modeloPorCurso = new DefaultTableModel(new Object[]{"Curso", "Estudiantes"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaPorCurso = Tema.crearTablaConVacio(modeloPorCurso, "No hay estudiantes registrados todavía");
        Tema.estilizarTabla(tablaPorCurso);
        tablaPorCurso.setPreferredScrollableViewportSize(new Dimension(0, 160));
        hacerNavegable(tablaPorCurso, fila -> {
            if (navegacion != null) {
                navegacion.verEstudiantes(String.valueOf(modeloPorCurso.getValueAt(fila, 0)));
            }
        }, "Clic para ver los estudiantes de este curso");
        JPanel panelPorCurso = Tema.panelTarjeta("Estudiantes por curso · clic para ver");
        panelPorCurso.add(new JScrollPane(tablaPorCurso), BorderLayout.CENTER);

        modeloProximasCitas = new DefaultTableModel(new Object[]{"Estudiante", "Curso", "Profesional", "Fecha y hora"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaProximasCitas = Tema.crearTablaConVacio(modeloProximasCitas, "No hay citas próximas");
        Tema.estilizarTabla(tablaProximasCitas);
        tablaProximasCitas.setPreferredScrollableViewportSize(new Dimension(0, 160));
        hacerNavegable(tablaProximasCitas, fila -> {
            if (navegacion != null && fila < idsProximasCitas.size()) {
                navegacion.abrirFicha(idsProximasCitas.get(fila));
            }
        }, "Clic para abrir la ficha del estudiante");
        JPanel panelProximasCitas = Tema.panelTarjeta("Próximas citas · clic para abrir la ficha");
        panelProximasCitas.add(new JScrollPane(tablaProximasCitas), BorderLayout.CENTER);

        modeloSeguimientosPendientes = new DefaultTableModel(new Object[]{"Estudiante", "Curso", "Última entrada reiterada"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaSeguimientosPendientes = Tema.crearTablaConVacio(modeloSeguimientosPendientes, "Sin casos pendientes de seguimiento");
        Tema.estilizarTabla(tablaSeguimientosPendientes);
        tablaSeguimientosPendientes.setPreferredScrollableViewportSize(new Dimension(0, 160));
        hacerNavegable(tablaSeguimientosPendientes, fila -> {
            if (navegacion != null && fila < idsSeguimientos.size()) {
                navegacion.abrirFicha(idsSeguimientos.get(fila));
            }
        }, "Clic para abrir la ficha y cargar el seguimiento");
        JPanel panelSeguimientosPendientes = Tema.panelTarjeta("Seguimientos vencidos · clic para abrir");
        panelSeguimientosVisible = panelSeguimientosPendientes;
        panelSeguimientosPendientes.add(new JScrollPane(tablaSeguimientosPendientes), BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new GridLayout(1, esSecretaria ? 2 : 3, 20, 0));
        panelInferior.setBackground(Tema.FONDO);
        panelInferior.add(panelPorCurso);
        panelInferior.add(panelProximasCitas);
        if (!esSecretaria) {
            panelInferior.add(panelSeguimientosPendientes);
        }

        modeloPorMotivo = new DefaultTableModel(new Object[]{"Motivo de la atención", "Casos"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaPorMotivo = Tema.crearTablaConVacio(modeloPorMotivo, "Todavía no hay casos registrados");
        Tema.estilizarTabla(tablaPorMotivo);
        tablaPorMotivo.setPreferredScrollableViewportSize(new Dimension(0, 160));
        JPanel panelPorMotivo = Tema.panelTarjeta("Casos por motivo de atención");
        panelPorMotivo.add(new JScrollPane(tablaPorMotivo), BorderLayout.CENTER);

        JPanel panelInferiorCompleto = new JPanel(new GridLayout(2, 1, 0, 20));
        panelInferiorCompleto.setBackground(Tema.FONDO);
        panelInferiorCompleto.add(panelInferior);
        panelInferiorCompleto.add(panelPorMotivo);
        centro.add(panelInferiorCompleto, BorderLayout.SOUTH);

        // `centro` ya no entra siempre en el alto fijo de la ventana (1200x800, no
        // redimensionable) ahora que tiene 5 tarjetas + 2 gráficos + 2 tablas: en vez de dejar
        // que BorderLayout comprima y superponga contenido para "hacerlo entrar" (lo que rompía
        // el gráfico Comparativa), se hace scrolleable — mismo patrón que ya usa
        // Configuracion.crearPanelPreferencias/initComponents para su propio panel "centro".
        JScrollPane scrollCentro = new JScrollPane(centro);
        scrollCentro.setBorder(null);
        scrollCentro.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollCentro, BorderLayout.CENTER);

        if (construirLayoutPropio) {
            // Footer
            JPanel footerPanel = new JPanel();
            footerPanel.setBackground(Tema.SUPERFICIE);
            footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE_SUAVE));
            footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 15));

            JButton btnVolver = Tema.botonPrimario("Volver", Icono.VOLVER);
            btnVolver.addActionListener(e -> alVolver.run());
            footerPanel.add(btnVolver);
            Tema.atajoEscape(this, btnVolver, alVolver);

            mainPanel.add(footerPanel, BorderLayout.SOUTH);
        }
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Fila clickeable: cursor de mano, resaltado al pasar el mouse (ya lo da Tema.estilizarTabla)
     * y clic o Enter ejecutan la acción con el índice de fila del MODELO (la tabla se puede ordenar).
     */
    private static void hacerNavegable(JTable tabla, java.util.function.IntConsumer accion, String tooltip) {
        tabla.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tabla.setToolTipText(tooltip);
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                if (fila >= 0 && SwingUtilities.isLeftMouseButton(e)) {
                    accion.accept(tabla.convertRowIndexToModel(fila));
                }
            }
        });
        tabla.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "abrirFila");
        tabla.getActionMap().put("abrirFila", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int fila = tabla.getSelectedRow();
                if (fila >= 0) {
                    accion.accept(tabla.convertRowIndexToModel(fila));
                }
            }
        });
    }

    private void cargarDatos() {
        boolean esteMes = comboPeriodo != null && "Este mes".equals(comboPeriodo.getSelectedItem());
        java.time.LocalDate desde = esteMes
            ? java.time.YearMonth.now().atDay(1)
            : java.time.LocalDate.of(2000, 1, 1);

        // Si se cambia el período (o se clickea "Actualizar") varias veces seguidas, una
        // respuesta vieja podría llegar después que la más nueva y pisarla con datos
        // desactualizados — mismo problema que ya se dio con el calendario de Agenda. Cada pedido
        // se numera y solo el más reciente aplica sus resultados a la interfaz.
        int miSolicitud = ++solicitudActual;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    PacienteDAO pacienteDAO = new PacienteDAO();
                    SesionDAO sesionDAO = new SesionDAO();
                    TurnoDAO turnoDAO = new TurnoDAO();
                    UsuarioDAO usuarioDAO = new UsuarioDAO();
                    HistoriaPsicologicaDAO historiaDAO = new HistoriaPsicologicaDAO();

                    // Un profesional ve los números de SUS estudiantes y citas (lo mismo que ve al
                    // tocarlos); administración y usuario autorizado, los del Departamento entero.
                    Integer psicologoId = util.Sesion.esPsicologo() ? util.Sesion.getUsuarioId() : null;
                    int totalPacientes = pacienteDAO.contarDesde(desde, psicologoId);
                    int totalSesiones = sesionDAO.obtenerCountSesionesCompletadasDesde(desde, psicologoId);
                    int totalTurnos = turnoDAO.obtenerCountTurnosConfirmadosDesde(desde, psicologoId);
                    int totalPsicologos = usuarioDAO.obtenerPorRol("psicologo").size();
                    java.util.LinkedHashMap<String, Integer> nuevosPorMes = pacienteDAO.obtenerNuevosPorMes(6, psicologoId);
                    java.util.LinkedHashMap<String, Integer> porCurso = pacienteDAO.contarPorCurso(psicologoId);

                    java.util.LinkedHashMap<String, Integer> porMotivo = historiaDAO.contarPorMotivo(desde, psicologoId);
                    int totalPendientes = turnoDAO.contarProgramadosVencidos(psicologoId);
                    java.util.List<TurnoDAO.ProximaCita> proximasCitas = turnoDAO.obtenerProximas(5, psicologoId);
                    int totalSeguimientosPendientes = historiaDAO.contarCasosReiteradosSinSeguimiento(
                        psicologoId, HistoriaPsicologicaDAO.DIAS_UMBRAL_SEGUIMIENTO);
                    java.util.List<HistoriaPsicologicaDAO.CasoPendiente> seguimientosPendientes =
                        historiaDAO.obtenerCasosReiteradosSinSeguimiento(5, psicologoId, HistoriaPsicologicaDAO.DIAS_UMBRAL_SEGUIMIENTO);
                    java.time.format.DateTimeFormatter formatoFechaHora =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                    SwingUtilities.invokeLater(() -> {
                        if (miSolicitud != solicitudActual) {
                            return;
                        }
                        lblPacientes.setNumero(String.valueOf(totalPacientes));
                        lblSesiones.setNumero(String.valueOf(totalSesiones));
                        lblTurnos.setNumero(String.valueOf(totalTurnos));
                        lblPsicologos.setNumero(String.valueOf(totalPsicologos));
                        lblPendientes.setNumero(String.valueOf(totalPendientes));
                        lblSeguimientosPendientes.setNumero(String.valueOf(totalSeguimientosPendientes));
                        grafico.setValores(new int[]{totalPacientes, totalSesiones, totalTurnos, totalPsicologos});
                        graficoTendencia.setValores(nuevosPorMes.values().stream().mapToInt(Integer::intValue).toArray());
                        modeloPorCurso.setRowCount(0);
                        for (java.util.Map.Entry<String, Integer> entrada : porCurso.entrySet()) {
                            modeloPorCurso.addRow(new Object[]{entrada.getKey(), entrada.getValue()});
                        }
                        modeloPorMotivo.setRowCount(0);
                        for (java.util.Map.Entry<String, Integer> entrada : porMotivo.entrySet()) {
                            modeloPorMotivo.addRow(new Object[]{entrada.getKey(), entrada.getValue()});
                        }
                        modeloProximasCitas.setRowCount(0);
                        idsProximasCitas.clear();
                        for (TurnoDAO.ProximaCita c : proximasCitas) {
                            idsProximasCitas.add(c.pacienteId);
                            modeloProximasCitas.addRow(new Object[]{
                                c.estudiante,
                                c.curso != null ? c.curso : "N/A",
                                c.profesional,
                                c.fechaHora.format(formatoFechaHora)
                            });
                        }
                        modeloSeguimientosPendientes.setRowCount(0);
                        idsSeguimientos.clear();
                        for (HistoriaPsicologicaDAO.CasoPendiente c : seguimientosPendientes) {
                            idsSeguimientos.add(c.pacienteId);
                            modeloSeguimientosPendientes.addRow(new Object[]{
                                c.estudiante,
                                c.curso != null ? c.curso : "N/A",
                                c.ultimoSeguimiento.format(formatoFechaHora)
                            });
                        }
                    });

                    System.out.println("Dashboard actualizado: " + totalPacientes + " pacientes");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    /** Solo para probar esta pantalla de forma aislada, fuera del shell de MenuPrincipal. */
    public static void main(String[] args) {
        Tema.instalarLookAndFeelGuardado();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Panel General (prueba aislada)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setIconImage(Tema.iconoApp());
            f.getContentPane().add(new Dashboard(() -> System.exit(0)));
            f.setSize(1200, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private static class TarjetaEstadistica extends JPanel {
        private final Icono icono;
        private final String titulo;
        private String numero;
        private final Color color;

        public TarjetaEstadistica(Icono icono, String titulo, String numero, Color color) {
            this.icono = icono;
            this.titulo = titulo;
            this.numero = numero;
            this.color = color;
            setBackground(Tema.SUPERFICIE);
            setOpaque(true);
            // Sin esto la tarjeta no tiene tamaño preferido propio (no tiene hijos, solo pinta a
            // mano en paintComponent): con 5 tarjetas + los paneles nuevos del Panel General, el
            // BorderLayout que las contiene las comprimía a una tira de 5px y el resto del
            // contenido (ícono, título, número) quedaba recortado fuera del área visible.
            setPreferredSize(new Dimension(0, 165));
        }

        private int valorMostrado = 0;
        private Timer animacionNumero;
        private boolean hover;
        private boolean clickeable;

        /** Hace la tarjeta clickeable (cursor de mano, borde de color al pasar el mouse). */
        public void setAccion(String descripcion, Runnable accion) {
            clickeable = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String actual = getToolTipText();
            setToolTipText((actual != null ? actual + " — " : "") + descripcion);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    accion.run();
                }
            });
        }

        /**
         * Anima el número "contando" desde el valor actual hasta el nuevo en vez de reemplazarlo
         * de golpe — más notorio al abrir el Panel o cambiar el período, cuando varias tarjetas
         * cambian de 0 a su valor real al mismo tiempo. Si el valor no es un entero simple (no
         * debería pasar acá, pero por las dudas), cae al comportamiento anterior sin animar.
         */
        public void setNumero(String numero) {
            int valorNuevo;
            try {
                valorNuevo = Integer.parseInt(numero);
            } catch (NumberFormatException e) {
                this.numero = numero;
                repaint();
                return;
            }

            if (animacionNumero != null && animacionNumero.isRunning()) {
                animacionNumero.stop();
            }

            int valorInicial = valorMostrado;
            long inicio = System.currentTimeMillis();
            int duracionMs = 550;

            animacionNumero = new Timer(15, null);
            animacionNumero.addActionListener(e -> {
                float t = Math.min(1f, (System.currentTimeMillis() - inicio) / (float) duracionMs);
                float avance = 1f - (float) Math.pow(1f - t, 3); // ease-out cúbico: rápido al inicio, se asienta al final
                valorMostrado = valorInicial + Math.round((valorNuevo - valorInicial) * avance);
                this.numero = String.valueOf(valorMostrado);
                repaint();
                if (t >= 1f) {
                    valorMostrado = valorNuevo;
                    this.numero = String.valueOf(valorNuevo);
                    ((Timer) e.getSource()).stop();
                }
            });
            animacionNumero.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(Tema.SUPERFICIE);
            g2.fillRoundRect(0, 0, w, h, 15, 15);

            g2.setColor(hover ? color : Tema.BORDE);
            g2.setStroke(new BasicStroke(hover ? 2 : 1));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 15, 15);
            if (clickeable) {
                // Flecha "→" dibujada abajo a la derecha: indica que la tarjeta lleva a algún lado.
                g2.setColor(hover ? color : Tema.TEXTO_SECUNDARIO);
                Icono.SIGUIENTE.dibujar(g2, w - 34, h - 34, 18, hover ? color : Tema.TEXTO_SECUNDARIO);
            }

            // Franja recortada a la forma redondeada (antes se salía por las esquinas al ser un
            // rectángulo recto sobre una tarjeta con esquina curva) y con degradé propio en vez
            // de color plano, a tono con el resto del refresco visual.
            Graphics2D gFranja = (Graphics2D) g2.create();
            gFranja.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 15, 15));
            gFranja.setPaint(new GradientPaint(0, 0, color.brighter(), w, 0, color));
            gFranja.fillRect(0, 0, w, 5);
            gFranja.dispose();

            int insignia = 36;
            g2.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(20 + insignia * 0.3f, 25 + insignia * 0.25f), insignia * 0.9f,
                new float[]{0f, 1f},
                new Color[]{Tema.aclarar(color, 0.4f), color}
            ));
            g2.fillRoundRect(20, 25, insignia, insignia, 10, 10);
            icono.dibujar(g2, 26, 31, insignia - 12, Color.WHITE);

            g2.setColor(Tema.TEXTO_SECUNDARIO);
            g2.setFont(Tema.TEXTO);
            g2.drawString(titulo, 20, 90);

            g2.setColor(color);
            g2.setFont(Tema.fuente(Font.BOLD, 46));
            g2.drawString(numero, 20, 140);
        }
    }

    /** Gráfico de barras simple, sin librerías externas, para comparar las métricas del dashboard. */
    private static class GraficoBarras extends JPanel {
        private final String[] etiquetas;
        private final Color[] colores;
        private int[] valores;

        GraficoBarras(String[] etiquetas, Color[] colores) {
            this.etiquetas = etiquetas;
            this.colores = colores;
            this.valores = new int[etiquetas.length];
            setBackground(Tema.SUPERFICIE);
            setPreferredSize(new Dimension(0, 220));
        }

        void setValores(int[] valores) {
            this.valores = valores;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int ejeY = h - 30;

            int max = 1;
            for (int v : valores) {
                max = Math.max(max, v);
            }

            int n = etiquetas.length;
            int espacio = w / n;
            int barW = espacio / 3;
            int altoMax = ejeY - 20;

            // Líneas de referencia horizontales tenues en vez de un solo eje: ayuda a comparar
            // alturas entre barras de un vistazo, sin agregar números de escala que compitan con
            // las etiquetas de valor de cada barra.
            g2.setColor(Tema.BORDE_SUAVE);
            for (int linea = 1; linea <= 3; linea++) {
                int y = ejeY - (altoMax * linea / 4);
                g2.drawLine(10, y, w - 10, y);
            }
            g2.setColor(Tema.BORDE);
            g2.drawLine(10, ejeY, w - 10, ejeY);

            for (int i = 0; i < n; i++) {
                int cx = espacio * i + espacio / 2;
                int valor = i < valores.length ? valores[i] : 0;
                int barH = (int) (altoMax * (valor / (double) max));
                int barX = cx - barW / 2;
                int barY = ejeY - barH;
                Color color = colores[i % colores.length];

                // Carril de fondo a todo el alto disponible, muy tenue: le da a cada barra un
                // "canal" propio en vez de flotar sola sobre blanco, y ancla visualmente la
                // categoría aunque su valor sea chico.
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 18));
                g2.fillRoundRect(barX, ejeY - altoMax, barW, altoMax, 8, 8);

                // Sombra propia de la barra, mismo criterio que las insignias del resto del refresco.
                for (int s = 6; s >= 1; s--) {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 4));
                    g2.fillRoundRect(barX - s, barY - s + 4, barW + s * 2, Math.max(barH, 2) + s * 2, 10, 10);
                }

                g2.setPaint(new GradientPaint(cx, barY, Tema.aclarar(color, 0.3f), cx, ejeY, color));
                g2.fillRoundRect(barX, barY, barW, Math.max(barH, 2), 8, 8);

                g2.setColor(color);
                g2.setFont(Tema.fuente(Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                String texto = String.valueOf(valor);
                g2.drawString(texto, cx - fm.stringWidth(texto) / 2, barY - 10);

                g2.setColor(Tema.TEXTO_SECUNDARIO);
                g2.setFont(Tema.TEXTO_CHICO);
                fm = g2.getFontMetrics();
                g2.drawString(etiquetas[i], cx - fm.stringWidth(etiquetas[i]) / 2, ejeY + 20);
            }
        }
    }
}
