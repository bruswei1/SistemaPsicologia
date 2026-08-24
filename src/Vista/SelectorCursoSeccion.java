package Vista;

import util.EstructuraAcademica;
import util.EstructuraAcademica.Seleccion;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Selector de curso/sección/turno con listas dependientes: solo permite armar combinaciones
 * que existen en la estructura académica oficial del colegio (ver util.EstructuraAcademica).
 * No admite texto libre ni combinaciones fuera de esa lista.
 */
public class SelectorCursoSeccion extends JPanel {

    private static final String CARTA_EEB = "eeb";
    private static final String CARTA_BACHILLERATO = "bachillerato";

    private final JComboBox<String> comboNivel;
    private final CardLayout cardLayout;
    private final JPanel panelDependiente;

    private final JComboBox<String> comboGrado;
    private final JComboBox<String> comboSeccion;

    private final JComboBox<String> comboAnio;
    private final JComboBox<String> comboModalidad;
    private final JComboBox<String> comboEspecialidad;

    private final JComboBox<String> comboTurno;

    public SelectorCursoSeccion() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        comboNivel = new JComboBox<>(EstructuraAcademica.NIVELES);
        comboGrado = new JComboBox<>(EstructuraAcademica.GRADOS_EEB);
        comboSeccion = new JComboBox<>(EstructuraAcademica.SECCIONES_EEB);
        comboAnio = new JComboBox<>(EstructuraAcademica.ANIOS_BACHILLERATO);
        comboModalidad = new JComboBox<>(EstructuraAcademica.MODALIDADES);
        comboEspecialidad = new JComboBox<>();
        comboTurno = new JComboBox<>();

        int fila = 0;
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        add(new JLabel("Nivel:"), gbc);
        gbc.gridx = 1;
        add(comboNivel, gbc);
        fila++;

        JPanel panelEEB = new JPanel(new GridBagLayout());
        panelEEB.setOpaque(false);
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(4, 4, 4, 4);
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.anchor = GridBagConstraints.WEST;
        g2.gridx = 0;
        g2.gridy = 0;
        panelEEB.add(new JLabel("Grado:"), g2);
        g2.gridx = 1;
        panelEEB.add(comboGrado, g2);
        g2.gridx = 0;
        g2.gridy = 1;
        panelEEB.add(new JLabel("Sección:"), g2);
        g2.gridx = 1;
        panelEEB.add(comboSeccion, g2);

        JPanel panelBachillerato = new JPanel(new GridBagLayout());
        panelBachillerato.setOpaque(false);
        GridBagConstraints g3 = new GridBagConstraints();
        g3.insets = new Insets(4, 4, 4, 4);
        g3.fill = GridBagConstraints.HORIZONTAL;
        g3.anchor = GridBagConstraints.WEST;
        g3.gridx = 0;
        g3.gridy = 0;
        panelBachillerato.add(new JLabel("Año:"), g3);
        g3.gridx = 1;
        panelBachillerato.add(comboAnio, g3);
        g3.gridx = 0;
        g3.gridy = 1;
        panelBachillerato.add(new JLabel("Modalidad:"), g3);
        g3.gridx = 1;
        panelBachillerato.add(comboModalidad, g3);
        g3.gridx = 0;
        g3.gridy = 2;
        panelBachillerato.add(new JLabel("Especialidad:"), g3);
        g3.gridx = 1;
        panelBachillerato.add(comboEspecialidad, g3);

        cardLayout = new CardLayout();
        panelDependiente = new JPanel(cardLayout);
        panelDependiente.setOpaque(false);
        panelDependiente.setBorder(BorderFactory.createEmptyBorder());
        panelDependiente.add(panelEEB, CARTA_EEB);
        panelDependiente.add(panelBachillerato, CARTA_BACHILLERATO);

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 2;
        add(panelDependiente, gbc);
        fila++;

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = fila;
        add(new JLabel("Turno:"), gbc);
        gbc.gridx = 1;
        add(comboTurno, gbc);

