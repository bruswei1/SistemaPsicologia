package Vista;

import javax.swing.*;
import java.awt.*;

/**
 * Sección "Estudiantes" del menú: lista/CRUD ({@link GestionPacientes}), agenda de citas
 * ({@link Agenda}) y reportes/archivos ({@link ReportesArchivos}) en pestañas. La ficha de un
 * estudiante ({@link DetallePaciente}) se abre acá mismo, reemplazando la lista, en vez de en
 * una ventana aparte; "Volver a la lista" o Esc regresan.
 */
public class EstudiantesPanel extends javax.swing.JPanel {

    private static final String CARTA_LISTA = "lista";
    private static final String CARTA_FICHA = "ficha";

    private final GestionPacientes panelLista;
    private final Agenda panelAgenda;
    private final ReportesArchivos panelReportes;
    private final CardLayout cartas = new CardLayout();
    private JTabbedPane tabbedPane;
    private DetallePaciente fichaAbierta;

    public EstudiantesPanel(Runnable alVolver) {
        this.panelLista = new GestionPacientes(alVolver, false);
        this.panelAgenda = new Agenda(alVolver, false);
        this.panelReportes = new ReportesArchivos(alVolver, false);
        panelLista.setAlAbrirFicha(this::abrirFicha);
        initComponents(alVolver);
    }

    public void refrescar() {
        panelLista.refrescar();
        panelAgenda.refrescar();
        panelReportes.refrescar();
    }

    private void initComponents(Runnable alVolver) {
        setLayout(cartas);

        JPanel vistaLista = new JPanel(new BorderLayout());
        vistaLista.add(Tema.panelEncabezado("Estudiantes", "Datos, seguimiento, atenciones, agenda y reportes"), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Tema.SUPERFICIE);
        tabbedPane.setFont(Tema.TEXTO_CHICO);

        tabbedPane.addTab("Lista de Estudiantes", new IconoSwing(Icono.PACIENTES, Tema.PRIMARIO, 16), panelLista);
        tabbedPane.addTab("Agenda", new IconoSwing(Icono.AGENDA, Tema.PRIMARIO, 16), panelAgenda);
        tabbedPane.addTab("Reportes y archivos", new IconoSwing(Icono.EXPORTAR, Tema.PRIMARIO, 16), panelReportes);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == panelReportes) {
                panelReportes.refrescar();
            }
        });

        vistaLista.add(tabbedPane, BorderLayout.CENTER);
        vistaLista.add(crearFooter(vistaLista, alVolver), BorderLayout.SOUTH);
        add(vistaLista, CARTA_LISTA);
    }

    private JPanel crearFooter(JPanel vistaLista, Runnable alVolver) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        footer.setBackground(Tema.FONDO);

        JButton btnVolver = Tema.botonSecundario("Volver al menú", Icono.VOLVER);
        btnVolver.addActionListener(e -> alVolver.run());
        footer.add(btnVolver);
        // Esc primero cierra el panel lateral abierto (estudiante o cita); si no, vuelve al menú.
        Tema.atajoEscape(vistaLista, btnVolver, () -> {
            if (panelLista.isShowing() && panelLista.formularioAbierto()) {
                panelLista.cerrarFormulario();
            } else if (panelAgenda.isShowing() && panelAgenda.formularioAbierto()) {
                panelAgenda.cerrarFormulario();
            } else {
                alVolver.run();
            }
        });

        return footer;
    }

    /**
     * Muestra la ficha del estudiante en lugar de la lista, con un fundido. Si ya hay otra ficha
     * abierta con algo sin guardar, primero pregunta.
     */
    void abrirFicha(int pacienteId) {
        if (fichaAbierta != null) {
            fichaAbierta.salir(() -> abrirFichaSinPreguntar(pacienteId));
        } else {
            abrirFichaSinPreguntar(pacienteId);
        }
    }

    private void abrirFichaSinPreguntar(int pacienteId) {
        Tema.conCursorEspera(this, () -> {
            DetallePaciente ficha = new DetallePaciente(pacienteId,
                () -> panelLista.editarEstudiante(pacienteId), this::volverALista);
            if (!ficha.estudianteEncontrado()) {
                JOptionPane.showMessageDialog(this, "El estudiante ya no existe (puede que otro usuario lo haya eliminado).",
                    "Estudiante no encontrado", JOptionPane.WARNING_MESSAGE);
                panelLista.refrescar();
                return;
            }
            if (fichaAbierta != null) {
                remove(fichaAbierta);
            }
            fichaAbierta = ficha;
            add(ficha, CARTA_FICHA);
            BusquedaRapida.registrarReciente(ficha.getPaciente());
            Tema.cambiarConFundido(SwingUtilities.getWindowAncestor(this), this, cartas, CARTA_FICHA);
        });
    }

    /** true si hay una atención en curso (cronómetro andando) en la ficha abierta. */
    boolean hayAtencionEnCurso() {
        return fichaAbierta != null && fichaAbierta.cronometroEnMarcha();
    }

    /** true si hay algo escrito y sin guardar (ficha abierta o panel de nuevo/editar estudiante). */
    boolean hayCambiosSinGuardar() {
        return (fichaAbierta != null && fichaAbierta.tieneBorrador()) || panelLista.tieneCambiosSinGuardar();
    }

    /** Muestra la pestaña Agenda (p. ej. desde el Panel o el resumen "Hoy" del menú). */
    void mostrarPestanaAgenda() {
        irALista(() -> tabbedPane.setSelectedComponent(panelAgenda));
    }

    /** Muestra la lista filtrada por un curso (null = todos), p. ej. desde "Estudiantes por curso" del Panel. */
    void mostrarListaFiltrada(String curso) {
        irALista(() -> {
            tabbedPane.setSelectedComponent(panelLista);
            panelLista.filtrarPorCurso(curso);
        });
    }

    /** Si hay una ficha abierta, la cierra (preguntando si tiene algo sin guardar) y después sigue. */
    private void irALista(Runnable despues) {
        if (fichaAbierta == null) {
            despues.run();
            return;
        }
        fichaAbierta.salir(() -> {
            DetallePaciente cerrada = fichaAbierta;
            fichaAbierta = null;
            cartas.show(this, CARTA_LISTA);
            remove(cerrada);
            despues.run();
        });
    }

    private void volverALista() {
        tabbedPane.setSelectedComponent(panelLista);
        Tema.cambiarConFundido(SwingUtilities.getWindowAncestor(this), this, cartas, CARTA_LISTA, true);
        if (fichaAbierta != null) {
            DetallePaciente cerrada = fichaAbierta;
            fichaAbierta = null;
            SwingUtilities.invokeLater(() -> remove(cerrada));
        }
        // La columna "Alerta" puede haber cambiado con lo que se cargó en la ficha.
        panelLista.refrescar();
    }
}
