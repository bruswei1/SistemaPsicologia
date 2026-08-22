package util;

import javax.swing.JOptionPane;

public class ControlPermiso {

    /**
     * Verifica si el usuario actual tiene un rol específico
     */
    public static boolean tieneRol(String rolRequerido) {
        try {
            String rolActual = Sesion.getRol();
            return rolActual != null && rolActual.equals(rolRequerido);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si el usuario es administrador
     */
    public static boolean esAdmin() {
        return tieneRol("admin");
    }

    /**
     * Verifica si el usuario es psicólogo
     */
    public static boolean esPsicologo() {
        return tieneRol("psicologo");
    }

    /**
     * Verifica si el usuario es secretaria
     */
    public static boolean esSecretaria() {
        return tieneRol("secretaria");
    }

    /**
     * Verifica si el usuario tiene alguno de los roles especificados
     */
    public static boolean tieneAlgunRol(String... rolesPermitidos) {
        String rolActual = Sesion.getRol();
        if (rolActual == null) return false;
        
        for (String rol : rolesPermitidos) {
            if (rolActual.equals(rol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica permiso y muestra mensaje si no tiene acceso
     */
    public static boolean verificarAcceso(String rolRequerido, String modulo) {
        if (!tieneRol(rolRequerido)) {
            JOptionPane.showMessageDialog(null, 
                "No tienes permiso para acceder a " + modulo + "\n" +
                "Rol requerido: " + rolRequerido,
                "Acceso denegado", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Obtiene el nombre del rol actual
     */
    public static String obtenerRolActual() {
        try {
            return Sesion.getRol();
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    /**
     * Obtiene el nombre del usuario actual
     */
    public static String obtenerNombreUsuario() {
        try {
            return Sesion.getNombre();
        } catch (Exception e) {
            return "Usuario";
        }
    }
}