        comboNivel.addActionListener(e -> alCambiarNivel());
        comboGrado.addActionListener(e -> alCambiarGradoOSeccion());
        comboSeccion.addActionListener(e -> alCambiarGradoOSeccion());
        comboModalidad.addActionListener(e -> alCambiarModalidad());
        comboEspecialidad.addActionListener(e -> alCambiarEspecialidad());

        alCambiarModalidad();
        alCambiarNivel();
    }

    private void alCambiarNivel() {
        boolean esBachillerato = EstructuraAcademica.NIVEL_BACHILLERATO.equals(comboNivel.getSelectedItem());
        cardLayout.show(panelDependiente, esBachillerato ? CARTA_BACHILLERATO : CARTA_EEB);
        if (esBachillerato) {
            alCambiarEspecialidad();
        } else {
            alCambiarGradoOSeccion();
        }
    }

    private void alCambiarGradoOSeccion() {
        String grado = (String) comboGrado.getSelectedItem();
        String seccion = (String) comboSeccion.getSelectedItem();
        if (grado == null || seccion == null) {
            return;
        }
        repoblar(comboTurno, EstructuraAcademica.turnosValidosEEB(grado, seccion));
    }

    private void alCambiarModalidad() {
        String modalidad = (String) comboModalidad.getSelectedItem();
        repoblar(comboEspecialidad, EstructuraAcademica.especialidadesPorModalidad(modalidad));
        alCambiarEspecialidad();
    }

    private void alCambiarEspecialidad() {
        String especialidad = (String) comboEspecialidad.getSelectedItem();
        if (especialidad == null) {
            return;
        }
        repoblar(comboTurno, EstructuraAcademica.turnosValidosBachillerato(especialidad));
    }

    private void repoblar(JComboBox<String> combo, String[] valores) {
        Object actual = combo.getSelectedItem();
        combo.setModel(new DefaultComboBoxModel<>(valores));
        if (actual != null) {
            for (String v : valores) {
                if (v.equals(actual)) {
                    combo.setSelectedItem(actual);
                    break;
                }
            }
        }
    }

    /** @return la etiqueta canónica de la selección actual, o null si algún combo quedó sin valor. */
    public String getCursoSeleccionado() {
        boolean esBachillerato = EstructuraAcademica.NIVEL_BACHILLERATO.equals(comboNivel.getSelectedItem());
        String turno = (String) comboTurno.getSelectedItem();
        if (turno == null) {
            return null;
        }

        if (esBachillerato) {
            String anio = (String) comboAnio.getSelectedItem();
            String modalidad = (String) comboModalidad.getSelectedItem();
            String especialidad = (String) comboEspecialidad.getSelectedItem();
            if (anio == null || modalidad == null || especialidad == null) {
                return null;
            }
            return EstructuraAcademica.construirEtiquetaBachillerato(anio, modalidad, especialidad, turno);
        }

        String grado = (String) comboGrado.getSelectedItem();
        String seccion = (String) comboSeccion.getSelectedItem();
        if (grado == null || seccion == null) {
            return null;
        }
        return EstructuraAcademica.construirEtiquetaEEB(grado, seccion, turno);
    }

    /** Reconstruye la selección visual a partir de una etiqueta canónica guardada (o la limpia si no reconoce el formato). */
    public void setCursoSeleccionado(String etiqueta) {
        Seleccion s = EstructuraAcademica.parsearEtiqueta(etiqueta);
        if (s == null) {
            comboNivel.setSelectedIndex(0);
            alCambiarNivel();
            return;
        }
        if (s.esBachillerato) {
            comboNivel.setSelectedItem(EstructuraAcademica.NIVEL_BACHILLERATO);
            comboModalidad.setSelectedItem(s.modalidad);
            alCambiarModalidad();
            comboEspecialidad.setSelectedItem(s.especialidad);
            alCambiarEspecialidad();
            comboAnio.setSelectedItem(s.anio);
            comboTurno.setSelectedItem(s.turno);
        } else {
            comboNivel.setSelectedItem(EstructuraAcademica.NIVEL_EEB);
            comboGrado.setSelectedItem(s.grado);
            comboSeccion.setSelectedItem(s.seccion);
            alCambiarGradoOSeccion();
            comboTurno.setSelectedItem(s.turno);
        }
        alCambiarNivel();
    }
}
