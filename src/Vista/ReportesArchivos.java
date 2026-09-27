package Vista;

import dao.PacienteDAO;
import modelos.Paciente;
import util.Carpetas;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pestaña "Reportes y archivos" de la sección Estudiantes: un único lugar para todo lo que el sistema
 * guarda en disco. Antes exportar/abrir carpetas vivía al fondo de Configuración → Mi cuenta y
 * no había forma de volver a encontrar una ficha o nota ya exportada sin buscarla a mano en el
 * Explorador de Windows.
 *
 * Arriba, acciones rápidas grandes; abajo, la lista de documentos ya generados (fichas, notas y
 * listados), con búsqueda instantánea, filtro por tipo y doble clic para abrir.
 */
public class ReportesArchivos extends JPanel {

    private static final String TIPO_TODOS = "Todos los tipos";
    private static final String TIPO_FICHA = "Fichas";
    private static final String TIPO_NOTA = "Notas de atención";
    private static final String TIPO_LISTADO = "Listados de estudiantes";
    private static final String TIPO_OTRO = "Otros";

    private static final Pattern ID_EN_CARPETA = Pattern.compile("\\((\\d+)\\)$");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Runnable alVolver;
    private final PacienteDAO pacienteDAO = new PacienteDAO();

    private DefaultTableModel modelo;
    private JTable tabla;
    private TableRowSorter<DefaultTableModel> ordenador;
    private JTextField txtBuscar;
    private JComboBox<String> cmbTipo;
    private JLabel lblConteo;
    private JButton btnAbrir;
    private JButton btnMostrar;
    private TarjetaAccion accionExportar;
    private TarjetaAccion accionRespaldar;

    /** Archivo de cada fila del modelo (mismo índice). */
    private final List<File> archivos = new ArrayList<>();

    private final boolean construirLayoutPropio;

    public ReportesArchivos(Runnable alVolver) {
        this(alVolver, true);
    }

    /**
     * @param construirLayoutPropio false cuando se embebe como pestaña (en {@link EstudiantesPanel}):
     *                              sin encabezado propio ni botón "Volver".
     */
    public ReportesArchivos(Runnable alVolver, boolean construirLayoutPropio) {
        this.alVolver = alVolver;
        this.construirLayoutPropio = construirLayoutPropio;
        initComponents();
        cargarDocumentos();
    }

