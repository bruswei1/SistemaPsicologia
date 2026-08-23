package util;

public final class Sesion {

    private static Integer usuarioId;
    private static String usuario;
    private static String nombre;
    private static String rol;

    private Sesion() {
    }

    public static void iniciar(int usuarioId, String usuario, String nombre, String rol) {
        Sesion.usuarioId = usuarioId;
        Sesion.usuario = usuario;
        Sesion.nombre = nombre;
        Sesion.rol = rol;
    }

    public static void actualizarNombre(String nuevoNombre) {
        Sesion.nombre = nuevoNombre;
    }

    public static void cerrar() {
        usuarioId = null;
        usuario = null;
        nombre = null;
        rol = null;
    }

    public static Integer getUsuarioId() {
        return usuarioId;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static String getNombre() {
        return nombre;
    }

    public static String getRol() {
        return rol;
    }

    public static boolean esAdmin() {
        return "admin".equals(rol);
    }

    public static boolean esPsicologo() {
        return "psicologo".equals(rol);
    }

    public static boolean esSecretaria() {
        return "secretaria".equals(rol);
    }
}
