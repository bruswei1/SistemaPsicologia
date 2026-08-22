package Vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initComponents() {
        setSize(1200, 800);
        
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setBackground(new Color(248, 249, 250));
        panelPrincipal.setLayout(new BorderLayout());

        // Header
        JPanel panelHeader = new JPanel();
        panelHeader.setBackground(new Color(255, 255, 255));
        panelHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        panelHeader.setPreferredSize(new Dimension(0, 100));
        panelHeader.setLayout(new BoxLayout(panelHeader, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Sistema de Psicología");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(33, 33, 33));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 0));
        panelHeader.add(lblTitulo);

        String usuarioInfo = "Usuario";
        String rolInfo = "Admin";
        try {
            usuarioInfo = util.Sesion.getNombre();
            rolInfo = util.Sesion.getRol();
        } catch (Exception e) {
            // Default
        }

        JLabel lblUsuario = new JLabel("Conectado como: " + usuarioInfo + " (" + rolInfo + ")");
        lblUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUsuario.setForeground(new Color(100, 100, 100));
        lblUsuario.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 0));
        panelHeader.add(lblUsuario);

        panelPrincipal.add(panelHeader, BorderLayout.NORTH);

        // Content - Grid de 6 opciones (3x2)
        JPanel panelContent = new JPanel();
        panelContent.setBackground(new Color(248, 249, 250));
        panelContent.setLayout(new GridLayout(2, 3, 30, 30));
        panelContent.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Tarjeta 1: Pacientes
        PanelCard cardPacientes = new PanelCard("👥 PACIENTES", "Registrar y gestionar\npacientes");
        cardPacientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                GestionPacientes gestion = new GestionPacientes();
                gestion.setVisible(true);
                MenuPrincipal.this.dispose();
            }
        });
        panelContent.add(cardPacientes);

        // Tarjeta 2: Turnos
        PanelCard cardTurnos = new PanelCard("📅 TURNOS", "Crear y gestionar\nTurnos");
        cardTurnos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                GestionTurnos turnos = new GestionTurnos();
                turnos.setVisible(true);
                MenuPrincipal.this.dispose();
            }
        });
        panelContent.add(cardTurnos);

        // Tarjeta 3: Sesiones
        PanelCard cardSesiones = new PanelCard("📝 SESIONES", "Registrar notas clínicas\nde sesiones");
        cardSesiones.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                try {
                    if (!util.Sesion.esSecretaria()) {
                        Sesiones sesiones = new Sesiones();
                        sesiones.setVisible(true);
                        MenuPrincipal.this.dispose();
                    } else {
                        JOptionPane.showMessageDialog(MenuPrincipal.this, 
                            "Las notas clínicas no están disponibles para secretarias", 
                            "Acceso denegado", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    Sesiones sesiones = new Sesiones();
                    sesiones.setVisible(true);
                    MenuPrincipal.this.dispose();
                }
            }
        });
        panelContent.add(cardSesiones);

        // Tarjeta 4: Historia Psicológica
        PanelCard cardHistoria = new PanelCard("📋 HISTORIA", "Ver y editar historia\npsicológica");
        cardHistoria.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                HistoriaPsicologicaView historia = new HistoriaPsicologicaView();
                historia.setVisible(true);
                MenuPrincipal.this.dispose();
            }
        });
        panelContent.add(cardHistoria);

        // Tarjeta 5: Dashboard
        PanelCard cardDashboard = new PanelCard("📊 DASHBOARD", "Estadísticas y\nreportes");
        cardDashboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                Dashboard dashboard = new Dashboard();
                dashboard.setVisible(true);
                MenuPrincipal.this.dispose();
            }
        });
        panelContent.add(cardDashboard);

        // Tarjeta 6: Agenda
        PanelCard cardAgenda = new PanelCard("📆 AGENDA", "Ver agenda de citas\npróximas");
        cardAgenda.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                Agenda agenda = new Agenda();
                agenda.setVisible(true);
                MenuPrincipal.this.dispose();
            }
        });
        panelContent.add(cardAgenda);

        panelPrincipal.add(panelContent, BorderLayout.CENTER);

        getContentPane().add(panelPrincipal);
    }

    public static void main(String args[]) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            System.out.println("FlatLaf no disponible, usando Look and Feel por defecto");
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MenuPrincipal().setVisible(true);
            }
        });
    }

    private static class PanelCard extends JPanel {
        private String titulo;
        private String descripcion;
        private boolean mouseEntered = false;

        public PanelCard(String titulo, String descripcion) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            
            setBackground(new Color(255, 255, 255));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setOpaque(true);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mouseEntered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouseEntered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            
            g2.setColor(new Color(255, 255, 255));
            g2.fillRoundRect(0, 0, w, h, 20, 20);

            if (mouseEntered) {
                g2.setColor(new Color(0, 0, 0, 20));
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 5 * i));
                    g2.drawRoundRect(i, i, w - 2*i - 1, h - 2*i - 1, 20, 20);
                }
                
                g2.setColor(new Color(79, 129, 245));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 20, 20);
            } else {
                g2.setColor(new Color(220, 220, 220));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
            }

            g2.setColor(new Color(33, 33, 33));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g2.drawString(titulo, 30, 50);

            g2.setColor(new Color(100, 100, 100));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            String[] lineas = descripcion.split("\n");
            int y = 85;
            for (String linea : lineas) {
                g2.drawString(linea, 30, y);
                y += 25;
            }
        }
    }
}