    public void refrescar() {
        cargarDocumentos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        if (construirLayoutPropio) {
            add(Tema.panelEncabezado("Reportes y archivos",
                "Exportá listados, abrí las carpetas del sistema y encontrá los documentos ya generados"), BorderLayout.NORTH);
        }

        JPanel principal = new JPanel(new BorderLayout(0, 15));
        principal.setBackground(Tema.FONDO);
        principal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        principal.add(crearAccionesRapidas(), BorderLayout.NORTH);
        principal.add(crearListaDocumentos(), BorderLayout.CENTER);
        add(principal, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Tema.FONDO);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JLabel lblUbicacion = new JLabel("Todo se guarda en: " + Carpetas.base().getAbsolutePath());
        lblUbicacion.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblUbicacion.setForeground(Tema.TEXTO_SECUNDARIO);
        lblUbicacion.setToolTipText(Carpetas.base().getAbsolutePath());
        // CENTER (no WEST): si la ruta es muy larga se recorta con "..." en vez de taparse con el botón.
        footer.add(lblUbicacion, BorderLayout.CENTER);

        if (construirLayoutPropio) {
            JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
            btnVolver.addActionListener(e -> alVolver.run());
            footer.add(btnVolver, BorderLayout.EAST);
            Tema.atajoEscape(this, btnVolver, alVolver);
        }
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel crearAccionesRapidas() {
        List<TarjetaAccion> acciones = new ArrayList<>();

        accionExportar = new TarjetaAccion(Icono.EXPORTAR, Tema.ACENTO_VERDE, "Exportar estudiantes",
            util.Sesion.esPsicologo() ? "Planilla de Excel con tus estudiantes" : "Planilla de Excel con todos los estudiantes",
            this::exportarEstudiantes);
        acciones.add(accionExportar);

        acciones.add(new TarjetaAccion(Icono.CARPETA, Tema.ACENTO_AZUL, "Carpeta de reportes",
            "Fichas, notas y listados exportados", () -> Tema.abrirCarpeta(this, Carpetas.reportes())));

        // La carpeta raíz de adjuntos tiene los archivos de todos los estudiantes; cada
        // profesional abre la de sus estudiantes desde la ficha de cada uno.
        if (util.Sesion.esAdmin()) {
            acciones.add(new TarjetaAccion(Icono.CARPETA, Tema.ACENTO_AMBAR, "Carpeta de adjuntos",
                "Una subcarpeta por estudiante", () -> Tema.abrirCarpeta(this, Carpetas.adjuntos())));
        }

        accionRespaldar = new TarjetaAccion(Icono.GUARDAR, Tema.ACENTO_ROSA, "Respaldar ahora",
            "Copia de seguridad de la base de datos (además de la automática diaria)", this::respaldarBaseDeDatos);
        acciones.add(accionRespaldar);

        acciones.add(new TarjetaAccion(Icono.CARPETA, Tema.ACENTO_GRIS, "Carpeta de respaldos",
            "Últimas copias de seguridad", () -> Tema.abrirCarpeta(this, Carpetas.respaldos())));

        JPanel fila = new JPanel(new GridLayout(1, acciones.size(), 12, 0));
        fila.setOpaque(false);
        for (TarjetaAccion accion : acciones) {
            fila.add(accion);
        }

        JPanel tarjeta = Tema.panelTarjeta("Acciones rápidas");
        tarjeta.add(fila, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel crearListaDocumentos() {
        modelo = new DefaultTableModel(new Object[]{"Documento", "Tipo", "Estudiante", "Fecha", "Tamaño"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                // Clase Comparable para que el ordenador compare el valor real, no el texto.
                return column == 3 ? FechaOrdenable.class : column == 4 ? TamanoOrdenable.class : String.class;
            }
        };
        tabla = Tema.crearTablaConVacio(modelo, "Todavía no se generó ningún documento. Exportá una ficha o nota desde la ficha de un estudiante.");
        Tema.estilizarTabla(tabla);
        ordenador = new TableRowSorter<>(modelo);
        tabla.setRowSorter(ordenador);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(320);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    abrirSeleccionado();
                }
            }
        });
        tabla.setComponentPopupMenu(crearMenuContextual());
        Tema.seleccionarFilaConClicDerecho(tabla);
        tabla.registerKeyboardAction(e -> abrirSeleccionado(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Barra de filtros: la búsqueda filtra mientras se escribe, sin botón "Buscar".
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Buscar:"));
        txtBuscar = new JTextField(22);
        txtBuscar.setBorder(Tema.bordeCampo());
        txtBuscar.putClientProperty("JTextField.placeholderText", "Nombre del estudiante o del archivo");
        txtBuscar.putClientProperty("JTextField.showClearButton", true);
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { aplicarFiltro(); }
            public void removeUpdate(DocumentEvent e) { aplicarFiltro(); }
            public void changedUpdate(DocumentEvent e) { aplicarFiltro(); }
        });
        filtros.add(txtBuscar);

        cmbTipo = new JComboBox<>(new String[]{TIPO_TODOS, TIPO_FICHA, TIPO_NOTA, TIPO_LISTADO});
        cmbTipo.addActionListener(e -> aplicarFiltro());
        filtros.add(cmbTipo);

        JButton btnActualizar = Tema.botonSecundario("Actualizar", Icono.LIMPIAR);
        btnActualizar.setToolTipText("Vuelve a leer la carpeta de reportes (F5)");
        btnActualizar.addActionListener(e -> cargarDocumentos());
        filtros.add(btnActualizar);
        Tema.atajo(this, KeyStroke.getKeyStroke("F5"), this::cargarDocumentos);

        lblConteo = new JLabel(" ");
        lblConteo.setFont(Tema.TEXTO_CHICO);
        lblConteo.setForeground(Tema.TEXTO_SECUNDARIO);
        filtros.add(lblConteo);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        btnAbrir = Tema.botonPrimario("Abrir", Icono.EXPORTAR);
        btnAbrir.setToolTipText("Abre el documento seleccionado (doble clic o Enter)");
        btnAbrir.addActionListener(e -> abrirSeleccionado());
        acciones.add(btnAbrir);
        btnMostrar = Tema.botonSecundario("Mostrar en carpeta", Icono.CARPETA);
        btnMostrar.addActionListener(e -> mostrarSeleccionadoEnCarpeta());
        acciones.add(btnMostrar);
        JLabel lblAyuda = new JLabel("Doble clic para abrir · clic derecho para más opciones");
        lblAyuda.setFont(Tema.TEXTO_ITALICA_CHICA);
        lblAyuda.setForeground(Tema.TEXTO_SECUNDARIO);
        acciones.add(lblAyuda);

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(filtros, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);
        contenido.add(acciones, BorderLayout.SOUTH);

