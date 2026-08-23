package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Lectura de la tabla auditoria (la escritura la hace util.Auditoria.registrar). */
public class AuditoriaDAO extends DAO {

    public AuditoriaDAO() {
        super();
    }

    public static class Registro {
        public final int id;
        public final String usuarioNombre;
        public final String accion;
        public final String entidad;
        public final Integer entidadId;
        public final String detalle;
        public final LocalDateTime fecha;

        Registro(int id, String usuarioNombre, String accion, String entidad,
                 Integer entidadId, String detalle, LocalDateTime fecha) {
            this.id = id;
            this.usuarioNombre = usuarioNombre;
            this.accion = accion;
            this.entidad = entidad;
            this.entidadId = entidadId;
            this.detalle = detalle;
            this.fecha = fecha;
        }
    }

    /** Los `limite` registros más recientes, más nuevos primero. */
    public List<Registro> obtenerRecientes(int limite) {
        List<Registro> lista = new ArrayList<>();
        String sql = "SELECT * FROM auditoria ORDER BY fecha DESC LIMIT ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, limite);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            registrarExito("Obtener auditoría reciente: " + lista.size());

        } catch (SQLException e) {
            registrarError("obtener recientes", e);
        }

        return lista;
    }

    /** Historial de una entidad puntual (ej. todos los cambios de una historia clínica). */
    public List<Registro> obtenerPorEntidad(String entidad, int entidadId) {
        List<Registro> lista = new ArrayList<>();
        String sql = "SELECT * FROM auditoria WHERE entidad = ? AND entidad_id = ? ORDER BY fecha DESC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, entidad);
            pst.setInt(2, entidadId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            registrarExito("Obtener auditoría de " + entidad + " #" + entidadId + ": " + lista.size());

        } catch (SQLException e) {
            registrarError("obtener por entidad", e);
        }

        return lista;
    }

    private Registro mapear(ResultSet rs) throws SQLException {
        int entidadIdRaw = rs.getInt("entidad_id");
        Integer entidadId = rs.wasNull() ? null : entidadIdRaw;

        return new Registro(
            rs.getInt("id"),
            rs.getString("usuario_nombre"),
            rs.getString("accion"),
            rs.getString("entidad"),
            entidadId,
            rs.getString("detalle"),
            rs.getTimestamp("fecha").toLocalDateTime()
        );
    }
}
