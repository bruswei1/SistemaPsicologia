package modelos;

import java.time.LocalDate;

public class Paciente {
    private int id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String genero;
    private String direccion;
    private String motivoConsulta;
    private String curso;
    private String antecedentesPersonales;
    private String antecedenteFamiliares;
    private String anamnesis;
    private int psicologoId;

    public Paciente() {}

    public Paciente(String nombre, String apellido, String email, String telefono) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getMotivoConsulta() { return motivoConsulta; }
    public void setMotivoConsulta(String motivoConsulta) { this.motivoConsulta = motivoConsulta; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getAntecedentesPersonales() { return antecedentesPersonales; }
    public void setAntecedentesPersonales(String antecedentesPersonales) { this.antecedentesPersonales = antecedentesPersonales; }

    public String getAntecedenteFamiliares() { return antecedenteFamiliares; }
    public void setAntecedenteFamiliares(String antecedenteFamiliares) { this.antecedenteFamiliares = antecedenteFamiliares; }

    public String getAnamnesis() { return anamnesis; }
    public void setAnamnesis(String anamnesis) { this.anamnesis = anamnesis; }

    public int getPsicologoId() { return psicologoId; }
    public void setPsicologoId(int psicologoId) { this.psicologoId = psicologoId; }

    @Override
    public String toString() {
        return (curso != null && !curso.isEmpty())
            ? nombre + " " + apellido + " — " + curso
            : nombre + " " + apellido;
    }
}
