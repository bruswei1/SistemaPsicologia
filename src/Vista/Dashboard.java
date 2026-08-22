package Vista;

import dao.*;
import javax.swing.*;
import java.awt.*;

public class Dashboard extends javax.swing.JFrame {

    private TarjetaEstadistica lblPacientes;
    private TarjetaEstadistica lblSesiones;
    private TarjetaEstadistica lblTurnos;
    private TarjetaEstadistica lblPsicologos;

    public Dashboard() {
        setSize(1200, 700);
        setTitle("Dashboard - Sistema de Psicología");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        cargarDatos();
        
        setVisible(true);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(255, 255, 255));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("📊 Dashboard");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(new Color(33, 33, 33));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 0));
        headerPanel.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel("Resumen general del sistema");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(100, 100, 100));
        lblSubtitulo.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 0));
        headerPanel.add(lblSubtitulo);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content - Grid de tarjetas
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(248, 249, 250));
        contentPanel.setLayout(new GridLayout(2, 2, 30, 30));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        lblPacientes = new TarjetaEstadistica("👥 Pacientes", "0", new Color(79, 129, 245));
        contentPanel.add(lblPacientes);

        lblSesiones = new TarjetaEstadistica("📝 Sesiones", "0", new Color(76, 175, 80));
        contentPanel.add(lblSesiones);

        lblTurnos = new TarjetaEstadistica("📅 Turnos", "0", new Color(255, 152, 0));
        contentPanel.add(lblTurnos);

        lblPsicologos = new TarjetaEstadistica("👨‍⚕️ Psicólogos", "0", new Color(156, 39, 176));
        contentPanel.add(lblPsicologos);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(255, 255, 255));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 15));

        JButton btnVolver = new JButton("← Volver");
        btnVolver.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnVolver.setBackground(new Color(79, 129, 245));
        btnVolver.setForeground(Color.WHITE);
        btnVolver.setBorderPainted(false);
        btnVolver.setFocusPainted(false);
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVolver.setPreferredSize(new Dimension(120, 40));
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
                    });

                    System.out.println("✓ Dashboard actualizado: " + totalPacientes + " pacientes");
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
        private String titulo;
        private String numero;
        private Color color;

        public TarjetaEstadistica(String titulo, String numero, Color color) {
            this.titulo = titulo;
            this.numero = numero;
            this.color = color;
            setBackground(new Color(255, 255, 255));
            setOpaque(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
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

            // Fondo blanco
            g2.setColor(new Color(255, 255, 255));
            g2.fillRoundRect(0, 0, w, h, 15, 15);

            // Borde coloreado
            g2.setColor(color);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 15, 15);

            // Línea decorativa superior
            g2.fillRect(0, 0, w, 5);

            // Título
            g2.setColor(new Color(100, 100, 100));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            g2.drawString(titulo, 30, 50);

            // Número grande
            g2.setColor(color);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 56));
            FontMetrics fm = g2.getFontMetrics();
            int numWidth = fm.stringWidth(numero);
            int x = (w - numWidth) / 2;
            g2.drawString(numero, x, 130);
        }
    }
}
