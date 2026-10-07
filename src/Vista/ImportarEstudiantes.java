package Vista;

import dao.PacienteDAO;
import dao.UsuarioDAO;
import modelos.Usuario;
import util.Auditoria;
import util.ImportadorEstudiantes;
import util.Sesion;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.util.List;

/**
 * Importar estudiantes desde una planilla de Excel (guardada como CSV), en dos pasos dentro de la
 * misma ventana: (1) cómo preparar el archivo + planilla modelo, (2) vista previa fila por fila
 * con lo que se importa, lo que tiene advertencias y lo que no (y por qué), antes de confirmar.
 */
public final class ImportarEstudiantes {

    private ImportarEstudiantes() {
    }

    /** Paso 1. {@code alTerminar} recibe cuántos estudiantes se importaron. */
    public static void iniciar(Component ancla, java.util.function.IntConsumer alTerminar) {
        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setOpaque(false);
        JLabel explicacion = new JLabel("<html>"
            + "<b>1.</b> En Excel, armá una planilla con una fila por estudiante. La primera fila tiene que tener los "
            + "nombres de las columnas: <b>Nombre</b> y <b>Apellido</b> (obligatorias), y si querés <b>CI</b>, "
            + "<b>Teléfono</b>, <b>Género</b>, <b>Fecha de nacimiento</b> (dd/mm/aaaa), <b>Curso</b>, <b>Tutor/a</b>, "
            + "<b>CI del tutor/a</b>.<br><br>"
            + "<b>2.</b> Guardala con <i>Archivo → Guardar como → CSV</i>.<br><br>"
            + "<b>3.</b> Tocá <b>Elegir archivo</b>. Antes de importar vas a ver fila por fila qué se carga."
            + "<br><br>Si no tenés una planilla, <b>Crear planilla modelo</b> te arma una con las columnas correctas "
            + "(también sirve un listado exportado desde el sistema).</html>");
        explicacion.setFont(Tema.TEXTO);
        explicacion.setForeground(Tema.TEXTO_PRIMARIO);
        explicacion.setVerticalAlignment(SwingConstants.TOP);
        contenido.add(explicacion, BorderLayout.CENTER);

        JButton btnElegir = Tema.botonPrimario("Elegir archivo…", Icono.CARPETA);
        JButton btnModelo = Tema.botonSecundario("Crear planilla modelo", Icono.EXPORTAR);
        Runnable cerrar = Tema.mostrarHoja(ancla, "Importar estudiantes desde Excel",
            "Cargá un curso entero de una vez", contenido, new Dimension(640, 420), btnElegir, btnModelo);

        btnModelo.addActionListener(e -> crearPlanillaModelo(ancla));
        btnElegir.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            selector.setDialogTitle("Elegí la planilla guardada como CSV");
            selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Planillas CSV (*.csv, *.txt)", "csv", "txt"));
            if (selector.showOpenDialog(ancla) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File archivo = selector.getSelectedFile();
            cerrar.run();
            Tema.enSegundoPlano(
                ancla,
                () -> ImportadorEstudiantes.leer(archivo, new PacienteDAO().obtenerTodasLasCi()),
                resultado -> mostrarVistaPrevia(ancla, archivo, resultado, alTerminar),
                error -> JOptionPane.showMessageDialog(ancla, "No se pudo leer el archivo: " + error.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE)
            );
        });
    }

    private static void crearPlanillaModelo(Component ancla) {
        try {
            File archivo = util.Carpetas.archivoUnico(util.Carpetas.listados(), "Planilla_modelo_importar_estudiantes.csv");
            String contenido = "Apellido;Nombre;CI;Teléfono;Género;Fecha de nacimiento;Curso;Tutor/a;CI del tutor/a\r\n"
                + "Pérez;Ana;5123456;0981 000000;Femenino;15/03/2011;"
                + util.EstructuraAcademica.todasLasEtiquetas().get(0) + ";María González;1234567\r\n";
            util.GeneradorReportes.guardarTexto(contenido, archivo, true);
            Tema.mostrarArchivoGuardado(ancla, "Planilla modelo creada", archivo);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(ancla, "No se pudo crear la planilla: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== Paso 2: vista previa ====================

    private static final class ItemProfesional {
        final Integer id;
        final String nombre;

        ItemProfesional(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static void mostrarVistaPrevia(Component ancla, File archivo, ImportadorEstudiantes.Resultado resultado,
                                           java.util.function.IntConsumer alTerminar) {
        if (resultado.columnasReconocidas.isEmpty() || resultado.filas.isEmpty()) {
            JOptionPane.showMessageDialog(ancla, resultado.filas.isEmpty() && !resultado.columnasReconocidas.isEmpty()
                    ? "El archivo no tiene filas de estudiantes debajo de los encabezados."
                    : "No se reconoció ninguna columna. La primera fila tiene que tener los nombres de las columnas "
                        + "(Nombre, Apellido, CI, Curso…).",
                "No hay nada para importar", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Fila", "Apellido", "Nombre", "CI", "Curso", "Estado", "Detalle"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (ImportadorEstudiantes.Fila f : resultado.filas) {
            modelo.addRow(new Object[]{
                f.numeroLinea,
                nulo(f.paciente.getApellido()),
                nulo(f.paciente.getNombre()),
                nulo(f.paciente.getCi()),
                nulo(f.paciente.getCurso()),
                f.estado,
                f.resumen()
            });
        }
        JTable tabla = Tema.crearTablaConVacio(modelo, "Sin filas");
        Tema.estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(0).setMaxWidth(50);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(5).setMinWidth(165);
        tabla.getColumnModel().getColumn(5).setMaxWidth(165);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(320);
        // El detalle puede ser largo: completo al pasar el mouse.
        tabla.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
                lbl.setToolTipText(String.valueOf(v));
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    lbl.setBackground(fila % 2 == 0 ? Tema.SUPERFICIE : Tema.BORDE_SUAVE);
                    lbl.setForeground(Tema.TEXTO_PRIMARIO);
                }
                return lbl;
            }
        });
        tabla.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                ImportadorEstudiantes.Estado estado = (ImportadorEstudiantes.Estado) v;
                String texto = estado == ImportadorEstudiantes.Estado.OK ? "Se importa"
                    : estado == ImportadorEstudiantes.Estado.ADVERTENCIA ? "Se importa (ver detalle)" : "No se importa";
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, texto, sel, foco, fila, col);
                lbl.setFont(Tema.fuente(Font.BOLD, 11));
                lbl.setForeground(estado == ImportadorEstudiantes.Estado.OK ? Tema.EXITO
                    : estado == ImportadorEstudiantes.Estado.ADVERTENCIA ? Tema.ACENTO_AMBAR.icono : Tema.PELIGRO);
                if (!sel) {
                    lbl.setBackground(fila % 2 == 0 ? Tema.SUPERFICIE : Tema.BORDE_SUAVE);
                }
                return lbl;
            }
        });
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        int ok = resultado.cantidad(ImportadorEstudiantes.Estado.OK);
        int advertencias = resultado.cantidad(ImportadorEstudiantes.Estado.ADVERTENCIA);
        int errores = resultado.cantidad(ImportadorEstudiantes.Estado.ERROR);
        int aImportar = ok + advertencias;

        JLabel lblResumen = new JLabel("<html><b>" + aImportar + "</b> estudiante(s) para importar"
            + (advertencias > 0 ? " (" + advertencias + " con advertencias)" : "")
            + (errores > 0 ? " · <b>" + errores + "</b> fila(s) no se importan" : "")
            + "<br><span style='font-size:90%'>Columnas usadas: " + String.join(", ", resultado.columnasReconocidas)
            + (resultado.columnasIgnoradas.isEmpty() ? "" : " · ignoradas: " + String.join(", ", resultado.columnasIgnoradas))
            + "</span></html>");
        lblResumen.setFont(Tema.TEXTO_CHICO);
        lblResumen.setForeground(Tema.TEXTO_PRIMARIO);

        JComboBox<ItemProfesional> cmbProfesional = new JComboBox<>();
        JPanel filaProfesional = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaProfesional.setOpaque(false);
        if (Sesion.esPsicologo()) {
            cmbProfesional.addItem(new ItemProfesional(Sesion.getUsuarioId(), Sesion.getNombre()));
            cmbProfesional.setEnabled(false);
        } else {
            cmbProfesional.addItem(new ItemProfesional(null, "Sin asignar"));
            for (Usuario u : new UsuarioDAO().obtenerPorRol("psicologo")) {
                cmbProfesional.addItem(new ItemProfesional(u.getId(), u.getNombre()));
            }
            if (cmbProfesional.getItemCount() > 1) {
                cmbProfesional.setSelectedIndex(1);
            }
        }
        JLabel lblProfesional = new JLabel("Profesional asignado a todos:");
        lblProfesional.setFont(Tema.fuente(Font.BOLD, 12));
        filaProfesional.add(lblProfesional);
        filaProfesional.add(cmbProfesional);

        JPanel arriba = new JPanel(new BorderLayout(0, 8));
        arriba.setOpaque(false);
        arriba.add(lblResumen, BorderLayout.NORTH);
        arriba.add(filaProfesional, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setOpaque(false);
        contenido.add(arriba, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

        JButton btnImportar = Tema.botonExito(aImportar == 1 ? "Importar 1 estudiante" : "Importar " + aImportar + " estudiantes", Icono.GUARDAR);
        btnImportar.setEnabled(aImportar > 0);
        Runnable cerrar = Tema.mostrarHoja(ancla, "Revisá antes de importar", archivo.getName(), contenido,
            new Dimension(1000, 620), "Cancelar", btnImportar);

        btnImportar.addActionListener(e -> {
            btnImportar.setEnabled(false);
            btnImportar.setText("Importando…");
            ItemProfesional profesional = (ItemProfesional) cmbProfesional.getSelectedItem();
            Integer profesionalId = profesional != null ? profesional.id : null;
            Tema.enSegundoPlano(
                ancla,
                () -> importar(resultado, profesionalId, archivo.getName()),
                importados -> {
                    cerrar.run();
                    alTerminar.accept(importados);
                },
                error -> {
                    btnImportar.setEnabled(true);
                    btnImportar.setText("Reintentar");
                    JOptionPane.showMessageDialog(ancla, "Error al importar: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            );
        });
    }

    /** Corre en segundo plano: da de alta las filas válidas y lo deja registrado en auditoría. */
    private static int importar(ImportadorEstudiantes.Resultado resultado, Integer profesionalId, String nombreArchivo) {
        PacienteDAO dao = new PacienteDAO();
        int importados = 0;
        try (Connection cn = new conexion.Conexion().conectar()) {
            for (ImportadorEstudiantes.Fila f : resultado.filas) {
                if (f.estado == ImportadorEstudiantes.Estado.ERROR) {
                    continue;
                }
                if (profesionalId != null) {
                    f.paciente.setPsicologoId(profesionalId);
                }
                int id = dao.crear(f.paciente);
                if (id > 0) {
                    importados++;
                    Auditoria.registrar(cn, "CREAR_PACIENTE", "pacientes", id, "Importado desde " + nombreArchivo);
                }
            }
            Auditoria.registrar(cn, "IMPORTAR_ESTUDIANTES", "pacientes", null,
                importados + " estudiante(s) desde " + nombreArchivo);
        } catch (java.sql.SQLException e) {
            System.out.println("Error registrando auditoría de la importación: " + e.getMessage());
        }
        return importados;
    }

    private static String nulo(String s) {
        return s != null ? s : "—";
    }
}
