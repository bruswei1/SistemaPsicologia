package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        return obtenerCountTurnosConfirmadosDesde(desde, null);
    }

    /** @param psicologoId si no es null, solo las citas de ese profesional */
    public int obtenerCountTurnosConfirmadosDesde(LocalDate desde, Integer psicologoId) {
        String sql = "SELECT COUNT(*) as total FROM turnos WHERE estado = 'programado' AND fecha_hora >= ?"
            + (psicologoId != null ? " AND psicologo_id = ?" : "");

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, Date.valueOf(desde));
            if (psicologoId != null) {
                pst.setInt(2, psicologoId);
            }
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

    /**
     * Citas "programado" cuya fecha/hora ya pasó (nadie actualizó el estado ni cargó una
     * atención) — se muestra como "Casos pendientes" en el Panel General.
     * Si psicologoId no es null, se limita a las citas de ese psicólogo.
     */
    public int contarProgramadosVencidos(Integer psicologoId) {
        // Hora de la PC, no NOW() de MySQL: el servidor corre en UTC (3 h adelantado respecto a
        // Paraguay) y fecha_hora se guarda con la hora local — con NOW() una cita recién pasada
        // tardaba 3 horas en contarse como pendiente.
        String sql = "SELECT COUNT(*) AS total FROM turnos WHERE estado = 'programado' AND fecha_hora < ?"
            + (psicologoId != null ? " AND psicologo_id = ?" : "");

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (psicologoId != null) {
                pst.setInt(2, psicologoId);
            }
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            registrarError("contar programados vencidos", e);
        }

        return 0;
    }

    /**
     * Próximas citas "programado" (fecha_hora >= ahora), para el panel "Próximas citas" del
     * Dashboard. Si psicologoId no es null, se limita a las citas de ese psicólogo.
     */
    public List<ProximaCita> obtenerProximas(int limite, Integer psicologoId) {
        List<ProximaCita> resultado = new ArrayList<>();
        // Hora de la PC en vez de NOW() (servidor en UTC): ver contarProgramadosVencidos.
        String sql = "SELECT p.id AS paciente_id, CONCAT(p.nombre,' ',p.apellido) AS estudiante, p.curso AS curso, "
            + "u.nombre AS profesional, t.fecha_hora "
            + "FROM turnos t JOIN pacientes p ON p.id = t.paciente_id JOIN usuarios u ON u.id = t.psicologo_id "
            + "WHERE t.estado = 'programado' AND t.fecha_hora >= ? "
            + (psicologoId != null ? "AND t.psicologo_id = ? " : "")
            + "ORDER BY t.fecha_hora ASC LIMIT ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            int indice = 1;
            pst.setTimestamp(indice++, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (psicologoId != null) {
                pst.setInt(indice++, psicologoId);
            }
            pst.setInt(indice, limite);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new ProximaCita(
                        rs.getInt("paciente_id"),
                        rs.getString("estudiante"),
                        rs.getString("curso"),
                        rs.getString("profesional"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime()
                    ));
                }
            }

        } catch (SQLException e) {
            registrarError("obtener próximas citas", e);
        }

        return resultado;
    }

    /**
     * Citas de un día (todas menos las canceladas), ordenadas por hora — para el resumen "Hoy"
     * del menú principal y los recordatorios. Si psicologoId no es null, solo las de ese profesional.
     */
    public List<CitaDelDia> obtenerDelDia(LocalDate dia, Integer psicologoId) {
        List<CitaDelDia> resultado = new ArrayList<>();
        String sql = "SELECT t.id, t.paciente_id, CONCAT(p.nombre,' ',p.apellido) AS estudiante, p.curso, "
            + "u.nombre AS profesional, t.fecha_hora, t.duracion_minutos, t.estado "
            + "FROM turnos t JOIN pacientes p ON p.id = t.paciente_id JOIN usuarios u ON u.id = t.psicologo_id "
            + "WHERE t.fecha_hora >= ? AND t.fecha_hora < ? AND t.estado <> 'cancelado' "
            + (psicologoId != null ? "AND t.psicologo_id = ? " : "")
            + "ORDER BY t.fecha_hora ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setTimestamp(1, java.sql.Timestamp.valueOf(dia.atStartOfDay()));
            pst.setTimestamp(2, java.sql.Timestamp.valueOf(dia.plusDays(1).atStartOfDay()));
            if (psicologoId != null) {
                pst.setInt(3, psicologoId);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new CitaDelDia(
                        rs.getInt("id"),
                        rs.getInt("paciente_id"),
                        rs.getString("estudiante"),
                        rs.getString("curso"),
                        rs.getString("profesional"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getInt("duracion_minutos"),
                        rs.getString("estado")
                    ));
                }
            }
        } catch (SQLException e) {
            registrarError("obtener citas del día", e);
        }
        return resultado;
    }

    public static class CitaDelDia {
        public final int turnoId;
        public final int pacienteId;
        public final String estudiante;
        public final String curso;
        public final String profesional;
        public final java.time.LocalDateTime fechaHora;
        public final int duracionMinutos;
        /** Valor crudo de la base (programado/completado/ausente): mostrar con Tema.etiquetaEstadoCita. */
        public final String estado;

        public CitaDelDia(int turnoId, int pacienteId, String estudiante, String curso, String profesional,
                          java.time.LocalDateTime fechaHora, int duracionMinutos, String estado) {
            this.turnoId = turnoId;
            this.pacienteId = pacienteId;
            this.estudiante = estudiante;
            this.curso = curso;
            this.profesional = profesional;
            this.fechaHora = fechaHora;
            this.duracionMinutos = duracionMinutos;
            this.estado = estado;
        }
    }

    /** Fila liviana para el panel "Próximas citas" del Dashboard (no es un Modelos.Turno completo). */
    public static class ProximaCita {
        public final int pacienteId;
        public final String estudiante;
        public final String curso;
        public final String profesional;
        public final java.time.LocalDateTime fechaHora;

        public ProximaCita(int pacienteId, String estudiante, String curso, String profesional, java.time.LocalDateTime fechaHora) {
            this.pacienteId = pacienteId;
            this.estudiante = estudiante;
            this.curso = curso;
            this.profesional = profesional;
            this.fechaHora = fechaHora;
        }
    }
}
