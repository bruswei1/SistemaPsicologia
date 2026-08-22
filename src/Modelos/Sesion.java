package modelos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Sesion {
    private int id;
    private int pacienteId;
    private int psicologoId;
    private LocalDate fechaSesion;
    private String horaInicio;
    private String horaFin;
    private String notasSesion;
    private String observaciones;
    private String estado; // programada, completada, cancelada
    private LocalDateTime fechaCreacion;

    public Sesion() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public int getPsicologoId() { return psicologoId; }
    public void setPsicologoId(int psicologoId) { this.psicologoId = psicologoId; }

    public LocalDate getFechaSesion() { return fechaSesion; }
    public void setFechaSesion(LocalDate fechaSesion) { this.fechaSesion = fechaSesion; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    public String getNotasSesion() { return notasSesion; }
    public void setNotasSesion(String notasSesion) { this.notasSesion = notasSesion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
