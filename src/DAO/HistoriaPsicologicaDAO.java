package dao;

import conexion.Conexion;
import modelos.HistoriaPsicologica;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoriaPsicologicaDAO {
    private Conexion conexion;

    public HistoriaPsicologicaDAO() {
        this.conexion = new Conexion();
    }

    public boolean crear(HistoriaPsicologica historia) {
        String sql = "INSERT INTO historia_psicologica (paciente_id, psicologo_id, antecedentes, motivo_consulta, " +
                     "observaciones_generales, diagnostico, tratamiento, fecha_creacion, ultima_actualizacion) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, historia.getPacienteId());
            pst.setInt(2, historia.getPsicologoId());
            pst.setString(3, historia.getAntecedentes());
            pst.setString(4, historia.getMotivoConsulta());
            pst.setString(5, historia.getObservacionesGenerales());
            pst.setString(6, historia.getDiagnostico());
            pst.setString(7, historia.getTratamiento());
            pst.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            pst.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al crear historia: " + e.getMessage());
            return false;
        }
    }

    public HistoriaPsicologica obtenerPorPaciente(int pacienteId) {
        String sql = "SELECT * FROM historia_psicologica WHERE paciente_id = ?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, pacienteId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return mapearHistoria(rs);
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener historia: " + e.getMessage());
        }

        return null;
    }

    public boolean actualizar(HistoriaPsicologica historia) {
        String sql = "UPDATE historia_psicologica SET antecedentes=?, motivo_consulta=?, " +
                     "observaciones_generales=?, diagnostico=?, tratamiento=?, ultima_actualizacion=? " +
                     "WHERE id=?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, historia.getAntecedentes());
            pst.setString(2, historia.getMotivoConsulta());
            pst.setString(3, historia.getObservacionesGenerales());
            pst.setString(4, historia.getDiagnostico());
            pst.setString(5, historia.getTratamiento());
            pst.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pst.setInt(7, historia.getId());

            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al actualizar historia: " + e.getMessage());
            return false;
        }
    }

    private HistoriaPsicologica mapearHistoria(ResultSet rs) throws SQLException {
        HistoriaPsicologica h = new HistoriaPsicologica();
        h.setId(rs.getInt("id"));
        h.setPacienteId(rs.getInt("paciente_id"));
        h.setPsicologoId(rs.getInt("psicologo_id"));
        h.setAntecedentes(rs.getString("antecedentes"));
        h.setMotivoConsulta(rs.getString("motivo_consulta"));
        h.setObservacionesGenerales(rs.getString("observaciones_generales"));
        h.setDiagnostico(rs.getString("diagnostico"));
        h.setTratamiento(rs.getString("tratamiento"));
        h.setFechaCreacion(rs.getTimestamp("fecha_creacion") != null ? 
                          rs.getTimestamp("fecha_creacion").toLocalDateTime() : null);
        h.setUltimaActualizacion(rs.getTimestamp("ultima_actualizacion") != null ? 
                                rs.getTimestamp("ultima_actualizacion").toLocalDateTime() : null);
        return h;
    }
}
