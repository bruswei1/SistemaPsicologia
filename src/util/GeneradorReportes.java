package util;

import modelos.Paciente;
import modelos.HistoriaPsicologica;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GeneradorReportes {

    /** CSV con los datos básicos de una lista de pacientes, para respaldo/exportación. */
    public static String generarCSVPacientes(List<Paciente> pacientes) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,nombre,apellido,email,telefono,genero,fecha_nacimiento,curso,motivo_atencion\n");

        for (Paciente p : pacientes) {
            sb.append(p.getId()).append(',')
              .append(csv(p.getNombre())).append(',')
              .append(csv(p.getApellido())).append(',')
              .append(csv(p.getEmail())).append(',')
              .append(csv(p.getTelefono())).append(',')
              .append(csv(p.getGenero())).append(',')
              .append(p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString() : "").append(',')
              .append(csv(p.getCurso())).append(',')
              .append(csv(p.getMotivoConsulta())).append('\n');
        }

        return sb.toString();
    }

    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    public static String obtenerNombreArchivoRespaldo() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "Respaldo_Estudiantes_" + fecha + ".csv";
    }

    public static String generarReportePaciente(Paciente paciente, HistoriaPsicologica historia) {
        StringBuilder sb = new StringBuilder();

        sb.append(crearLinea("=", 80)).append("\n");
        sb.append("FICHA DEL ESTUDIANTE — DEPARTAMENTO DE PSICOLOGÍA\n");
        sb.append(crearLinea("=", 80)).append("\n\n");

        sb.append("DATOS DEL ESTUDIANTE\n");
        sb.append(crearLinea("-", 80)).append("\n");
        sb.append("Nombre: ").append(paciente.getNombre()).append(" ").append(paciente.getApellido()).append("\n");
        sb.append("Curso: ").append(paciente.getCurso() != null ? paciente.getCurso() : "No especificado").append("\n");
        sb.append("Email: ").append(paciente.getEmail()).append("\n");
        sb.append("Teléfono: ").append(paciente.getTelefono()).append("\n");
        sb.append("Género: ").append(paciente.getGenero()).append("\n");
        sb.append("Dirección: ").append(paciente.getDireccion()).append("\n\n");

        if (historia != null) {
            sb.append("INFORMACIÓN DE SEGUIMIENTO\n");
            sb.append(crearLinea("-", 80)).append("\n");

            sb.append("\nANTECEDENTES:\n");
            sb.append(historia.getAntecedentes() != null ? historia.getAntecedentes() : "No especificado").append("\n");

            sb.append("\nMOTIVO DE LA ATENCIÓN:\n");
            sb.append(historia.getMotivoConsulta() != null ? historia.getMotivoConsulta() : "No especificado").append("\n");

            sb.append("\nOBSERVACIONES:\n");
            sb.append(historia.getObservacionesGenerales() != null ? historia.getObservacionesGenerales() : "No especificado").append("\n");

            sb.append("\nDIAGNÓSTICO:\n");
            sb.append(historia.getDiagnostico() != null ? historia.getDiagnostico() : "No especificado").append("\n");

            sb.append("\nPLAN DE INTERVENCIÓN:\n");
            sb.append(historia.getTratamiento() != null ? historia.getTratamiento() : "No especificado").append("\n");
        }
        
        sb.append("\n").append(crearLinea("=", 80)).append("\n");
        sb.append("Fecha de Generación: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append(crearLinea("=", 80)).append("\n");
        
        return sb.toString();
    }

    public static boolean guardarReportePDF(String contenido, String nombreArchivo) throws IOException {
        try (FileWriter fw = new FileWriter(nombreArchivo)) {
            fw.write(contenido);
            return true;
        } catch (IOException e) {
            System.out.println("Error al guardar reporte: " + e.getMessage());
            return false;
        }
    }

    public static String obtenerNombreArchivoReporte(String nombrePaciente) {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "Ficha_" + nombrePaciente.replace(" ", "_") + "_" + fecha + ".txt";
    }
    
    private static String crearLinea(String caracter, int longitud) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longitud; i++) {
            sb.append(caracter);
        }
        return sb.toString();
    }
}
