package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * La gestión de turnos (crear/editar/eliminar/listar) vive en Vista.Agenda, que
 * trabaja directo con JDBC. Este DAO solo sirve al conteo que usa el Dashboard.
 */
public class TurnoDAO extends DAO {

    public TurnoDAO() {
        super();
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

    public int obtenerCountTurnosConfirmadosDesde(LocalDate desde) {
        String sql = "SELECT COUNT(*) as total FROM turnos WHERE estado = 'programado' AND fecha_hora >= ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, Date.valueOf(desde));
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            registrarError("contar confirmados desde fecha", e);
        }

        return 0;
    }
}
