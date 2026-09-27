package Vista;

import dao.PacienteDAO;
import modelos.Paciente;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Búsqueda rápida de estudiantes (Ctrl+K desde cualquier pantalla): una hoja dentro de la misma
 * ventana con un buscador que filtra mientras se escribe. Flechas para moverse, Enter para abrir
 * la ficha, Esc para cerrar. Con el buscador vacío muestra los últimos estudiantes abiertos.
 *
 * Respeta los permisos: el/la profesional solo encuentra a sus estudiantes asignados.
 */
public final class BusquedaRapida {

    private static final int MAX_RECIENTES = 6;
    private static final int MAX_RESULTADOS = 30;

    /** Últimos estudiantes abiertos en esta sesión (el más reciente primero). */
    private static final LinkedList<Paciente> RECIENTES = new LinkedList<>();

    private BusquedaRapida() {
    }

    /** Registra que se abrió la ficha de este estudiante (para "Recientes"). */
    public static void registrarReciente(Paciente p) {
        if (p == null) {
            return;
        }
        RECIENTES.removeIf(r -> r.getId() == p.getId());
        RECIENTES.addFirst(p);
        while (RECIENTES.size() > MAX_RECIENTES) {
            RECIENTES.removeLast();
        }
    }

    /** Se borra al cerrar sesión (los recientes son de quien estaba conectado). */
    public static void olvidarRecientes() {
        RECIENTES.clear();
    }

    public static void mostrar(Component ancla, IntConsumer alElegir) {
        DefaultListModel<Paciente> modelo = new DefaultListModel<>();
        JList<Paciente> lista = new JList<>(modelo);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RendererEstudiante());
        lista.setFixedCellHeight(52);
        lista.setOpaque(false);
        lista.setFocusable(false); // el foco queda siempre en el buscador; las flechas mueven la lista

