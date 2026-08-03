package conexion;

import java.sql.Connection;
import java.sql.DriverManager;

public class Conexion {

    public Connection conectar() {

        try {

            String url =
                "jdbc:mysql://localhost:3306/psicologia";

            String usuario = "root";
            String password = "";

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

            return null;
        }
    }
}