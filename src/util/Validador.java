package util;

import java.util.regex.Pattern;

public class Validador {

    // Patrones de validación
    private static final Pattern PATRON_EMAIL = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    private static final Pattern PATRON_TELEFONO = 
        Pattern.compile("^[0-9]{7,15}$");
    
    private static final Pattern PATRON_CEDULA = 
        Pattern.compile("^[0-9]{6,12}$");

    /**
     * Valida que un campo no esté vacío
     */
    public static boolean noVacio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    /**
     * Valida un email
     */
    public static boolean esEmailValido(String email) {
        if (!noVacio(email)) return false;
        return PATRON_EMAIL.matcher(email).matches();
    }

    /**
     * Valida un teléfono (7 a 15 dígitos)
     */
    public static boolean esTelefonoValido(String telefono) {
        if (!noVacio(telefono)) return true; // Es opcional
        return PATRON_TELEFONO.matcher(telefono).matches();
    }

    /**
     * Valida una cédula
     */
    public static boolean esCedulaValida(String cedula) {
        if (!noVacio(cedula)) return true; // Es opcional
        return PATRON_CEDULA.matcher(cedula).matches();
    }

    /**
     * Valida nombre (no números, no caracteres especiales)
     */
    public static boolean esNombreValido(String nombre) {
        if (!noVacio(nombre)) return false;
        return nombre.matches("^[a-záéíóúñA-ZÁÉÍÓÚÑ ]+$");
    }

    /**
     * Valida contraseña (mínimo 6 caracteres)
     */
    public static boolean esContraseñaValida(String contraseña) {
        return noVacio(contraseña) && contraseña.length() >= 6;
    }

    /**
     * Valida usuario (alfanuméricos y guiones, 3-20 caracteres)
     */
    public static boolean esUsuarioValido(String usuario) {
        if (!noVacio(usuario)) return false;
        return usuario.matches("^[a-zA-Z0-9_-]{3,20}$");
    }

    /**
     * Valida hora en formato HH:mm
     */
    public static boolean esHoraValida(String hora) {
        if (!noVacio(hora)) return false;
        return hora.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    }

    /**
     * Mensaje de validación personalizado
     */
    public static String obtenerMensajeError(String campo, String tipo) {
        switch (tipo) {
            case "vacio":
                return campo + " es obligatorio";
            case "email":
                return campo + " no es válido";
            case "telefono":
                return campo + " debe tener 7-15 dígitos";
            case "cedula":
                return campo + " debe tener 6-12 dígitos";
            case "nombre":
                return campo + " solo puede contener letras y espacios";
            case "contraseña":
                return campo + " debe tener mínimo 6 caracteres";
            case "usuario":
                return campo + " debe tener 3-20 caracteres (letras, números, - y _)";
            case "hora":
                return campo + " debe estar en formato HH:mm";
            default:
                return "Datos inválidos";
        }
    }
}
