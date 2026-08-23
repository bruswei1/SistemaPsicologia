package dao;

import modelos.Turno;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TurnoDAO extends DAO {

    public TurnoDAO() {
        super();
    }

    /**
     * Crear nuevo turno.
     * @return el ID generado, o -1 si falló
     */
    public int crear(Turno turno) {
        if (turno == null || turno.getPacienteId() <= 0 || turno.getPsicologoId() <= 0) {
            registrarError("crear", new Exception("Turno inválido"));
            return -1;
        }

        String sql = "INSERT INTO turnos (paciente_id, psicologo_id, fecha_hora, duracion_minutos, estado, notas) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, turno.getPacienteId());
            pst.setInt(2, turno.getPsicologoId());
            pst.setTimestamp(3, Timestamp.valueOf(turno.getFechaHora()));
            pst.setInt(4, turno.getDuracionMinutos());
            pst.setString(5, turno.getEstado());
            pst.setString(6, turno.getNotas());

            pst.executeUpdate();
            registrarExito("Crear turno para paciente: " + turno.getPacienteId());

            try (ResultSet keys = pst.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }

        } catch (SQLException e) {
            registrarError("crear turno", e);
            return -1;
        }
    }

    /**
     * Obtener turnos por fecha
     */
    public List<Turno> obtenerPorFecha(LocalDate fecha) {
        List<Turno> turnos = new ArrayList<>();
        
        if (fecha == null) {
            return turnos;
        }

        String sql = "SELECT * FROM turnos WHERE DATE(fecha_hora) = ? ORDER BY fecha_hora ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, Date.valueOf(fecha));
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                turnos.add(mapearTurno(rs));
            }
            registrarExito("Obtener turnos por fecha: " + turnos.size());

        } catch (SQLException e) {
            registrarError("obtener por fecha", e);
        }

        return turnos;
    }

    /**
     * Obtener turnos por psicólogo
     */
    public List<Turno> obtenerPorPsicologo(int psicologoId) {
        List<Turno> turnos = new ArrayList<>();
        
        if (psicologoId <= 0) {
            return turnos;
        }

        String sql = "SELECT * FROM turnos WHERE psicologo_id = ? ORDER BY fecha_hora DESC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, psicologoId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                turnos.add(mapearTurno(rs));
            }
            registrarExito("Obtener turnos por psicólogo: " + turnos.size());

        } catch (SQLException e) {
            registrarError("obtener por psicólogo", e);
        }

        return turnos;
    }

    /**
     * Obtener todos los turnos pendientes
     */
    public List<Turno> obtenerTodosPendientes() {
        List<Turno> turnos = new ArrayList<>();
        String sql = "SELECT * FROM turnos WHERE estado = 'programado' AND fecha_hora >= NOW() ORDER BY fecha_hora";

        try (Connection con = obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                turnos.add(mapearTurno(rs));
            }
            registrarExito("Obtener turnos pendientes: " + turnos.size());

        } catch (SQLException e) {
            registrarError("obtener pendientes", e);
        }

        return turnos;
    }

    /**
     * Obtener turnos pendientes de un psicólogo específico
     */
    public List<Turno> obtenerPendientesPorPsicologo(int psicologoId) {
        List<Turno> turnos = new ArrayList<>();
        String sql = "SELECT * FROM turnos WHERE psicologo_id = ? AND estado = 'programado' AND fecha_hora >= NOW() ORDER BY fecha_hora";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, psicologoId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                turnos.add(mapearTurno(rs));
            }
            registrarExito("Obtener turnos pendientes por psicólogo " + psicologoId + ": " + turnos.size());

        } catch (SQLException e) {
            registrarError("obtener pendientes por psicólogo: " + psicologoId, e);
        }

        return turnos;
    }

    /**
     * Actualizar turno
     */
    public boolean actualizar(Turno turno) {
        if (turno == null || turno.getId() <= 0) {
            registrarError("actualizar", new Exception("Turno inválido"));
            return false;
        }

        String sql = "UPDATE turnos SET estado=?, notas=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, turno.getEstado());
            pst.setString(2, turno.getNotas());
            pst.setInt(3, turno.getId());

            int filasActualizadas = pst.executeUpdate();
            if (filasActualizadas > 0) {
                registrarExito("Actualizar turno ID: " + turno.getId());
                return true;
            }

        } catch (SQLException e) {
            registrarError("actualizar turno", e);
        }

        return false;
    }

    /**
     * Eliminar turno
     */
    public boolean eliminar(int id) {
        if (id <= 0) {
            registrarError("eliminar", new Exception("ID inválido"));
            return false;
        }

        String sql = "DELETE FROM turnos WHERE id = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            int filasEliminadas = pst.executeUpdate();
            
            if (filasEliminadas > 0) {
                registrarExito("Eliminar turno ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            registrarError("eliminar turno", e);
        }

        return false;
    }

    /**
     * Obtener cantidad de turnos confirmados
     */
    public int obtenerCountTurnosConfirmados() {
        String sql = "SELECT COUNT(*) as total FROM turnos WHERE estado = 'programado'";

        try (Connection con = obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            registrarError("contar confirmados", e);
        }

        return 0;
    }

    /**
     * Mapear ResultSet a objeto Turno
     */
    private Turno mapearTurno(ResultSet rs) throws SQLException {
        Turno t = new Turno();
        t.setId(rs.getInt("id"));
        t.setPacienteId(rs.getInt("paciente_id"));
        t.setPsicologoId(rs.getInt("psicologo_id"));
        t.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
        t.setDuracionMinutos(rs.getInt("duracion_minutos"));
        t.setEstado(rs.getString("estado"));
        t.setNotas(rs.getString("notas"));
        return t;
    }
}
