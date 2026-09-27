package util;

import conexion.Conexion;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Respaldo automático de la base de datos: genera un archivo .sql con los datos de todas las
 * tablas (no la estructura — esa ya vive versionada en db-init/*.sql) en el orden que respeta las
 * llaves foráneas al reinsertar. Usa la misma conexión JDBC que el resto de la aplicación
 * (Conexion.conectar()) en vez de invocar mysqldump o depender de Docker, así que sirve tanto
 * para la base local de hoy como para una futura base en la nube.
 */
public class RespaldoBaseDatos {

    private static final int RESPALDOS_A_CONSERVAR = 14;

    /** Orden que respeta las llaves foráneas entre tablas al reinsertar. */
    private static final String[] TABLAS = {
        "usuarios", "pacientes", "turnos", "sesiones",
        "historia_psicologica", "documentos_paciente", "auditoria"
    };

    private RespaldoBaseDatos() {
    }

    /**
     * Genera el archivo de respaldo y borra los más viejos que excedan
     * {@link #RESPALDOS_A_CONSERVAR}. Pensado para correr en un hilo de fondo
     * ({@link Vista.Tema#enSegundoPlano}) — no toca componentes Swing.
     *
     * @return la ruta del archivo generado, o null si falló.
     */
    public static String generarRespaldo() {
        File carpeta = Carpetas.respaldos();

        String nombreArchivo = "respaldo_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        File archivo = new File(carpeta, nombreArchivo);

        try (Connection cn = new Conexion().conectar()) {
            if (cn == null) {
                return null;
            }

            // FileWriter usa la codificación por defecto del sistema (en Windows, no siempre
            // UTF-8) — con eso, cualquier tilde o "ñ" de un nombre quedaba corrupta en el archivo
            // aunque estuviera bien en la base. Se fuerza UTF-8 explícitamente.
            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8), false)) {
                out.println("-- Respaldo automático de SistemaPsicologia");
                out.println("-- Generado: " + LocalDateTime.now());
                out.println("-- Este archivo solo tiene los DATOS; la estructura de las tablas se");
                out.println("-- recrea aplicando los scripts de db-init/ en una base nueva.");
                out.println("SET FOREIGN_KEY_CHECKS=0;");
                out.println();

                for (String tabla : TABLAS) {
                    volcarTabla(cn, tabla, out);
                }

                out.println("SET FOREIGN_KEY_CHECKS=1;");
            }

            limpiarRespaldosViejos(carpeta);
            return archivo.getPath();

        } catch (Exception e) {
            System.out.println("Error generando respaldo de la base de datos: " + e.getMessage());
            return null;
        }
    }

    private static void volcarTabla(Connection cn, String tabla, PrintWriter out) throws Exception {
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnas = meta.getColumnCount();
            String[] nombresColumnas = new String[columnas];
            for (int i = 0; i < columnas; i++) {
                nombresColumnas[i] = meta.getColumnLabel(i + 1);
            }

            out.println("-- Tabla: " + tabla);
            out.println("DELETE FROM " + tabla + ";");

            int filas = 0;
            while (rs.next()) {
                StringBuilder sql = new StringBuilder("INSERT INTO ").append(tabla).append(" (");
                for (int i = 0; i < columnas; i++) {
                    sql.append(nombresColumnas[i]);
                    if (i < columnas - 1) {
                        sql.append(", ");
                    }
                }
                sql.append(") VALUES (");
                for (int i = 0; i < columnas; i++) {
                    sql.append(formatearValor(rs.getObject(i + 1)));
                    if (i < columnas - 1) {
                        sql.append(", ");
                    }
                }
                sql.append(");");
                out.println(sql);
                filas++;
            }
            out.println("-- " + filas + " fila(s)");
            out.println();
        }
    }

    private static String formatearValor(Object valor) {
        if (valor == null) {
            return "NULL";
        }
        if (valor instanceof Number) {
            return valor.toString();
        }
        if (valor instanceof Boolean) {
            return ((Boolean) valor) ? "1" : "0";
        }
        if (valor instanceof java.sql.Timestamp || valor instanceof java.sql.Date || valor instanceof java.sql.Time) {
            return "'" + valor.toString() + "'";
        }
        String texto = valor.toString().replace("\\", "\\\\").replace("'", "\\'");
        return "'" + texto + "'";
    }

    /** Conserva solo los últimos {@link #RESPALDOS_A_CONSERVAR}, para no llenar el disco con el tiempo. */
    private static void limpiarRespaldosViejos(File carpeta) {
        File[] archivos = carpeta.listFiles((dir, nombre) -> nombre.startsWith("respaldo_") && nombre.endsWith(".sql"));
        if (archivos == null || archivos.length <= RESPALDOS_A_CONSERVAR) {
            return;
        }
        java.util.Arrays.sort(archivos, java.util.Comparator.comparingLong(File::lastModified));
        int aBorrar = archivos.length - RESPALDOS_A_CONSERVAR;
        for (int i = 0; i < aBorrar; i++) {
            archivos[i].delete();
        }
    }

    /** true si el respaldo más reciente tiene más de {@code horas} de antigüedad (o no hay ninguno). */
    public static boolean requiereRespaldo(int horas) {
        File carpeta = Carpetas.respaldos();
        File[] archivos = carpeta.listFiles((dir, nombre) -> nombre.startsWith("respaldo_") && nombre.endsWith(".sql"));
        if (archivos == null || archivos.length == 0) {
            return true;
        }
        long masReciente = 0;
        for (File f : archivos) {
            masReciente = Math.max(masReciente, f.lastModified());
        }
        long limiteMillis = horas * 60L * 60L * 1000L;
        return (System.currentTimeMillis() - masReciente) > limiteMillis;
    }
}
