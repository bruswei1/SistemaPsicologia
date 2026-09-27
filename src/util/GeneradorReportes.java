package util;

import modelos.Paciente;
import modelos.HistoriaPsicologica;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GeneradorReportes {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Excel en español (configuración regional de Paraguay) usa ";" como separador de listas:
     * con "," todo el CSV se abría en una sola columna. Se escribe además con BOM UTF-8 (ver
     * {@link #guardarTexto}) — sin él, Excel lo lee como ANSI y rompe tildes y eñes.
     */
    private static final char SEPARADOR_CSV = ';';

    private static final String AVISO_CONFIDENCIAL =
        "Documento confidencial. Uso exclusivo del Departamento de Psicología.\n"
        + "Contiene datos sensibles de un/a estudiante: no difundir ni compartir fuera del ámbito profesional.";

    /** CSV con los datos básicos de una lista de estudiantes, listo para abrir en Excel. */
    public static String generarCSVPacientes(List<Paciente> pacientes) {
        // El motivo de atención es información clínica: "Usuario autorizado" (secretaria) no la ve
        // en ninguna pantalla, así que tampoco puede salir en la planilla que exporta.
        boolean incluirMotivo = !Sesion.esSecretaria();
        StringBuilder sb = new StringBuilder();
        String[] encabezado = {"ID", "Apellido", "Nombre", "CI", "Teléfono", "Género", "Fecha de nacimiento",
            "Curso", "Tutor/a", "CI del tutor/a", "Motivo de atención"};
        filaCSV(sb, incluirMotivo ? encabezado : java.util.Arrays.copyOf(encabezado, encabezado.length - 1));

        for (Paciente p : pacientes) {
            String[] fila = {
                String.valueOf(p.getId()),
                p.getApellido(),
                p.getNombre(),
                p.getCi(),
                p.getTelefono(),
                p.getGenero(),
                p.getFechaNacimiento() != null ? p.getFechaNacimiento().format(FORMATO_FECHA) : "",
                p.getCurso(),
                p.getNombreTutor(),
                p.getCiTutor(),
                p.getMotivoConsulta()};
            filaCSV(sb, incluirMotivo ? fila : java.util.Arrays.copyOf(fila, fila.length - 1));
        }

        return sb.toString();
    }

    private static void filaCSV(StringBuilder sb, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR_CSV);
            }
            sb.append(csv(valores[i]));
        }
        sb.append("\r\n");
    }

    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        // Un valor que empieza con = + - @ Excel lo interpreta como fórmula (inyección CSV):
        // se antepone un apóstrofo para que quede como texto.
        if (!valor.isEmpty() && "=+-@".indexOf(valor.charAt(0)) >= 0) {
            valor = "'" + valor;
        }
        if (valor.indexOf(SEPARADOR_CSV) >= 0 || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    public static String generarReportePaciente(Paciente paciente, HistoriaPsicologica historia) {
        StringBuilder sb = new StringBuilder();

        sb.append(crearLinea("=", 80)).append("\n");
        sb.append("FICHA DEL ESTUDIANTE — DEPARTAMENTO DE PSICOLOGÍA\n");
        sb.append(crearLinea("=", 80)).append("\n\n");

        sb.append("DATOS DEL ESTUDIANTE\n");
        sb.append(crearLinea("-", 80)).append("\n");
        sb.append("Nombre: ").append(paciente.getNombre()).append(" ").append(paciente.getApellido()).append("\n");
        sb.append("Curso: ").append(valorONoEspecificado(paciente.getCurso())).append("\n");
        sb.append("CI: ").append(valorONoEspecificado(paciente.getCi())).append("\n");
        sb.append("Teléfono: ").append(valorONoEspecificado(paciente.getTelefono())).append("\n");
        sb.append("Género: ").append(valorONoEspecificado(paciente.getGenero())).append("\n");
        sb.append("Dirección: ").append(valorONoEspecificado(paciente.getDireccion())).append("\n");
        sb.append("Consentimiento del tutor: ").append(paciente.isConsentimientoTutor() ? "Sí" : "No registrado").append("\n\n");

        if (historia != null) {
            sb.append("INFORMACIÓN DE SEGUIMIENTO\n");
            sb.append(crearLinea("-", 80)).append("\n");

            sb.append("\nANTECEDENTES:\n");
            sb.append(valorONoEspecificado(historia.getAntecedentes())).append("\n");

            sb.append("\nMOTIVO DE LA ATENCIÓN:\n");
            sb.append(valorONoEspecificado(historia.getMotivoConsulta())).append("\n");

            sb.append("\nOBSERVACIONES:\n");
            sb.append(valorONoEspecificado(historia.getObservacionesGenerales())).append("\n");

            sb.append("\nDIAGNÓSTICO SITUACIONAL:\n");
            sb.append(valorONoEspecificado(historia.getDiagnostico())).append("\n");
            if (historia.getCodigoCie10() != null && !historia.getCodigoCie10().trim().isEmpty()) {
                sb.append("Código CIE-10: ").append(historia.getCodigoCie10()).append("\n");
            }

            sb.append("\nPLAN DE INTERVENCIÓN:\n");
            sb.append(valorONoEspecificado(historia.getTratamiento())).append("\n");

            if (historia.getTipoAcoso() != null && !historia.getTipoAcoso().trim().isEmpty()) {
                sb.append("\nCLASIFICACIÓN (Ley 4633/2012 de acoso escolar):\n");
                sb.append("Tipo: ").append(util.TipoAcosoEscolar.etiqueta(historia.getTipoAcoso())).append("\n");
                sb.append("¿Situación reiterada?: ").append(historia.isEsReiterado() ? "Sí" : "No").append("\n");
            }
        }

        agregarPie(sb);
        return sb.toString();
    }

    /**
     * Nota de una atención con las etiquetas escolares de los campos SOAP. Las notas privadas
     * no se incluyen a propósito: no forman parte del informe.
     *
     * @param guardada false si se exporta un borrador que todavía no se guardó en el sistema.
     */
    public static String generarNotaAtencion(Paciente paciente, boolean guardada,
            String subjetivo, String objetivo, String analisis, String plan) {
        StringBuilder sb = new StringBuilder();
        sb.append(crearLinea("=", 80)).append("\n");
        sb.append("NOTA DE ATENCIÓN PSICOLÓGICA\n");
        sb.append(crearLinea("=", 80)).append("\n\n");
        sb.append("Estudiante: ").append(paciente.getNombre()).append(" ").append(paciente.getApellido()).append("\n");
        if (paciente.getCurso() != null && !paciente.getCurso().isEmpty()) {
            sb.append("Curso: ").append(paciente.getCurso()).append("\n");
        }
        if (!guardada) {
            sb.append("Estado: BORRADOR — esta atención todavía no fue guardada en el sistema\n");
        }
        sb.append("\n");

        seccion(sb, "RELATO DEL ESTUDIANTE (SUBJETIVO)", subjetivo);
        seccion(sb, "OBSERVACIÓN PROFESIONAL (OBJETIVO)", objetivo);
        seccion(sb, "ANÁLISIS DE LA SITUACIÓN", analisis);
        seccion(sb, "ACUERDOS Y RECOMENDACIONES (PLAN)", plan);

        agregarPie(sb);
        return sb.toString();
    }

    private static void seccion(StringBuilder sb, String titulo, String contenido) {
        sb.append(titulo).append("\n");
        sb.append(crearLinea("-", 80)).append("\n");
        sb.append(valorONoEspecificado(contenido != null ? contenido.trim() : null)).append("\n\n");
    }

    private static void agregarPie(StringBuilder sb) {
        sb.append("\n").append(crearLinea("=", 80)).append("\n");
        sb.append("Generado por: ").append(Sesion.getNombre() != null ? Sesion.getNombre() : "—").append("\n");
        sb.append("Fecha de generación: ").append(LocalDateTime.now().format(FORMATO_FECHA_HORA)).append("\n\n");
        sb.append(AVISO_CONFIDENCIAL).append("\n");
        sb.append(crearLinea("=", 80)).append("\n");
    }

    /**
     * Escribe el texto en UTF-8 explícito (FileWriter usa la codificación por defecto del
     * sistema, que en Windows no siempre es UTF-8: las tildes y eñes quedaban corruptas).
     *
     * @param conBom true para los CSV: Excel necesita el BOM para detectar UTF-8.
     */
    public static void guardarTexto(String contenido, File archivo, boolean conBom) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            if (conBom) {
                w.write('\uFEFF');
            }
            w.write(contenido);
        }
    }

    /**
     * Genera el CSV en reportes/Listados/ y lo registra en auditoría (EXPORTAR_ESTUDIANTES).
     * Sin UI: pensado para correr en el hilo de fondo de {@code Tema.enSegundoPlano}.
     *
     * @param descripcion qué se exportó, para el log (ej. "todos", "filtro: 9° EEB").
     */
    public static File exportarListadoEstudiantes(List<Paciente> pacientes, String descripcion) throws IOException {
        File archivo = Carpetas.archivoUnico(Carpetas.listados(), nombreArchivoListadoEstudiantes());
        guardarTexto(generarCSVPacientes(pacientes), archivo, true);
        try (java.sql.Connection cn = new conexion.Conexion().conectar()) {
            Auditoria.registrar(cn, "EXPORTAR_ESTUDIANTES", "pacientes", null,
                pacientes.size() + " estudiantes (" + descripcion + ") → " + archivo.getName());
        } catch (Exception ex) {
            System.out.println("Error registrando auditoría: " + ex.getMessage());
        }
        return archivo;
    }

    /** "Ficha_Perez_Juan_20260924_153000.txt" — sin caracteres inválidos para Windows. */
    public static String nombreArchivoEstudiante(String prefijo, Paciente paciente, String extension) {
        String nombre = Carpetas.nombreSeguro(paciente.getApellido() + " " + paciente.getNombre()).replace(' ', '_');
        return prefijo + "_" + nombre + "_" + LocalDateTime.now().format(FORMATO_ARCHIVO) + "." + extension;
    }

    public static String nombreArchivoListadoEstudiantes() {
        return "Estudiantes_" + LocalDateTime.now().format(FORMATO_ARCHIVO) + ".csv";
    }

    private static String valorONoEspecificado(String valor) {
        return valor != null && !valor.trim().isEmpty() ? valor : "No especificado";
    }

    private static String crearLinea(String caracter, int longitud) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longitud; i++) {
            sb.append(caracter);
        }
        return sb.toString();
    }
}
