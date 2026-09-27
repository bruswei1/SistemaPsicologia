package dao;

import conexion.Conexion;
import modelos.HistoriaPsicologica;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoriaPsicologicaDAO {

    /**
     * Días sin una nueva entrada de Seguimiento antes de considerar "vencido" un caso marcado
     * como reiterado (Ley 4633/2012) — evita que un caso de acoso activo quede sin revisión.
     */
    public static final int DIAS_UMBRAL_SEGUIMIENTO = 15;

    private Conexion conexion;

    public HistoriaPsicologicaDAO() {
        this.conexion = new Conexion();
    }

    /** Crea una nueva entrada de seguimiento (una fila por atención registrada, no una por estudiante). */
    public int crear(HistoriaPsicologica historia) {
        String sql = "INSERT INTO historia_psicologica (paciente_id, psicologo_id, antecedentes, motivo_consulta, " +
                     "observaciones_generales, diagnostico, tratamiento, fecha_creacion, ultima_actualizacion, " +
                     "codigo_cie10, tipo_acoso, es_reiterado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, historia.getPacienteId());
            pst.setInt(2, historia.getPsicologoId());
            pst.setString(3, historia.getAntecedentes());
            pst.setString(4, historia.getMotivoConsulta());
            pst.setString(5, historia.getObservacionesGenerales());
            pst.setString(6, historia.getDiagnostico());
            pst.setString(7, historia.getTratamiento());
            pst.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            pst.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            if (historia.getCodigoCie10() == null || historia.getCodigoCie10().trim().isEmpty()) {
                pst.setNull(10, Types.VARCHAR);
            } else {
                pst.setString(10, historia.getCodigoCie10().trim());
            }
            if (historia.getTipoAcoso() == null || historia.getTipoAcoso().trim().isEmpty()) {
                pst.setNull(11, Types.VARCHAR);
            } else {
                pst.setString(11, historia.getTipoAcoso());
            }
            pst.setBoolean(12, historia.isEsReiterado());

            pst.executeUpdate();

            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            System.out.println("Error al crear historia: " + e.getMessage());
            return -1;
        }
    }

    /** Última entrada de seguimiento del estudiante (por si algún llamador solo necesita la más reciente). */
    public HistoriaPsicologica obtenerPorPaciente(int pacienteId) {
        String sql = "SELECT * FROM historia_psicologica WHERE paciente_id = ? ORDER BY fecha_creacion DESC LIMIT 1";

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

    /** Todas las entradas de seguimiento del estudiante, más recientes primero. */
    public List<HistoriaPsicologica> obtenerListaPorPaciente(int pacienteId) {
        List<HistoriaPsicologica> lista = new ArrayList<>();
        String sql = "SELECT * FROM historia_psicologica WHERE paciente_id = ? ORDER BY fecha_creacion DESC";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, pacienteId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearHistoria(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener historial de seguimiento: " + e.getMessage());
        }

        return lista;
    }

    public HistoriaPsicologica obtenerPorId(int id) {
        String sql = "SELECT * FROM historia_psicologica WHERE id = ?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapearHistoria(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener entrada de seguimiento: " + e.getMessage());
        }

        return null;
    }

    public boolean actualizar(HistoriaPsicologica historia) {
        String sql = "UPDATE historia_psicologica SET antecedentes=?, motivo_consulta=?, " +
                     "observaciones_generales=?, diagnostico=?, tratamiento=?, ultima_actualizacion=?, " +
                     "codigo_cie10=?, tipo_acoso=?, es_reiterado=? " +
                     "WHERE id=?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, historia.getAntecedentes());
            pst.setString(2, historia.getMotivoConsulta());
            pst.setString(3, historia.getObservacionesGenerales());
            pst.setString(4, historia.getDiagnostico());
            pst.setString(5, historia.getTratamiento());
            pst.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            if (historia.getCodigoCie10() == null || historia.getCodigoCie10().trim().isEmpty()) {
                pst.setNull(7, Types.VARCHAR);
            } else {
                pst.setString(7, historia.getCodigoCie10().trim());
            }
            if (historia.getTipoAcoso() == null || historia.getTipoAcoso().trim().isEmpty()) {
                pst.setNull(8, Types.VARCHAR);
            } else {
                pst.setString(8, historia.getTipoAcoso());
            }
            pst.setBoolean(9, historia.isEsReiterado());
            pst.setInt(10, historia.getId());

            pst.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al actualizar historia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cantidad de casos por motivo de atención, para el Panel. `motivo_consulta` es texto libre
     * (el combo de motivos frecuentes puede combinar varias líneas en un mismo registro), así que
     * se cuenta línea por línea: cada línea que coincide con un preset de
     * {@link util.MotivosAtencion#PRESETS} se agrupa bajo ese preset; cualquier otra línea con
     * contenido se agrupa bajo {@link util.MotivosAtencion#OTRO}. Solo devuelve motivos con al
     * menos un caso, de mayor a menor.
     */
    public java.util.LinkedHashMap<String, Integer> contarPorMotivo(java.time.LocalDate desde, Integer psicologoId) {
        java.util.Map<String, Integer> conteo = new java.util.LinkedHashMap<>();

        // JOIN con pacientes para poder acotar por psicólogo asignado — mismo criterio de
        // acceso que el resto del Panel (contarProgramadosVencidos, obtenerProximas, etc.):
        // un psicólogo no debe ver el desglose de motivos de estudiantes ajenos.
        String sql = "SELECT hp.motivo_consulta FROM historia_psicologica hp "
            + "JOIN pacientes p ON p.id = hp.paciente_id "
            + "WHERE hp.motivo_consulta IS NOT NULL AND hp.motivo_consulta <> '' AND hp.fecha_creacion >= ? "
            + (psicologoId != null ? "AND p.psicologo_id = ? " : "");

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            if (psicologoId != null) {
                pst.setInt(2, psicologoId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String motivo = rs.getString("motivo_consulta");
                    if (motivo == null) {
                        continue;
                    }
                    for (String linea : motivo.split("\n")) {
                        String limpio = linea.trim();
                        if (limpio.isEmpty()) {
                            continue;
                        }
                        String clave = util.MotivosAtencion.OTRO;
                        for (String preset : util.MotivosAtencion.PRESETS) {
                            if (preset.equalsIgnoreCase(limpio)) {
                                clave = preset;
                                break;
                            }
                        }
                        conteo.merge(clave, 1, Integer::sum);
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al contar por motivo: " + e.getMessage());
        }

        java.util.LinkedHashMap<String, Integer> resultado = new java.util.LinkedHashMap<>();
        conteo.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> resultado.put(e.getKey(), e.getValue()));
        return resultado;
    }

    /**
     * Consulta compartida por {@link #contarCasosReiteradosSinSeguimiento} y
     * {@link #obtenerCasosReiteradosSinSeguimiento}: para cada estudiante, mira su entrada de
     * Seguimiento MÁS RECIENTE; si esa entrada está marcada como reiterada (Ley 4633/2012) y
     * tiene más de {@code diasUmbral} días sin que se haya cargado una entrada más nueva, el caso
     * se considera "sin seguimiento reciente". Si `psicologoId` no es null, se limita a los
     * estudiantes asignados a ese psicólogo (mismo criterio que el resto del sistema).
     */
    private String sqlCasosReiteradosSinSeguimiento(Integer psicologoId) {
        // Se identifica "la entrada más reciente" por MAX(id) (clave única autoincremental), no
        // por MAX(fecha_creacion): fecha_creacion tiene precisión de segundo, así que dos
        // entradas guardadas en el mismo segundo empatarían y el JOIN por igualdad de fecha
        // duplicaría o perdería estudiantes.
        return "SELECT p.id AS paciente_id, p.nombre, p.apellido, p.curso, hp.fecha_creacion "
            + "FROM historia_psicologica hp "
            + "INNER JOIN ("
            + "    SELECT paciente_id, MAX(id) AS ultimo_id "
            + "    FROM historia_psicologica GROUP BY paciente_id"
            + ") ultima ON hp.paciente_id = ultima.paciente_id AND hp.id = ultima.ultimo_id "
            + "INNER JOIN pacientes p ON p.id = hp.paciente_id "
            + "WHERE hp.es_reiterado = 1 AND hp.fecha_creacion < ? "
            + (psicologoId != null ? "AND p.psicologo_id = ? " : "")
            + "ORDER BY hp.fecha_creacion ASC";
    }

    public int contarCasosReiteradosSinSeguimiento(Integer psicologoId, int diasUmbral) {
        String sql = "SELECT COUNT(*) AS total FROM (" + sqlCasosReiteradosSinSeguimiento(psicologoId) + ") x";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(diasUmbral)));
            if (psicologoId != null) {
                pst.setInt(2, psicologoId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al contar casos reiterados sin seguimiento: " + e.getMessage());
        }

        return 0;
    }

    /** IDs de estudiantes con un caso reiterado sin seguimiento reciente, para marcarlos en la tabla de Estudiantes. */
    public java.util.Set<Integer> obtenerIdsConSeguimientoVencido(Integer psicologoId, int diasUmbral) {
        java.util.Set<Integer> resultado = new java.util.HashSet<>();
        String sql = sqlCasosReiteradosSinSeguimiento(psicologoId);

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(diasUmbral)));
            if (psicologoId != null) {
                pst.setInt(2, psicologoId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(rs.getInt("paciente_id"));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener ids con seguimiento vencido: " + e.getMessage());
        }

        return resultado;
    }

    public List<CasoPendiente> obtenerCasosReiteradosSinSeguimiento(int limite, Integer psicologoId, int diasUmbral) {
        List<CasoPendiente> resultado = new ArrayList<>();
        String sql = sqlCasosReiteradosSinSeguimiento(psicologoId) + " LIMIT ?";

        try (Connection con = conexion.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            int indice = 1;
            pst.setTimestamp(indice++, Timestamp.valueOf(LocalDateTime.now().minusDays(diasUmbral)));
            if (psicologoId != null) {
                pst.setInt(indice++, psicologoId);
            }
            pst.setInt(indice, limite);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new CasoPendiente(
                        rs.getInt("paciente_id"),
                        rs.getString("nombre") + " " + rs.getString("apellido"),
                        rs.getString("curso"),
                        rs.getTimestamp("fecha_creacion").toLocalDateTime()
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener casos reiterados sin seguimiento: " + e.getMessage());
        }

        return resultado;
    }

    public static class CasoPendiente {
        public final int pacienteId;
        public final String estudiante;
        public final String curso;
        public final LocalDateTime ultimoSeguimiento;

        public CasoPendiente(int pacienteId, String estudiante, String curso, LocalDateTime ultimoSeguimiento) {
            this.pacienteId = pacienteId;
            this.estudiante = estudiante;
            this.curso = curso;
            this.ultimoSeguimiento = ultimoSeguimiento;
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
        h.setCodigoCie10(rs.getString("codigo_cie10"));
        h.setTipoAcoso(rs.getString("tipo_acoso"));
        h.setEsReiterado(rs.getBoolean("es_reiterado"));
        return h;
    }
}
