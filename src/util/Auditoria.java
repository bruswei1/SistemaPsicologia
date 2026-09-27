package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

public final class Auditoria {

    private Auditoria() {
    }

    public static void registrar(Connection cn, String accion, String entidad, Integer entidadId, String detalle) throws SQLException {

        // `fecha` se manda explícita con la hora de la PC en vez de dejar que la ponga
        // MySQL (DEFAULT CURRENT_TIMESTAMP): el servidor de la base (contenedor Docker) corre en
        // UTC, 3 horas adelantado respecto a la hora local de Paraguay — sin esto, todo el
        // historial de auditoría quedaba con la hora del servidor, no la real.
        String sql =
            "INSERT INTO auditoria (usuario_id, usuario_nombre, accion, entidad, entidad_id, detalle, fecha) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {

            if (Sesion.getUsuarioId() != null) {
                ps.setInt(1, Sesion.getUsuarioId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, Sesion.getNombre() != null ? Sesion.getNombre() : "desconocido");
            ps.setString(3, accion);
            ps.setString(4, entidad);

            if (entidadId != null) {
                ps.setInt(5, entidadId);
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            if (detalle != null) {
                ps.setString(6, detalle);
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        }
    }
}
