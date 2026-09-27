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
     * Crear nuevo paciente con validaciones.
     * @return el ID generado, o -1 si falló
     */
    public int crear(Paciente paciente) {
        if (paciente == null || paciente.getNombre() == null) {
            registrarError("crear", new Exception("Paciente o nombre null"));
            return -1;
        }

        // creado_en va explícito con la hora de la PC (ver util.Auditoria para el porqué: el
        // servidor MySQL corre en UTC, 3 horas adelantado respecto a Paraguay).
        String sql = "INSERT INTO pacientes (nombre, apellido, ci, telefono, fecha_nacimiento, genero, " +
                     "nombre_tutor, ci_tutor, direccion, motivo_consulta, curso, psicologo_id, antecedentes_personales, " +
                     "antecedentes_familiares, anamnesis, creado_en, consentimiento_tutor, consentimiento_fecha) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, paciente.getNombre());
            pst.setString(2, paciente.getApellido());
            pst.setString(3, paciente.getCi());
            pst.setString(4, paciente.getTelefono());
            pst.setDate(5, paciente.getFechaNacimiento() != null ?
                        Date.valueOf(paciente.getFechaNacimiento()) : null);
            pst.setString(6, paciente.getGenero());
            pst.setString(7, paciente.getNombreTutor());
            pst.setString(8, paciente.getCiTutor());
            pst.setString(9, paciente.getDireccion());
            pst.setString(10, paciente.getMotivoConsulta());
            pst.setString(11, paciente.getCurso());
            if (paciente.getPsicologoId() > 0) {
                pst.setInt(12, paciente.getPsicologoId());
            } else {
                pst.setNull(12, Types.INTEGER);
            }
            pst.setString(13, paciente.getAntecedentesPersonales());
            pst.setString(14, paciente.getAntecedenteFamiliares());
            pst.setString(15, paciente.getAnamnesis());
            pst.setTimestamp(16, Timestamp.valueOf(java.time.LocalDateTime.now()));
            pst.setBoolean(17, paciente.isConsentimientoTutor());
            if (paciente.isConsentimientoTutor()) {
                pst.setTimestamp(18, Timestamp.valueOf(java.time.LocalDateTime.now()));
            } else {
                pst.setNull(18, Types.TIMESTAMP);
            }

            pst.executeUpdate();
            registrarExito("Crear paciente: " + paciente.getNombre());

            try (ResultSet keys = pst.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }

        } catch (SQLException e) {
            registrarError("crear paciente", e);
            return -1;
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

        String sql = "UPDATE pacientes SET nombre=?, apellido=?, ci=?, telefono=?, fecha_nacimiento=?, " +
                     "genero=?, nombre_tutor=?, ci_tutor=?, direccion=?, motivo_consulta=?, curso=?, psicologo_id=?, " +
                     "antecedentes_personales=?, antecedentes_familiares=?, anamnesis=?, consentimiento_tutor=?, " +
                     "consentimiento_fecha=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, paciente.getNombre());
            pst.setString(2, paciente.getApellido());
            pst.setString(3, paciente.getCi());
            pst.setString(4, paciente.getTelefono());
            pst.setDate(5, paciente.getFechaNacimiento() != null ?
                        Date.valueOf(paciente.getFechaNacimiento()) : null);
            pst.setString(6, paciente.getGenero());
            pst.setString(7, paciente.getNombreTutor());
            pst.setString(8, paciente.getCiTutor());
            pst.setString(9, paciente.getDireccion());
            pst.setString(10, paciente.getMotivoConsulta());
            pst.setString(11, paciente.getCurso());
            if (paciente.getPsicologoId() > 0) {
                pst.setInt(12, paciente.getPsicologoId());
            } else {
                pst.setNull(12, Types.INTEGER);
            }
            pst.setString(13, paciente.getAntecedentesPersonales());
            pst.setString(14, paciente.getAntecedenteFamiliares());
            pst.setString(15, paciente.getAnamnesis());
            pst.setBoolean(16, paciente.isConsentimientoTutor());
            if (paciente.getConsentimientoFecha() != null) {
                pst.setTimestamp(17, Timestamp.valueOf(paciente.getConsentimientoFecha()));
            } else {
                pst.setNull(17, Types.TIMESTAMP);
            }
            pst.setInt(18, paciente.getId());

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
     * Elimina un paciente. Falla si tiene turnos, sesiones, historia clínica o
     * documentos asociados (integridad referencial).
     * @return null si se eliminó correctamente, o un mensaje de error listo para mostrar
     */
    public String eliminar(int id) {
        if (id <= 0) {
            return "ID inválido.";
        }

        String sql = "DELETE FROM pacientes WHERE id = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            int filasEliminadas = pst.executeUpdate();

            if (filasEliminadas > 0) {
                registrarExito("Eliminar paciente ID: " + id);
                return null;
            }
            return "No se encontró el estudiante.";

        } catch (SQLIntegrityConstraintViolationException e) {
            registrarError("eliminar paciente (referencias)", e);
            return "No se puede eliminar: tiene citas, atenciones, historia clínica o documentos asociados. "
                + "Eliminá o reasigná esos datos primero.";
        } catch (SQLException e) {
            registrarError("eliminar paciente", e);
            return "Error al eliminar: " + e.getMessage();
        }
    }

    /**
     * Buscar paciente por nombre, apellido, teléfono o CI
     */
    public List<Paciente> buscar(String termino) {
        List<Paciente> pacientes = new ArrayList<>();

        if (termino == null || termino.trim().isEmpty()) {
            return obtenerTodos();
        }

        String sql = "SELECT * FROM pacientes WHERE nombre LIKE ? OR apellido LIKE ? OR telefono LIKE ? OR ci LIKE ? ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            String patron = "%" + termino + "%";
            pst.setString(1, patron);
            pst.setString(2, patron);
            pst.setString(3, patron);
            pst.setString(4, patron);
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
     * Obtener pacientes asignados a un psicólogo específico
     */
    public List<Paciente> obtenerPorPsicologo(int psicologoId) {
        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT * FROM pacientes WHERE psicologo_id = ? ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, psicologoId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                pacientes.add(mapearPaciente(rs));
            }
            registrarExito("Obtener pacientes por psicólogo " + psicologoId + ": " + pacientes.size());

        } catch (SQLException e) {
            registrarError("obtener por psicólogo: " + psicologoId, e);
        }

        return pacientes;
    }

    /**
     * Buscar por nombre/apellido/teléfono/CI dentro de los pacientes de un psicólogo específico
     */
    public List<Paciente> buscarPorPsicologo(String termino, int psicologoId) {
        if (termino == null || termino.trim().isEmpty()) {
            return obtenerPorPsicologo(psicologoId);
        }

        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT * FROM pacientes WHERE psicologo_id = ? AND (nombre LIKE ? OR apellido LIKE ? OR telefono LIKE ? OR ci LIKE ?) ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            String patron = "%" + termino + "%";
            pst.setInt(1, psicologoId);
            pst.setString(2, patron);
            pst.setString(3, patron);
            pst.setString(4, patron);
            pst.setString(5, patron);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                pacientes.add(mapearPaciente(rs));
            }
            registrarExito("Buscar pacientes por psicólogo " + psicologoId + ": " + pacientes.size() + " resultados");

        } catch (SQLException e) {
            registrarError("buscar por psicólogo: " + psicologoId, e);
        }

        return pacientes;
    }

    /**
     * Cantidad de pacientes nuevos desde una fecha (para el filtro de período del Dashboard).
     */
    public int contarDesde(java.time.LocalDate desde) {
        return contarDesde(desde, null);
    }

    /** @param psicologoId si no es null, solo los estudiantes asignados a ese profesional */
    public int contarDesde(java.time.LocalDate desde, Integer psicologoId) {
        String sql = "SELECT COUNT(*) as total FROM pacientes WHERE creado_en >= ?"
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
            registrarError("contar desde fecha", e);
        }

        return 0;
    }

    /**
     * Pacientes nuevos agrupados por mes (yyyy-MM), para el gráfico de tendencia del Dashboard.
     * Incluye los últimos `meses` meses aunque no tengan pacientes (quedan en 0).
     */
    public java.util.LinkedHashMap<String, Integer> obtenerNuevosPorMes(int meses) {
        return obtenerNuevosPorMes(meses, null);
    }

    /** @param psicologoId si no es null, solo los estudiantes asignados a ese profesional */
    public java.util.LinkedHashMap<String, Integer> obtenerNuevosPorMes(int meses, Integer psicologoId) {
        java.util.LinkedHashMap<String, Integer> resultado = new java.util.LinkedHashMap<>();
        java.time.YearMonth actual = java.time.YearMonth.now();
        for (int i = meses - 1; i >= 0; i--) {
            resultado.put(actual.minusMonths(i).toString(), 0);
        }

        java.time.LocalDate desde = actual.minusMonths(meses - 1L).atDay(1);
        String sql = "SELECT DATE_FORMAT(creado_en, '%Y-%m') AS mes, COUNT(*) AS total "
            + "FROM pacientes WHERE creado_en >= ? "
            + (psicologoId != null ? "AND psicologo_id = ? " : "")
            + "GROUP BY mes";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, Date.valueOf(desde));
            if (psicologoId != null) {
                pst.setInt(2, psicologoId);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("mes"), rs.getInt("total"));
                }
            }
            registrarExito("Obtener pacientes nuevos por mes: " + resultado.size() + " meses");

        } catch (SQLException e) {
            registrarError("obtener nuevos por mes", e);
        }

        return resultado;
    }

    /**
     * Verificar si un CI ya existe
     */
    public boolean existeCi(String ci) {
        if (ci == null || ci.isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) as total FROM pacientes WHERE ci = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, ci);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            registrarError("verificar CI", e);
        }

        return false;
    }

    /**
     * Cantidad de estudiantes por curso (para el panel "Estudiantes por curso" del Dashboard
     * y para las exportaciones). Solo cuenta filas con curso asignado.
     */
    public java.util.LinkedHashMap<String, Integer> contarPorCurso() {
        return contarPorCurso(null);
    }

    /**
     * @param psicologoId si no es null, solo los estudiantes asignados a ese profesional (así
     *                    el número coincide con la lista que ve al tocar el curso en el Panel)
     */
    public java.util.LinkedHashMap<String, Integer> contarPorCurso(Integer psicologoId) {
        java.util.LinkedHashMap<String, Integer> resultado = new java.util.LinkedHashMap<>();
        String sql = "SELECT curso, COUNT(*) AS total FROM pacientes WHERE curso IS NOT NULL AND curso <> '' "
            + (psicologoId != null ? "AND psicologo_id = ? " : "")
            + "GROUP BY curso ORDER BY curso";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            if (psicologoId != null) {
                pst.setInt(1, psicologoId);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("curso"), rs.getInt("total"));
                }
            }
            registrarExito("Contar pacientes por curso: " + resultado.size() + " cursos");

        } catch (SQLException e) {
            registrarError("contar por curso", e);
        }

        return resultado;
    }

    /**
     * Mapear ResultSet a objeto Paciente
     */
    private Paciente mapearPaciente(ResultSet rs) throws SQLException {
        Paciente p = new Paciente();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        p.setCi(rs.getString("ci"));
        p.setTelefono(rs.getString("telefono"));
        p.setFechaNacimiento(rs.getDate("fecha_nacimiento") != null ? 
                            rs.getDate("fecha_nacimiento").toLocalDate() : null);
        p.setGenero(rs.getString("genero"));
        p.setNombreTutor(rs.getString("nombre_tutor"));
        p.setCiTutor(rs.getString("ci_tutor"));
        p.setDireccion(rs.getString("direccion"));
        p.setMotivoConsulta(rs.getString("motivo_consulta"));
        p.setCurso(rs.getString("curso"));
        p.setAntecedentesPersonales(rs.getString("antecedentes_personales"));
        p.setAntecedenteFamiliares(rs.getString("antecedentes_familiares"));
        p.setAnamnesis(rs.getString("anamnesis"));
        p.setPsicologoId(rs.getInt("psicologo_id"));
        p.setConsentimientoTutor(rs.getBoolean("consentimiento_tutor"));
        Timestamp consentimientoFecha = rs.getTimestamp("consentimiento_fecha");
        p.setConsentimientoFecha(consentimientoFecha != null ? consentimientoFecha.toLocalDateTime() : null);
        return p;
    }
}
