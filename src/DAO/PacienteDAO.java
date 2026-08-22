package dao;

import conexion.Conexion;
import modelos.Paciente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PacienteDAO extends DAO {

    public PacienteDAO() {
        super();
    }

    /**
     * Crear nuevo paciente con validaciones
     */
    public boolean crear(Paciente paciente) {
        if (paciente == null || paciente.getNombre() == null) {
            registrarError("crear", new Exception("Paciente o nombre null"));
            return false;
        }

        String sql = "INSERT INTO pacientes (nombre, apellido, email, telefono, fecha_nacimiento, genero, " +
                     "direccion, motivo_consulta, psicologo_id, antecedentes_personales, antecedentes_familiares, anamnesis) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, paciente.getNombre());
            pst.setString(2, paciente.getApellido());
            pst.setString(3, paciente.getEmail());
            pst.setString(4, paciente.getTelefono());
            pst.setDate(5, paciente.getFechaNacimiento() != null ? 
                        Date.valueOf(paciente.getFechaNacimiento()) : null);
            pst.setString(6, paciente.getGenero());
            pst.setString(7, paciente.getDireccion());
            pst.setString(8, paciente.getMotivoConsulta());
            pst.setInt(9, paciente.getPsicologoId() > 0 ? paciente.getPsicologoId() : 0);
            pst.setString(10, paciente.getAntecedentesPersonales());
            pst.setString(11, paciente.getAntecedenteFamiliares());
            pst.setString(12, paciente.getAnamnesis());

            pst.executeUpdate();
            registrarExito("Crear paciente: " + paciente.getNombre());
            return true;

        } catch (SQLException e) {
            registrarError("crear paciente", e);
            return false;
        }
    }

    /**
     * Obtener todos los pacientes
     */
    public List<Paciente> obtenerTodos() {
        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT * FROM pacientes ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                pacientes.add(mapearPaciente(rs));
            }
            registrarExito("Obtener todos los pacientes: " + pacientes.size());

        } catch (SQLException e) {
            registrarError("obtener todos", e);
        }

        return pacientes;
    }

    /**
     * Obtener paciente por ID
     */
    public Paciente obtenerPorId(int id) {
        String sql = "SELECT * FROM pacientes WHERE id = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return mapearPaciente(rs);
            }

        } catch (SQLException e) {
            registrarError("obtener por ID: " + id, e);
        }

        return null;
    }

    /**
     * Actualizar paciente
     */
    public boolean actualizar(Paciente paciente) {
        if (paciente == null || paciente.getId() <= 0) {
            registrarError("actualizar", new Exception("ID inválido"));
            return false;
        }

        String sql = "UPDATE pacientes SET nombre=?, apellido=?, email=?, telefono=?, fecha_nacimiento=?, " +
                     "genero=?, direccion=?, motivo_consulta=?, psicologo_id=?, antecedentes_personales=?, " +
                     "antecedentes_familiares=?, anamnesis=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, paciente.getNombre());
            pst.setString(2, paciente.getApellido());
            pst.setString(3, paciente.getEmail());
            pst.setString(4, paciente.getTelefono());
            pst.setDate(5, paciente.getFechaNacimiento() != null ? 
                        Date.valueOf(paciente.getFechaNacimiento()) : null);
            pst.setString(6, paciente.getGenero());
            pst.setString(7, paciente.getDireccion());
            pst.setString(8, paciente.getMotivoConsulta());
            pst.setInt(9, paciente.getPsicologoId() > 0 ? paciente.getPsicologoId() : 0);
            pst.setString(10, paciente.getAntecedentesPersonales());
            pst.setString(11, paciente.getAntecedenteFamiliares());
            pst.setString(12, paciente.getAnamnesis());
            pst.setInt(13, paciente.getId());

            int filasActualizadas = pst.executeUpdate();
            if (filasActualizadas > 0) {
                registrarExito("Actualizar paciente ID: " + paciente.getId());
                return true;
            }

        } catch (SQLException e) {
            registrarError("actualizar paciente", e);
        }

        return false;
    }

    /**
     * Eliminar paciente
     */
    public boolean eliminar(int id) {
        if (id <= 0) {
            registrarError("eliminar", new Exception("ID inválido"));
            return false;
        }

        String sql = "DELETE FROM pacientes WHERE id = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            int filasEliminadas = pst.executeUpdate();
            
            if (filasEliminadas > 0) {
                registrarExito("Eliminar paciente ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            registrarError("eliminar paciente", e);
        }

        return false;
    }

    /**
     * Buscar paciente por nombre o apellido
     */
    public List<Paciente> buscar(String termino) {
        List<Paciente> pacientes = new ArrayList<>();
        
        if (termino == null || termino.trim().isEmpty()) {
            return obtenerTodos();
        }

        String sql = "SELECT * FROM pacientes WHERE nombre LIKE ? OR apellido LIKE ? ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            String patron = "%" + termino + "%";
            pst.setString(1, patron);
            pst.setString(2, patron);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                pacientes.add(mapearPaciente(rs));
            }
            registrarExito("Buscar pacientes: " + pacientes.size() + " resultados");

        } catch (SQLException e) {
            registrarError("buscar pacientes", e);
        }

        return pacientes;
    }

    /**
     * Verificar si email ya existe
     */
    public boolean existeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) as total FROM pacientes WHERE email = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            registrarError("verificar email", e);
        }

        return false;
    }

    /**
     * Mapear ResultSet a objeto Paciente
     */
    private Paciente mapearPaciente(ResultSet rs) throws SQLException {
        Paciente p = new Paciente();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        p.setEmail(rs.getString("email"));
        p.setTelefono(rs.getString("telefono"));
        p.setFechaNacimiento(rs.getDate("fecha_nacimiento") != null ? 
                            rs.getDate("fecha_nacimiento").toLocalDate() : null);
        p.setGenero(rs.getString("genero"));
        p.setDireccion(rs.getString("direccion"));
        p.setMotivoConsulta(rs.getString("motivo_consulta"));
        p.setAntecedentesPersonales(rs.getString("antecedentes_personales"));
        p.setAntecedenteFamiliares(rs.getString("antecedentes_familiares"));
        p.setAnamnesis(rs.getString("anamnesis"));
        p.setPsicologoId(rs.getInt("psicologo_id"));
        return p;
    }
}
