package Vista;

import dao.*;
import javax.swing.*;
import java.awt.*;

public class Dashboard extends javax.swing.JFrame {

    private TarjetaEstadistica lblPacientes;
    private TarjetaEstadistica lblSesiones;
    private TarjetaEstadistica lblTurnos;
    private TarjetaEstadistica lblPsicologos;
    private GraficoBarras grafico;

    public Dashboard() {
        setSize(1200, 800);
        setTitle("Dashboard - Sistema de Psicología");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Tema.iconoApp());
        setLocationRelativeTo(null);

        initComponents();
        cargarDatos();

        setVisible(true);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Tema.FONDO);
        mainPanel.setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Tema.SUPERFICIE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Dashboard");
        lblTitulo.setFont(Tema.TITULO);
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 0));
        headerPanel.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel("Resumen general del sistema");
        lblSubtitulo.setFont(Tema.TEXTO_CHICO);
        lblSubtitulo.setForeground(Tema.TEXTO_SECUNDARIO);
        lblSubtitulo.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 0));
        headerPanel.add(lblSubtitulo);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Centro: tarjetas + gráfico
        JPanel centro = new JPanel(new BorderLayout(0, 20));
        centro.setBackground(Tema.FONDO);
        centro.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Tema.FONDO);
        contentPanel.setLayout(new GridLayout(1, 4, 20, 0));

        lblPacientes = new TarjetaEstadistica(Icono.PACIENTES, "Pacientes", "0", Tema.ACENTO_AZUL.icono);
        contentPanel.add(lblPacientes);

        lblSesiones = new TarjetaEstadistica(Icono.SESIONES, "Sesiones", "0", Tema.ACENTO_MORADO.icono);
        contentPanel.add(lblSesiones);

        lblTurnos = new TarjetaEstadistica(Icono.TURNOS, "Turnos", "0", Tema.ACENTO_VERDE.icono);
        contentPanel.add(lblTurnos);

        lblPsicologos = new TarjetaEstadistica(Icono.PACIENTES, "Psicólogos", "0", Tema.ACENTO_CELESTE.icono);
        contentPanel.add(lblPsicologos);

        centro.add(contentPanel, BorderLayout.NORTH);

        grafico = new GraficoBarras(
            new String[]{"Pacientes", "Sesiones", "Turnos", "Psicólogos"},
            new Color[]{Tema.ACENTO_AZUL.icono, Tema.ACENTO_MORADO.icono, Tema.ACENTO_VERDE.icono, Tema.ACENTO_CELESTE.icono}
        );
        JPanel panelGrafico = new JPanel(new BorderLayout());
        panelGrafico.setBackground(Tema.SUPERFICIE);
        panelGrafico.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDE),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        panelGrafico.add(grafico, BorderLayout.CENTER);
        centro.add(panelGrafico, BorderLayout.CENTER);

        mainPanel.add(centro, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Tema.SUPERFICIE);
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE_SUAVE));
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 15));

        JButton btnVolver = Tema.botonPrimario("Volver", Icono.VOLVER);
        btnVolver.addActionListener(e -> {
            this.dispose();
            MenuPrincipal menu = new MenuPrincipal();
            menu.setVisible(true);
        });
        footerPanel.add(btnVolver);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);
    }

    private void cargarDatos() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    PacienteDAO pacienteDAO = new PacienteDAO();
                    SesionDAO sesionDAO = new SesionDAO();
                    TurnoDAO turnoDAO = new TurnoDAO();
                    UsuarioDAO usuarioDAO = new UsuarioDAO();

                    int totalPacientes = pacienteDAO.obtenerTodos().size();
                    int totalSesiones = sesionDAO.obtenerCountSesionesCompletadas();
                    int totalTurnos = turnoDAO.obtenerCountTurnosConfirmados();
                    int totalPsicologos = usuarioDAO.obtenerPorRol("psicologo").size();

                    SwingUtilities.invokeLater(() -> {
                        lblPacientes.setNumero(String.valueOf(totalPacientes));
                        lblSesiones.setNumero(String.valueOf(totalSesiones));
                        lblTurnos.setNumero(String.valueOf(totalTurnos));
                        lblPsicologos.setNumero(String.valueOf(totalPsicologos));
                        grafico.setValores(new int[]{totalPacientes, totalSesiones, totalTurnos, totalPsicologos});
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

    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            System.out.println("FlatLaf no disponible");
        }

        SwingUtilities.invokeLater(() -> new Dashboard());
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
        }

        public void setNumero(String numero) {
            this.numero = numero;
            this.repaint();
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

            g2.setColor(Tema.BORDE);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 15, 15);

            g2.setColor(color);
            g2.fillRect(0, 0, w, 5);

            int insignia = 36;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
            g2.fillRoundRect(20, 25, insignia, insignia, 10, 10);
            icono.dibujar(g2, 26, 31, insignia - 12, color);

            g2.setColor(Tema.TEXTO_SECUNDARIO);
            g2.setFont(Tema.TEXTO);
            g2.drawString(titulo, 20, 90);

            g2.setColor(color);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 46));
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

            g2.setColor(Tema.BORDE);
            g2.drawLine(10, ejeY, w - 10, ejeY);

            for (int i = 0; i < n; i++) {
                int cx = espacio * i + espacio / 2;
                int valor = i < valores.length ? valores[i] : 0;
                int barH = (int) ((ejeY - 20) * (valor / (double) max));
                int barX = cx - barW / 2;
                int barY = ejeY - barH;

                g2.setColor(colores[i % colores.length]);
                g2.fillRoundRect(barX, barY, barW, Math.max(barH, 2), 8, 8);

                g2.setColor(Tema.TEXTO_PRIMARIO);
                g2.setFont(Tema.BOTON);
                FontMetrics fm = g2.getFontMetrics();
                String texto = String.valueOf(valor);
                g2.drawString(texto, cx - fm.stringWidth(texto) / 2, barY - 8);

                g2.setColor(Tema.TEXTO_SECUNDARIO);
                g2.setFont(Tema.TEXTO_CHICO);
                fm = g2.getFontMetrics();
                g2.drawString(etiquetas[i], cx - fm.stringWidth(etiquetas[i]) / 2, ejeY + 20);
            }
        }
    }
}
