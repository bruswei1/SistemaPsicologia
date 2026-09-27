package dao;

import conexion.Conexion;
import java.sql.*;

/**
 * Clase base para todos los DAOs
 * Proporciona métodos comunes de seguridad y logging
 */
public abstract class DAO {
    
    protected Conexion conexion;

    public DAO() {
        this.conexion = new Conexion();
    }

    /**
     * Cierra recursos de conexión de forma segura
     */
    protected void cerrarRecursos(ResultSet rs, PreparedStatement pst, Connection con) {
        try {
            if (rs != null) rs.close();
            if (pst != null) pst.close();
            if (con != null) con.close();
        } catch (SQLException e) {
            System.out.println("Error cerrando recursos: " + e.getMessage());
        }
    }

    /**
     * Registra un error en la consola
     */
    protected void registrarError(String operacion, Exception e) {
        System.out.println("ERROR en " + operacion + ": " + e.getMessage());
        e.printStackTrace();
    }

    /**
     * Registra una operación exitosa
     */
    protected void registrarExito(String operacion) {
        System.out.println("✓ " + operacion + " exitosa");
    }

    /**
     * Obtiene conexión de forma segura
     */
    protected Connection obtenerConexion() throws SQLException {
        try {
            return conexion.conectar();
        } catch (conexion.Conexion.SinConexionException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
