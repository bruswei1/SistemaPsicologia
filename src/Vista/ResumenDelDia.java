package Vista;

import dao.TurnoDAO;
import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Franja "Hoy" del menú principal: las citas del día (del profesional conectado, o de todos
 * para administración / usuario autorizado) como fichas clickeables que abren la ficha del
 * estudiante, con la próxima cita destacada ("En 25 min"). Se refresca al volver al menú y
 * cada minuto, para que el "en X min" y los estados se mantengan al día.
 */
public class ResumenDelDia extends JPanel {

    private static final int MAX_FICHAS = 5;
    private static final Locale ES = new Locale("es");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final IntConsumer alAbrirFicha;
    private final Runnable alVerAgenda;
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblDetalle = new JLabel(" ");
    private final JPanel fichas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private List<TurnoDAO.CitaDelDia> citas = new java.util.ArrayList<>();

    public ResumenDelDia(IntConsumer alAbrirFicha, Runnable alVerAgenda) {
        super(new BorderLayout(16, 0));
        this.alAbrirFicha = alAbrirFicha;
        this.alVerAgenda = alVerAgenda;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 14));

        JComponent insignia = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.esOscuro() ? Tema.oscurecer(Tema.ACENTO_VERDE.icono, 0.45f) : Tema.ACENTO_VERDE.insignia);
                g2.fillRoundRect(0, 0, 44, 44, 12, 12);
                Icono.AGENDA.dibujar(g2, 10, 10, 24, Tema.ACENTO_VERDE.icono);
                g2.dispose();
            }
        };
        insignia.setPreferredSize(new Dimension(44, 44));
        JPanel envInsignia = new JPanel(new GridBagLayout());
        envInsignia.setOpaque(false);
        envInsignia.add(insignia);
        add(envInsignia, BorderLayout.WEST);

        lblTitulo.setFont(Tema.fuente(Font.BOLD, 15));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        lblDetalle.setFont(Tema.TEXTO_CHICO);
        lblDetalle.setForeground(Tema.TEXTO_SECUNDARIO);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblDetalle);

        fichas.setOpaque(false);
        JPanel centro = new JPanel(new BorderLayout(18, 0));
        centro.setOpaque(false);
        centro.add(textos, BorderLayout.WEST);
        centro.add(fichas, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        JButton btnAgenda = Tema.botonSecundario("Ver agenda", Icono.SIGUIENTE);
        btnAgenda.setHorizontalTextPosition(SwingConstants.LEFT);
        btnAgenda.setToolTipText("Abrir la agenda de citas");
        btnAgenda.addActionListener(e -> alVerAgenda.run());
        JPanel envBoton = new JPanel(new GridBagLayout());
        envBoton.setOpaque(false);
        envBoton.add(btnAgenda);
        add(envBoton, BorderLayout.EAST);

        lblTitulo.setText(tituloHoy() + "  ·  cargando citas…");

        // Cada minuto: vuelve a pintar el "en X min" (sin consultar la base); los datos se
        // recargan desde MenuPrincipal al volver al menú y en su propio chequeo periódico.
        Timer reloj = new Timer(60_000, e -> {
            if (!isDisplayable()) {
                ((Timer) e.getSource()).stop(); // la franja se descartó (p. ej. al cambiar el tema)
            } else {
                pintar();
            }
        });
        reloj.start();
    }

    /** Actualiza con citas ya consultadas (el chequeo de recordatorios de MenuPrincipal las comparte). */
    public void setCitas(List<TurnoDAO.CitaDelDia> nuevas) {
        citas = nuevas;
        pintar();
    }

    public List<TurnoDAO.CitaDelDia> getCitas() {
        return citas;
    }

    /** Consulta las citas de hoy en segundo plano y actualiza la franja. */
    public void refrescar(Runnable alTerminar) {
        Integer psicologoId = util.Sesion.esPsicologo() ? util.Sesion.getUsuarioId() : null;
        Tema.enSegundoPlano(
            this,
            () -> new TurnoDAO().obtenerDelDia(LocalDate.now(), psicologoId),
            resultado -> {
                citas = resultado;
                pintar();
                if (alTerminar != null) {
                    alTerminar.run();
                }
            },
            error -> lblDetalle.setText("No se pudieron cargar las citas de hoy")
        );
    }

    private static String tituloHoy() {
        LocalDate hoy = LocalDate.now();
        String dia = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
        String mes = hoy.getMonth().getDisplayName(TextStyle.FULL, ES);
        return "Hoy, " + dia + " " + hoy.getDayOfMonth() + " de " + mes;
    }

    private void pintar() {
        LocalDateTime ahora = LocalDateTime.now();
        TurnoDAO.CitaDelDia proxima = null;
        int pendientes = 0;
        for (TurnoDAO.CitaDelDia c : citas) {
            if ("programado".equals(c.estado) && !c.fechaHora.isBefore(ahora.minusMinutes(c.duracionMinutos))) {
                pendientes++;
                if (proxima == null) {
                    proxima = c;
                }
            }
        }

        int total = citas.size();
        lblTitulo.setText(tituloHoy() + "  ·  " + (total == 0 ? "sin citas" : total == 1 ? "1 cita" : total + " citas"));
        if (total == 0) {
            lblDetalle.setText(util.Sesion.esPsicologo() ? "No tenés citas programadas para hoy" : "No hay citas programadas para hoy");
        } else if (proxima == null) {
            lblDetalle.setText("Ya no quedan citas pendientes hoy");
        } else {
            lblDetalle.setText("Próxima: " + proxima.estudiante + " a las " + proxima.fechaHora.format(HORA)
                + " (" + cuandoFalta(proxima.fechaHora, ahora) + ")"
                + (pendientes > 1 ? "  ·  quedan " + pendientes : ""));
        }

        fichas.removeAll();
        int mostradas = 0;
        for (TurnoDAO.CitaDelDia c : citas) {
            if (mostradas == MAX_FICHAS) {
                JLabel mas = new JLabel("+" + (total - MAX_FICHAS) + " más");
                mas.setFont(Tema.fuente(Font.BOLD, 12));
                mas.setForeground(Tema.TEXTO_SECUNDARIO);
                fichas.add(mas);
                break;
            }
            fichas.add(new FichaCita(c, c == proxima));
            mostradas++;
        }
        revalidate();
        repaint();
    }

    static String cuandoFalta(LocalDateTime cuando, LocalDateTime ahora) {
        long min = Duration.between(ahora, cuando).toMinutes();
        if (min <= 0) {
            return "ahora";
        }
        if (min < 60) {
            return "en " + min + " min";
        }
        long h = min / 60;
        long m = min % 60;
        return "en " + h + " h" + (m > 0 ? " " + m + " min" : "");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Tema.SUPERFICIE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g2.setColor(Tema.BORDE);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g2.dispose();
        super.paintComponent(g);
    }

    /** Ficha de una cita: hora grande + estudiante; color según estado; clic abre la ficha. */
    private final class FichaCita extends JComponent {
        private final TurnoDAO.CitaDelDia cita;
        private final boolean esProxima;
        private boolean hover;

        FichaCita(TurnoDAO.CitaDelDia cita, boolean esProxima) {
            this.cita = cita;
            this.esProxima = esProxima;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(cita.estudiante + (cita.curso != null ? " · " + cita.curso : "") + " · "
                + cita.fechaHora.format(HORA) + " · " + Tema.etiquetaEstadoCita(cita.estado)
                + (util.Sesion.esPsicologo() ? "" : " · " + cita.profesional) + " — clic para abrir la ficha");
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
                    alAbrirFicha.accept(cita.pacienteId);
                }
            });
        }

        private String nombreCorto() {
            String[] partes = cita.estudiante.trim().split("\\s+");
            String n = partes.length >= 2 ? partes[0] + " " + partes[partes.length - 1].charAt(0) + "." : cita.estudiante;
            return n.length() > 16 ? n.substring(0, 15) + "…" : n;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(124, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color acento;
            switch (cita.estado) {
                case "completado": acento = Tema.EXITO; break;
                case "ausente": acento = Tema.PELIGRO; break;
                default: acento = Tema.PRIMARIO;
            }
            boolean terminada = !"programado".equals(cita.estado);
            Color fondo = esProxima
                ? Tema.mezclar(Tema.SUPERFICIE, acento, Tema.esOscuro() ? 0.30f : 0.14f)
                : Tema.mezclar(Tema.SUPERFICIE, acento, hover ? 0.10f : 0.04f);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(hover || esProxima ? acento : Tema.BORDE);
            g2.setStroke(new BasicStroke(esProxima ? 2f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2.setFont(Tema.fuente(Font.BOLD, 14));
            g2.setColor(terminada ? Tema.TEXTO_SECUNDARIO : acento);
            g2.drawString(cita.fechaHora.format(HORA), 10, 19);

            String estado = esProxima ? "PRÓXIMA" : terminada ? Tema.etiquetaEstadoCita(cita.estado).toUpperCase() : "";
            if (!estado.isEmpty()) {
                g2.setFont(Tema.fuente(Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(acento);
                g2.drawString(estado, getWidth() - fm.stringWidth(estado) - 9, 18);
            }

            g2.setFont(Tema.TEXTO_CHICO);
            g2.setColor(terminada ? Tema.TEXTO_SECUNDARIO : Tema.TEXTO_PRIMARIO);
            g2.drawString(nombreCorto(), 10, 37);
            g2.dispose();
        }
    }
}
