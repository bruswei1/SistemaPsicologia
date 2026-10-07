package dao;

import conexion.Conexion;
import modelos.DocumentoPaciente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentoPacienteDAO {
    private Conexion conexion;

    public DocumentoPacienteDAO() {
        this.conexion = new Conexion();
    }

    public int crear(DocumentoPaciente doc) {
        // subido_en explícito con la hora de la PC (el servidor MySQL corre en UTC).
        String sql = "INSERT INTO documentos_paciente (paciente_id, nombre_archivo, ruta_archivo, tipo, subido_por, subido_en) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, doc.getPacienteId());
            pst.setString(2, doc.getNombreArchivo());
            pst.setString(3, doc.getRutaArchivo());
            pst.setString(4, doc.getTipo());
            if (doc.getSubidoPor() != null) {
                pst.setInt(5, doc.getSubidoPor());
            } else {
                pst.setNull(5, Types.INTEGER);
            }
            pst.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));

            pst.executeUpdate();

            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            System.out.println("Error al adjuntar documento: " + e.getMessage());
            return -1;
        }
    }

    public List<DocumentoPaciente> obtenerPorPaciente(int pacienteId) {
        List<DocumentoPaciente> lista = new ArrayList<>();
        String sql = "SELECT * FROM documentos_paciente WHERE paciente_id = ? ORDER BY subido_en DESC";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, pacienteId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener documentos del estudiante: " + e.getMessage());
        }

        return lista;
    }

    public DocumentoPaciente obtenerPorId(int id) {
        String sql = "SELECT * FROM documentos_paciente WHERE id = ?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener documento: " + e.getMessage());
        }

        return null;
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM documentos_paciente WHERE id = ?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al eliminar documento: " + e.getMessage());
            return false;
        }
    }

    private DocumentoPaciente mapear(ResultSet rs) throws SQLException {
        DocumentoPaciente d = new DocumentoPaciente();
        d.setId(rs.getInt("id"));
        d.setPacienteId(rs.getInt("paciente_id"));
        d.setNombreArchivo(rs.getString("nombre_archivo"));
        d.setRutaArchivo(rs.getString("ruta_archivo"));
        d.setTipo(rs.getString("tipo"));
        int subidoPor = rs.getInt("subido_por");
        d.setSubidoPor(rs.wasNull() ? null : subidoPor);
        Timestamp subidoEn = rs.getTimestamp("subido_en");
        d.setSubidoEn(subidoEn != null ? subidoEn.toLocalDateTime() : null);
        return d;
    }
}