        JTextField txtBuscar = new JTextField();
        txtBuscar.setFont(Tema.fuente(Font.PLAIN, 16));
        txtBuscar.putClientProperty("JTextField.placeholderText", "Nombre, apellido, CI o teléfono del estudiante");
        txtBuscar.putClientProperty("JTextField.leadingIcon", new IconoSwing(Icono.BUSCAR, Tema.TEXTO_SECUNDARIO, 18));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.PRIMARIO, 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel lblEstado = new JLabel(" ");
        lblEstado.setFont(Tema.fuente(Font.BOLD, 11));
        lblEstado.setForeground(Tema.TEXTO_SECUNDARIO);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(12, 2, 6, 0));

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        JLabel lblAyuda = new JLabel("↑ ↓ para moverte  ·  Enter abre la ficha  ·  Esc cierra");
        lblAyuda.setFont(Tema.TEXTO_CHICO);
        lblAyuda.setForeground(Tema.TEXTO_SECUNDARIO);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(lblEstado, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);
        centro.add(lblAyuda, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setOpaque(false);
        contenido.add(txtBuscar, BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);

        Runnable cerrar = Tema.mostrarHoja(ancla, "Buscar estudiante", "Abrí la ficha de cualquier estudiante desde donde estés",
            contenido, new Dimension(640, 560));

        Runnable abrirSeleccionado = () -> {
            Paciente p = lista.getSelectedValue();
            if (p != null) {
                cerrar.run();
                alElegir.accept(p.getId());
            }
        };

        // --- Búsqueda en vivo (en segundo plano; gana siempre la última tecla) ---
        final int[] consulta = {0};
        // Enter antes de que lleguen los resultados: se abre el primero apenas lleguen.
        final boolean[] buscando = {false};
        final boolean[] abrirAlLlegar = {false};
        Runnable mostrarRecientes = () -> {
            modelo.clear();
            for (Paciente p : RECIENTES) {
                modelo.addElement(p);
            }
            lblEstado.setText(RECIENTES.isEmpty() ? "EMPEZÁ A ESCRIBIR PARA BUSCAR" : "ABIERTOS RECIENTEMENTE");
            if (!modelo.isEmpty()) {
                lista.setSelectedIndex(0);
            }
        };
        Runnable buscar = () -> {
            String termino = txtBuscar.getText().trim();
            if (termino.isEmpty()) {
                mostrarRecientes.run();
                return;
            }
            int numero = ++consulta[0];
            buscando[0] = true;
            lblEstado.setText("BUSCANDO…");
            Tema.enSegundoPlano(
                contenido,
                () -> {
                    PacienteDAO dao = new PacienteDAO();
                    return util.Sesion.esPsicologo()
                        ? dao.buscarPorPsicologo(termino, util.Sesion.getUsuarioId())
                        : dao.buscar(termino);
                },
                resultados -> {
                    if (numero != consulta[0]) {
                        return; // llegó tarde: ya se escribió otra cosa
                    }
                    buscando[0] = false;
                    modelo.clear();
                    List<Paciente> recorte = resultados.size() > MAX_RESULTADOS
                        ? new ArrayList<>(resultados.subList(0, MAX_RESULTADOS)) : resultados;
                    for (Paciente p : recorte) {
                        modelo.addElement(p);
                    }
                    if (resultados.isEmpty()) {
                        lblEstado.setText("NINGÚN ESTUDIANTE COINCIDE CON \"" + termino.toUpperCase() + "\"");
                    } else {
                        lblEstado.setText(resultados.size() == 1 ? "1 ESTUDIANTE"
                            : resultados.size() > MAX_RESULTADOS ? "MOSTRANDO " + MAX_RESULTADOS + " DE " + resultados.size() + " · SEGUÍ ESCRIBIENDO PARA ACOTAR"
                            : resultados.size() + " ESTUDIANTES");
                        lista.setSelectedIndex(0);
                        if (abrirAlLlegar[0]) {
                            abrirAlLlegar[0] = false;
                            abrirSeleccionado.run();
                        }
                    }
                    abrirAlLlegar[0] = false;
                },
                error -> {
                    buscando[0] = false;
                    abrirAlLlegar[0] = false;
                    lblEstado.setText("NO SE PUDO BUSCAR: " + error.getMessage());
                }
            );
        };
        Tema.alEscribir(txtBuscar, 200, buscar);
        mostrarRecientes.run();

        // --- Teclado y mouse ---
        txtBuscar.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "bajar");
        txtBuscar.getActionMap().put("bajar", accion(() -> mover(lista, 1)));
        txtBuscar.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "subir");
        txtBuscar.getActionMap().put("subir", accion(() -> mover(lista, -1)));
        txtBuscar.addActionListener(e -> {
            boolean textoSinBuscar = !txtBuscar.getText().trim().isEmpty() && modelo.isEmpty();
            if (buscando[0] || textoSinBuscar) {
                abrirAlLlegar[0] = true;
                if (!buscando[0]) {
                    buscar.run();
                }
            } else {
                abrirSeleccionado.run();
            }
        });
        lista.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int i = lista.locationToIndex(e.getPoint());
                if (i >= 0) {
                    lista.setSelectedIndex(i);
                    abrirSeleccionado.run();
                }
            }
        });
        lista.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int i = lista.locationToIndex(e.getPoint());
                if (i >= 0 && i != lista.getSelectedIndex()) {
                    lista.setSelectedIndex(i);
                }
            }
        });
        lista.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Después del foco que pone la hoja por defecto.
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(txtBuscar::requestFocusInWindow));
    }

    private static void mover(JList<Paciente> lista, int delta) {
        int n = lista.getModel().getSize();
        if (n == 0) {
            return;
        }
        int i = Math.max(0, Math.min(n - 1, lista.getSelectedIndex() + delta));
        lista.setSelectedIndex(i);
        lista.ensureIndexIsVisible(i);
    }

    private static Action accion(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        };
    }

    /** Fila de resultado: iniciales en un círculo + "Apellido, Nombre" + curso/CI. */
    private static final class RendererEstudiante extends JPanel implements ListCellRenderer<Paciente> {
        private final JLabel lblNombre = new JLabel();
        private final JLabel lblDetalle = new JLabel();
        private String iniciales = "";
        private boolean seleccionada;

        RendererEstudiante() {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 52, 6, 10));
            lblNombre.setFont(Tema.fuente(Font.BOLD, 14));
            lblDetalle.setFont(Tema.TEXTO_CHICO);
            JPanel textos = new JPanel(new GridLayout(2, 1));
            textos.setOpaque(false);
            textos.add(lblNombre);
            textos.add(lblDetalle);
            add(textos, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Paciente> list, Paciente p, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String apellido = p.getApellido() != null ? p.getApellido() : "";
            String nombre = p.getNombre() != null ? p.getNombre() : "";
            lblNombre.setText(apellido + ", " + nombre);
            lblNombre.setForeground(Tema.TEXTO_PRIMARIO);
            String curso = p.getCurso() != null && !p.getCurso().isEmpty() ? p.getCurso() : "Sin curso asignado";
            lblDetalle.setText(curso + (p.getCi() != null && !p.getCi().isEmpty() ? "  ·  CI " + p.getCi() : ""));
            lblDetalle.setForeground(Tema.TEXTO_SECUNDARIO);
            iniciales = (nombre.isEmpty() ? "" : nombre.substring(0, 1)) + (apellido.isEmpty() ? "" : apellido.substring(0, 1));
            iniciales = iniciales.toUpperCase();
            seleccionada = isSelected;
            setOpaque(false);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (seleccionada) {
                g2.setColor(new Color(Tema.PRIMARIO.getRed(), Tema.PRIMARIO.getGreen(), Tema.PRIMARIO.getBlue(), 38));
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 12, 12);
                g2.setColor(Tema.PRIMARIO);
                g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
            }
            int d = 34;
            int y = (getHeight() - d) / 2;
            g2.setColor(Tema.esOscuro() ? Tema.oscurecer(Tema.ACENTO_AZUL.icono, 0.3f) : Tema.ACENTO_AZUL.insignia);
            g2.fillOval(10, y, d, d);
            g2.setColor(Tema.esOscuro() ? Color.WHITE : Tema.ACENTO_AZUL.icono);
            g2.setFont(Tema.fuente(Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(iniciales, 10 + (d - fm.stringWidth(iniciales)) / 2, y + (d + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
