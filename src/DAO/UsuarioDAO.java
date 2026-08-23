package dao;

import conexion.Conexion;
import modelos.Usuario;
import util.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO extends DAO {

    public UsuarioDAO() {
        super();
    }

    /**
     * Autenticar usuario con usuario y contraseña
     */
    public Usuario autenticar(String usuario, String contraseña) {
        if (usuario == null || usuario.isEmpty() || contraseña == null || contraseña.isEmpty()) {
            registrarError("autenticar", new Exception("Usuario o contraseña vacíos"));
            return null;
        }

        String sql = "SELECT * FROM usuarios WHERE usuario = ? AND activo = 1";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, usuario);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String salt = rs.getString("salt");
                String hashEsperado = rs.getString("password_hash");

                if (PasswordUtil.verificar(contraseña, salt, hashEsperado)) {
                    Usuario u = mapearUsuario(rs);
                    registrarExito("Autenticar usuario: " + usuario);
                    return u;
                }
            }

        } catch (SQLException e) {
            registrarError("autenticar", e);
        }

        return null;
    }

    /**
     * Obtener usuario por ID
     */
    public Usuario obtenerPorId(int id) {
        if (id <= 0) {
            registrarError("obtenerPorId", new Exception("ID inválido"));
            return null;
        }

        String sql = "SELECT * FROM usuarios WHERE id = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }

        } catch (SQLException e) {
            registrarError("obtener por ID: " + id, e);
        }

        return null;
    }

    /**
     * Obtener todos los usuarios
     */
    public List<Usuario> obtenerTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
            registrarExito("Obtener todos usuarios: " + usuarios.size());

        } catch (SQLException e) {
            registrarError("obtener todos", e);
        }

        return usuarios;
    }

    /**
     * Obtener usuarios por rol
     */
    public List<Usuario> obtenerPorRol(String rol) {
        List<Usuario> usuarios = new ArrayList<>();
        
        if (rol == null || rol.isEmpty()) {
            return usuarios;
        }

        String sql = "SELECT * FROM usuarios WHERE rol = ? AND activo = 1 ORDER BY nombre ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, rol);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
            registrarExito("Obtener usuarios por rol " + rol + ": " + usuarios.size());

        } catch (SQLException e) {
            registrarError("obtener por rol: " + rol, e);
        }

        return usuarios;
    }

    /**
     * Verificar si usuario ya existe
     */
    public boolean existeUsuario(String usuario) {
        if (usuario == null || usuario.isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) as total FROM usuarios WHERE usuario = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, usuario);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            registrarError("verificar usuario", e);
        }

        return false;
    }

    /**
     * Obtener nombre de usuario por ID
     */
    public String obtenerNombrePorId(int id) {
        Usuario u = obtenerPorId(id);
        return u != null ? u.getNombre() : "N/A";
    }

    /**
     * Crear un nuevo usuario, generando salt+hash a partir de la contraseña en texto plano.
     * @return el ID generado, o -1 si falló
     */
    public int crear(String usuario, String nombre, String passwordPlano, String rol) {
        if (usuario == null || usuario.trim().isEmpty() || nombre == null || nombre.trim().isEmpty()
                || passwordPlano == null || passwordPlano.isEmpty() || rol == null || rol.isEmpty()) {
            registrarError("crear", new Exception("Datos de usuario incompletos"));
            return -1;
        }

        String salt = PasswordUtil.generarSalt();
        String hash = PasswordUtil.hash(passwordPlano, salt);

        String sql = "INSERT INTO usuarios (usuario, password_hash, salt, nombre, rol, activo) VALUES (?, ?, ?, ?, ?, 1)";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, usuario.trim());
            pst.setString(2, hash);
            pst.setString(3, salt);
            pst.setString(4, nombre.trim());
            pst.setString(5, rol);

            pst.executeUpdate();
            registrarExito("Crear usuario: " + usuario);

            try (ResultSet keys = pst.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }

        } catch (SQLException e) {
            registrarError("crear usuario", e);
            return -1;
        }
    }

    /**
     * Cambia la contraseña de un usuario, regenerando salt+hash.
     */
    public boolean cambiarPassword(int usuarioId, String passwordPlano) {
        if (usuarioId <= 0 || passwordPlano == null || passwordPlano.isEmpty()) {
            registrarError("cambiarPassword", new Exception("Datos inválidos"));
            return false;
        }

        String salt = PasswordUtil.generarSalt();
        String hash = PasswordUtil.hash(passwordPlano, salt);

        String sql = "UPDATE usuarios SET password_hash=?, salt=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, hash);
            pst.setString(2, salt);
            pst.setInt(3, usuarioId);

            boolean ok = pst.executeUpdate() > 0;
            if (ok) {
                registrarExito("Cambiar contraseña usuario ID: " + usuarioId);
            }
            return ok;

        } catch (SQLException e) {
            registrarError("cambiar contraseña", e);
            return false;
        }
    }

    /**
     * Activa/desactiva una cuenta (baja lógica, no se borra el usuario).
     */
    public boolean actualizarActivo(int usuarioId, boolean activo) {
        String sql = "UPDATE usuarios SET activo=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setBoolean(1, activo);
            pst.setInt(2, usuarioId);

            boolean ok = pst.executeUpdate() > 0;
            if (ok) {
                registrarExito((activo ? "Activar" : "Desactivar") + " usuario ID: " + usuarioId);
            }
            return ok;

        } catch (SQLException e) {
            registrarError("actualizar activo", e);
            return false;
        }
    }

    /**
     * Cambia el nombre para mostrar de un usuario.
     */
    public boolean actualizarNombre(int usuarioId, String nombre) {
        String sql = "UPDATE usuarios SET nombre=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, nombre);
            pst.setInt(2, usuarioId);

            boolean ok = pst.executeUpdate() > 0;
            if (ok) {
                registrarExito("Actualizar nombre usuario ID: " + usuarioId);
            }
            return ok;

        } catch (SQLException e) {
            registrarError("actualizar nombre", e);
            return false;
        }
    }

    /**
     * Cambia el rol de un usuario.
     */
    public boolean actualizarRol(int usuarioId, String rol) {
        String sql = "UPDATE usuarios SET rol=? WHERE id=?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, rol);
            pst.setInt(2, usuarioId);

            boolean ok = pst.executeUpdate() > 0;
            if (ok) {
                registrarExito("Actualizar rol usuario ID: " + usuarioId + " -> " + rol);
            }
            return ok;

        } catch (SQLException e) {
            registrarError("actualizar rol", e);
            return false;
        }
    }

    /**
     * Elimina definitivamente un usuario. Falla si tiene pacientes, turnos, sesiones,
     * documentos o auditoría asociados (integridad referencial) — en ese caso hay que
     * reasignar o borrar esos datos primero, o simplemente desactivar la cuenta en su lugar.
     * @return null si se eliminó correctamente, o un mensaje de error listo para mostrar
     */
    public String eliminar(int usuarioId) {
        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, usuarioId);
            int filas = pst.executeUpdate();

            if (filas > 0) {
                registrarExito("Eliminar usuario ID: " + usuarioId);
                return null;
            }
            return "No se encontró el usuario.";

        } catch (SQLIntegrityConstraintViolationException e) {
            registrarError("eliminar usuario (referencias)", e);
            return "No se puede eliminar: tiene pacientes, turnos, sesiones u otros registros asociados. "
                + "Reasigná o eliminá esos datos primero, o desactivá la cuenta en vez de borrarla.";
        } catch (SQLException e) {
            registrarError("eliminar usuario", e);
            return "Error al eliminar: " + e.getMessage();
        }
    }

    /**
     * Mapear ResultSet a objeto Usuario
     */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
}