        JPanel tarjeta = Tema.panelTarjeta("Documentos generados");
        tarjeta.add(contenido, BorderLayout.CENTER);
        actualizarBotones();
        return tarjeta;
    }

    private JPopupMenu crearMenuContextual() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem abrir = new JMenuItem("Abrir");
        abrir.addActionListener(e -> abrirSeleccionado());
        menu.add(abrir);
        JMenuItem mostrar = new JMenuItem("Mostrar en carpeta");
        mostrar.addActionListener(e -> mostrarSeleccionadoEnCarpeta());
        menu.add(mostrar);
        JMenuItem copiarRuta = new JMenuItem("Copiar ubicación");
        copiarRuta.addActionListener(e -> {
            File f = archivoSeleccionado();
            if (f != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(f.getAbsolutePath()), null);
                Tema.mostrarNotificacion(this, "Ubicación copiada");
            }
        });
        menu.add(copiarRuta);
        return menu;
    }

    // ==================== Carga de la lista ====================

    private static final class Documento {
        File archivo;
        String tipo;
        String estudiante;
        long modificado;
    }

    private void cargarDocumentos() {
        Tema.enSegundoPlano(
            this,
            this::leerDocumentos,
            documentos -> {
                modelo.setRowCount(0);
                archivos.clear();
                for (Documento d : documentos) {
                    archivos.add(d.archivo);
                    modelo.addRow(new Object[]{
                        d.archivo.getName(),
                        d.tipo,
                        d.estudiante,
                        new FechaOrdenable(d.modificado),
                        new TamanoOrdenable(d.archivo.length())
                    });
                }
                // Más recientes primero.
                ordenador.setSortKeys(java.util.Collections.singletonList(
                    new RowSorter.SortKey(3, SortOrder.DESCENDING)));
                aplicarFiltro();
            },
            error -> System.out.println("Error leyendo documentos: " + error.getMessage())
        );
    }

    /**
     * Corre en segundo plano. Recorre reportes/ (incluye archivos sueltos de versiones
     * anteriores, que se guardaban todos juntos en la raíz). Cada rol ve solo lo que le
     * corresponde: el/la profesional, las carpetas de sus estudiantes; "Usuario autorizado"
     * no ve fichas ni notas (son clínicas), solo listados.
     */
    private List<Documento> leerDocumentos() {
        Set<Integer> idsPermitidos = null;
        if (util.Sesion.esPsicologo()) {
            idsPermitidos = new HashSet<>();
            for (Paciente p : pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())) {
                idsPermitidos.add(p.getId());
            }
        }
        boolean veClinicos = !util.Sesion.esSecretaria();

        List<Documento> lista = new ArrayList<>();
        File raiz = Carpetas.reportes();

        File[] sueltos = raiz.listFiles(File::isFile);
        if (sueltos != null && util.Sesion.esAdmin()) {
            for (File f : sueltos) {
                lista.add(documento(f, null));
            }
        }

        File[] listados = Carpetas.listados().listFiles(File::isFile);
        if (listados != null) {
            for (File f : listados) {
                lista.add(documento(f, null));
            }
        }

        File[] carpetasEstudiantes = new File(raiz, "Estudiantes").listFiles(File::isDirectory);
        if (carpetasEstudiantes != null && veClinicos) {
            for (File carpeta : carpetasEstudiantes) {
                if (idsPermitidos != null) {
                    Matcher m = ID_EN_CARPETA.matcher(carpeta.getName());
                    if (!m.find() || !idsPermitidos.contains(Integer.parseInt(m.group(1)))) {
                        continue;
                    }
                }
                File[] archivosCarpeta = carpeta.listFiles(File::isFile);
                if (archivosCarpeta != null) {
                    String estudiante = ID_EN_CARPETA.matcher(carpeta.getName()).replaceAll("").trim();
                    for (File f : archivosCarpeta) {
                        lista.add(documento(f, estudiante));
                    }
                }
            }
        }
        return lista;
    }

    private static Documento documento(File f, String estudiante) {
        Documento d = new Documento();
        d.archivo = f;
        d.modificado = f.lastModified();
        String nombre = f.getName();
        if (nombre.startsWith("Ficha_")) {
            d.tipo = TIPO_FICHA;
        } else if (nombre.startsWith("NotaAtencion_")) {
            d.tipo = TIPO_NOTA;
        } else if (nombre.toLowerCase().endsWith(".csv")) {
            d.tipo = TIPO_LISTADO;
        } else {
            d.tipo = TIPO_OTRO;
        }
        d.estudiante = estudiante != null ? estudiante : (TIPO_LISTADO.equals(d.tipo) ? "Varios" : "—");
        return d;
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        String tipo = (String) cmbTipo.getSelectedItem();
        ordenador.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> fila) {
                if (!TIPO_TODOS.equals(tipo) && !tipo.equals(fila.getStringValue(1))) {
                    return false;
                }
                return texto.isEmpty()
                    || fila.getStringValue(0).toLowerCase().contains(texto)
                    || fila.getStringValue(2).toLowerCase().contains(texto);
            }
        });
        int visibles = tabla.getRowCount();
        lblConteo.setText(visibles == modelo.getRowCount()
            ? visibles + (visibles == 1 ? " documento" : " documentos")
            : visibles + " de " + modelo.getRowCount() + " documentos");
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean haySeleccion = tabla.getSelectedRow() >= 0;
        btnAbrir.setEnabled(haySeleccion);
        btnMostrar.setEnabled(haySeleccion);
    }

    private File archivoSeleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : archivos.get(tabla.convertRowIndexToModel(fila));
    }

    private void abrirSeleccionado() {
        File f = archivoSeleccionado();
        if (f == null) {
            return;
        }
        try {
            Carpetas.abrir(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el documento: " + e.getMessage()
                + "\n\nPuede que se haya movido o borrado. Tocá \"Actualizar\".", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarSeleccionadoEnCarpeta() {
        File f = archivoSeleccionado();
        if (f == null) {
            return;
        }
        try {
            Carpetas.mostrarEnCarpeta(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir la carpeta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== Acciones rápidas ====================

    private void exportarEstudiantes() {
        accionExportar.setHabilitada(false);
        Tema.enSegundoPlano(
            this,
            () -> {
                List<Paciente> pacientes = util.Sesion.esPsicologo()
                    ? pacienteDAO.obtenerPorPsicologo(util.Sesion.getUsuarioId())
                    : pacienteDAO.obtenerTodos();
                return pacientes.isEmpty() ? null
                    : util.GeneradorReportes.exportarListadoEstudiantes(pacientes, "todos");
            },
            archivo -> {
                accionExportar.setHabilitada(true);
                if (archivo == null) {
                    JOptionPane.showMessageDialog(this, "No hay estudiantes para exportar", "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                cargarDocumentos();
                Tema.mostrarArchivoGuardado(this, "Estudiantes exportados", archivo);
            },
            error -> {
                accionExportar.setHabilitada(true);
                JOptionPane.showMessageDialog(this, "No se pudo exportar: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    private void respaldarBaseDeDatos() {
        accionRespaldar.setHabilitada(false);
        Tema.enSegundoPlano(
            this,
            util.RespaldoBaseDatos::generarRespaldo,
            ruta -> {
                accionRespaldar.setHabilitada(true);
                if (ruta != null) {
                    Tema.mostrarNotificacion(this, "Respaldo de la base de datos generado");
                } else {
                    JOptionPane.showMessageDialog(this,
                        "No se pudo generar el respaldo. Revisá la consola de la aplicación para el detalle.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            error -> {
                accionRespaldar.setHabilitada(true);
                JOptionPane.showMessageDialog(this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }

    // ==================== Celdas ordenables ====================
    // Se muestran como texto amigable ("24/09/2026 15:30", "12 KB") pero ordenan por el valor
    // real: ordenar las fechas como texto dejaría "01/10" antes que "24/09".

    private static final class FechaOrdenable implements Comparable<FechaOrdenable> {
        final long millis;
        FechaOrdenable(long millis) { this.millis = millis; }
        public int compareTo(FechaOrdenable o) { return Long.compare(millis, o.millis); }
        @Override public String toString() {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(FORMATO_FECHA);
        }
    }

    private static final class TamanoOrdenable implements Comparable<TamanoOrdenable> {
        final long bytes;
        TamanoOrdenable(long bytes) { this.bytes = bytes; }
        public int compareTo(TamanoOrdenable o) { return Long.compare(bytes, o.bytes); }
        @Override public String toString() {
            if (bytes < 1024) {
                return bytes + " B";
            }
            return bytes < 1024 * 1024 ? (bytes / 1024) + " KB" : String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }
}
