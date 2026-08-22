package dao;

import conexion.Conexion;
import modelos.Sesion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SesionDAO {
    private Conexion conexion;

    public SesionDAO() {
        this.conexion = new Conexion();
    }

    public boolean crear(Sesion sesion) {
        String sql = "INSERT INTO sesiones (paciente_id, psicologo_id, fecha, subjetivo, objetivo, analisis, plan, notas_privadas, duracion_minutos) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, sesion.getPacienteId());
            pst.setInt(2, sesion.getPsicologoId());
            pst.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            pst.setString(4, sesion.getNotasSesion());
            pst.setString(5, sesion.getObservaciones());
            pst.setString(6, "");
            pst.setString(7, "");
            pst.setString(8, "");
            pst.setInt(9, 60);

            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al crear sesión: " + e.getMessage());
            return false;
        }
    }

    public List<Sesion> obtenerPorPaciente(int pacienteId) {
        List<Sesion> sesiones = new ArrayList<>();
        String sql = "SELECT * FROM sesiones WHERE paciente_id = ? ORDER BY fecha DESC";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, pacienteId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                sesiones.add(mapearSesion(rs));
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener sesiones: " + e.getMessage());
        }

        return sesiones;
    }

    public List<Sesion> obtenerPorPsicologo(int psicologoId) {
        List<Sesion> sesiones = new ArrayList<>();
        String sql = "SELECT * FROM sesiones WHERE psicologo_id = ? ORDER BY fecha DESC";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, psicologoId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                sesiones.add(mapearSesion(rs));
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener sesiones por psicólogo: " + e.getMessage());
        }

        return sesiones;
    }

    public int obtenerCountSesionesCompletadas() {
        String sql = "SELECT COUNT(*) as total FROM sesiones";

        try (Connection con = conexion.conectar();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("Error al contar sesiones: " + e.getMessage());
        }

        return 0;
    }

    public boolean actualizar(Sesion sesion) {
        String sql = "UPDATE sesiones SET subjetivo=?, objetivo=?, analisis=?, plan=?, notas_privadas=? WHERE id=?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, sesion.getNotasSesion());
            pst.setString(2, sesion.getObservaciones());
            pst.setString(3, "");
            pst.setString(4, "");
            pst.setString(5, "");
            pst.setInt(6, sesion.getId());

            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al actualizar sesión: " + e.getMessage());
            return false;
        }
    }

    private Sesion mapearSesion(ResultSet rs) throws SQLException {
        Sesion s = new Sesion();
        s.setId(rs.getInt("id"));
        s.setPacienteId(rs.getInt("paciente_id"));
        s.setPsicologoId(rs.getInt("psicologo_id"));
        s.setNotasSesion(rs.getString("subjetivo"));
        s.setObservaciones(rs.getString("objetivo"));
        s.setFechaCreacion(rs.getTimestamp("fecha").toLocalDateTime());
        return s;
    }
}
