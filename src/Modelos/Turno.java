package modelos;

import java.time.LocalDateTime;

public class Turno {
    private int id;
    private int pacienteId;
    private int psicologoId;
    private LocalDateTime fechaHora;
    private int duracionMinutos;
    private String estado; // programado, completado, cancelado
    private String notas;

    public Turno() {}

    public Turno(int pacienteId, int psicologoId, LocalDateTime fechaHora) {
        this.pacienteId = pacienteId;
        this.psicologoId = psicologoId;
        this.fechaHora = fechaHora;
        this.estado = "programado";
        this.duracionMinutos = 45;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public int getPsicologoId() { return psicologoId; }
    public void setPsicologoId(int psicologoId) { this.psicologoId = psicologoId; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
