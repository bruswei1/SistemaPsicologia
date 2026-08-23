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
     * Mapear ResultSet a objeto Usuario
     */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setRol(rs.getString("rol"));
        return u;
    }
}
