package util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Único lugar que decide dónde se guardan en disco los archivos que genera el sistema
 * (adjuntos, reportes, respaldos). Antes cada pantalla hacía {@code new File("reportes")} con
 * una ruta relativa al directorio de trabajo: si la app se lanzaba desde otro lugar (acceso
 * directo, doble clic al .jar) los archivos quedaban desparramados, y los adjuntos guardados
 * con ruta relativa en la base dejaban de encontrarse. Instalada en "Archivos de programa"
 * (lo que hace el instalador NSIS) ni siquiera se podía escribir ahí.
 *
 * Sin UI: los diálogos de error quedan del lado de {@code Vista}.
 */
public final class Carpetas {

    private static final String ADJUNTOS = "adjuntos";
    private static final String REPORTES = "reportes";
    private static final String RESPALDOS = "respaldos_bd";

    private static final int LARGO_MAXIMO_NOMBRE = 80;

    private static File base;

    private Carpetas() {
    }

    /**
     * Carpeta raíz de datos. Es el directorio de trabajo (donde siempre se guardó todo, así los
     * datos existentes siguen encontrándose) salvo que no se pueda escribir en él; en ese caso,
     * Documentos\Sistema Psicologia del usuario de Windows.
     */
    public static synchronized File base() {
        if (base == null) {
            File dirTrabajo = new File(System.getProperty("user.dir")).getAbsoluteFile();
            if (esEscribible(dirTrabajo)) {
                base = dirTrabajo;
            } else {
                base = asegurar(new File(System.getProperty("user.home"), "Documents" + File.separator + "Sistema Psicologia"));
            }
        }
        return base;
    }

    public static File adjuntos() {
        return asegurar(new File(base(), ADJUNTOS));
    }

    public static File adjuntosDe(int pacienteId) {
        return asegurar(new File(adjuntos(), String.valueOf(pacienteId)));
    }

    public static File reportes() {
        return asegurar(new File(base(), REPORTES));
    }

    /** reportes/Estudiantes/Apellido, Nombre (id)/ — fichas y notas de un mismo estudiante juntas. */
    public static File reportesDe(int pacienteId, String nombre, String apellido) {
        String carpeta = nombreSeguro(textoONada(apellido) + ", " + textoONada(nombre)) + " (" + pacienteId + ")";
        return asegurar(new File(new File(reportes(), "Estudiantes"), carpeta));
    }

    /** reportes/Listados/ — exportaciones de varios estudiantes a la vez (CSV). */
    public static File listados() {
        return asegurar(new File(reportes(), "Listados"));
    }

    public static File respaldos() {
        return asegurar(new File(base(), RESPALDOS));
    }

    /**
     * Resuelve una ruta guardada en la base (documentos_paciente.ruta_archivo). Las relativas
     * ("adjuntos\18\...") se interpretan contra {@link #base()}, no contra el directorio de
     * trabajo del momento.
     */
    public static File resolver(String ruta) {
        File archivo = new File(ruta);
        return archivo.isAbsolute() ? archivo : new File(base(), ruta);
    }

    /** Ruta a guardar en la base: relativa a {@link #base()} si cae adentro, así la carpeta se puede mover entera. */
    public static String rutaRelativa(File archivo) {
        Path raiz = base().toPath().toAbsolutePath().normalize();
        Path ruta = archivo.toPath().toAbsolutePath().normalize();
        return ruta.startsWith(raiz) ? raiz.relativize(ruta).toString() : ruta.toString();
    }

    /**
     * Limpia un texto para usarlo como nombre de archivo/carpeta en Windows: quita los
     * caracteres prohibidos (\ / : * ? " &lt; &gt; |) y de control, colapsa espacios y recorta
     * puntos/espacios finales (Windows los descarta y rompe la ruta). Conserva tildes y ñ.
     */
    public static String nombreSeguro(String texto) {
        String limpio = textoONada(texto)
            .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            limpio = limpio.substring(0, LARGO_MAXIMO_NOMBRE).trim();
        }
        limpio = limpio.replaceAll("[. ]+$", "");
        return limpio.isEmpty() || limpio.equals(",") ? "sin_nombre" : limpio;
    }

    /** Si {@code nombre} ya existe en {@code carpeta}, devuelve "nombre (2).ext", "nombre (3).ext", ... */
    public static File archivoUnico(File carpeta, String nombre) {
        File candidato = new File(carpeta, nombre);
        if (!candidato.exists()) {
            return candidato;
        }
        int punto = nombre.lastIndexOf('.');
        String raiz = punto > 0 ? nombre.substring(0, punto) : nombre;
        String extension = punto > 0 ? nombre.substring(punto) : "";
        for (int i = 2; ; i++) {
            candidato = new File(carpeta, raiz + " (" + i + ")" + extension);
            if (!candidato.exists()) {
                return candidato;
            }
        }
    }

    /** Abre un archivo o carpeta con el programa predeterminado del sistema. */
    public static void abrir(File archivo) throws IOException {
        if (!archivo.exists()) {
            throw new IOException("No existe: " + archivo.getAbsolutePath());
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(archivo);
                return;
            } catch (IOException e) {
                // Desktop.open falla con algunas extensiones sin programa asociado; en Windows
                // el Explorador igual ofrece "¿Cómo querés abrir este archivo?".
                if (!esWindows()) {
                    throw e;
                }
            }
        }
        if (esWindows()) {
            new ProcessBuilder("explorer.exe", archivo.getAbsolutePath()).start();
            return;
        }
        throw new IOException("Esta plataforma no permite abrir archivos desde la aplicación");
    }

    /** Abre la carpeta que contiene el archivo, dejándolo seleccionado (en Windows). */
    public static void mostrarEnCarpeta(File archivo) throws IOException {
        if (esWindows() && archivo.exists()) {
            new ProcessBuilder("explorer.exe", "/select,", archivo.getAbsolutePath()).start();
            return;
        }
        abrir(archivo.getAbsoluteFile().getParentFile());
    }

    private static File asegurar(File carpeta) {
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return carpeta;
    }

    private static boolean esEscribible(File carpeta) {
        try {
            File prueba = File.createTempFile("escritura", ".tmp", carpeta);
            prueba.delete();
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String textoONada(String texto) {
        return texto != null ? texto : "";
    }
}
