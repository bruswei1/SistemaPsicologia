package conexion;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Conexion {

    private static final String ARCHIVO_CONFIG = "conexion.properties";

    /** @throws SinConexionException si no se puede conectar (nunca devuelve null). */
    public Connection conectar() {

        try {

            Properties config = cargarConfiguracion();

            String host = config.getProperty("db.host", "localhost");
            String puerto = config.getProperty("db.puerto", "3306");
            String nombreBase = config.getProperty("db.nombre", "psicologia");
            String parametros = config.getProperty("db.parametros", "");
            String usuario = config.getProperty("db.usuario", "root");
            String password = config.getProperty("db.password", "");

            String url = "jdbc:mysql://" + host + ":" + puerto + "/" + nombreBase
                + (parametros.isEmpty() ? "" : "?" + parametros);

            Connection con =
                DriverManager.getConnection(
                    url,
                    usuario,
                    password
                );

            return con;

        } catch (Exception e) {

            System.out.println(
                "Error: " + e.getMessage()
            );

            // Antes devolvía null: la mayoría de las pantallas no lo contemplaba y, si MySQL se
            // caía, fallaban con un "Error: null" sin explicación. Ahora el error dice qué pasó.
            throw new SinConexionException(e);
        }
    }

    /**
     * No se pudo abrir la conexión (MySQL apagado, red, credenciales). Extiende
     * IllegalStateException para que el código que ya atrapaba ese caso lo siga haciendo.
     */
    public static class SinConexionException extends IllegalStateException {
        public SinConexionException(Throwable causa) {
            super("No se pudo conectar a la base de datos. Revisá que el servidor MySQL esté encendido"
                + " y accesible desde esta PC." + (causa.getMessage() != null ? " (" + causa.getMessage() + ")" : ""), causa);
        }
    }

    /**
     * Busca conexion.properties junto al ejecutable. Si no existe (caso actual,
     * instalacion de desarrollo), se usan los valores por defecto de siempre
     * (localhost/psicologia, root, sin password) sin romper nada.
     */
    private Properties cargarConfiguracion() {
        Properties config = new Properties();
        try (InputStream in = new FileInputStream(ARCHIVO_CONFIG)) {
            config.load(in);
        } catch (IOException e) {
            // Sin archivo externo: se mantienen los valores por defecto.
        }
        return config;
    }
}