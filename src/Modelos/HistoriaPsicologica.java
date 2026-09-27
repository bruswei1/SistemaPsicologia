package modelos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HistoriaPsicologica {
    private int id;
    private int pacienteId;
    private String antecedentes;
    private String motivoConsulta;
    private String observacionesGenerales;
    private String diagnostico;
    private String tratamiento;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;
    private int psicologoId;
    private String codigoCie10;
    private String tipoAcoso;
    private boolean esReiterado;

    public HistoriaPsicologica() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public String getAntecedentes() { return antecedentes; }
    public void setAntecedentes(String antecedentes) { this.antecedentes = antecedentes; }

    public String getMotivoConsulta() { return motivoConsulta; }
    public void setMotivoConsulta(String motivoConsulta) { this.motivoConsulta = motivoConsulta; }

    public String getObservacionesGenerales() { return observacionesGenerales; }
    public void setObservacionesGenerales(String observacionesGenerales) { this.observacionesGenerales = observacionesGenerales; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public int getPsicologoId() { return psicologoId; }
    public void setPsicologoId(int psicologoId) { this.psicologoId = psicologoId; }

    public String getCodigoCie10() { return codigoCie10; }
    public void setCodigoCie10(String codigoCie10) { this.codigoCie10 = codigoCie10; }

    public String getTipoAcoso() { return tipoAcoso; }
    public void setTipoAcoso(String tipoAcoso) { this.tipoAcoso = tipoAcoso; }

    public boolean isEsReiterado() { return esReiterado; }
    public void setEsReiterado(boolean esReiterado) { this.esReiterado = esReiterado; }
}
