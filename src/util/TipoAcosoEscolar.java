package util;

/**
 * Clasificación de acoso escolar según la Ley N° 4633/2012 ("Contra el Acoso Escolar"): la ley
 * distingue acoso directo (daño físico), indirecto (daño a bienes/pertenencias) y verbal
 * (expresión injuriosa u ofensiva). Los códigos cortos son los que se guardan en
 * `historia_psicologica.tipo_acoso`; las etiquetas son solo para mostrar en pantalla/reportes.
 */
public final class TipoAcosoEscolar {

    private TipoAcosoEscolar() {
    }

    public static final String DIRECTO = "directo";
    public static final String INDIRECTO = "indirecto";
    public static final String VERBAL = "verbal";

    public static String etiqueta(String codigo) {
        if (codigo == null) {
            return null;
        }
        switch (codigo) {
            case DIRECTO: return "Acoso directo (físico)";
            case INDIRECTO: return "Acoso indirecto (daño a bienes)";
            case VERBAL: return "Acoso verbal";
            default: return codigo;
        }
    }
}
