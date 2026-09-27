package modelos;

import java.time.LocalDateTime;

/** Metadata de un archivo adjunto a un estudiante (documentos_paciente). El archivo en sí vive
 * en disco bajo adjuntos/&lt;pacienteId&gt;/, no como blob en la base de datos. */
public class DocumentoPaciente {
    private int id;
    private int pacienteId;
    private String nombreArchivo;
    private String rutaArchivo;
    private String tipo;
    private Integer subidoPor;
    private LocalDateTime subidoEn;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getSubidoPor() { return subidoPor; }
    public void setSubidoPor(Integer subidoPor) { this.subidoPor = subidoPor; }

    public LocalDateTime getSubidoEn() { return subidoEn; }
    public void setSubidoEn(LocalDateTime subidoEn) { this.subidoEn = subidoEn; }
}
