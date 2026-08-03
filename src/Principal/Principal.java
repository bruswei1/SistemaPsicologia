package principal;

import conexion.Conexion;
import java.sql.Connection;

public class Principal {

    public static void main(String[] args) {

        Conexion c = new Conexion();

        Connection cn = c.conectar();

        if (cn != null) {

            System.out.println(
                    "Conexión exitosa");

        } else {

            System.out.println(
                    "Error de conexión");
        }
    }
}