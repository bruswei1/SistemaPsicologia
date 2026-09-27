package util;

/**
 * Catálogo de motivos frecuentes de atención (disciplinarios/convivencia) compartido entre el
 * combo de "Motivo de la atención" en Vista.DetallePaciente y el conteo de casos por motivo del
 * Panel — así ambos lugares usan exactamente el mismo texto y no se desincronizan.
 */
public final class MotivosAtencion {

    private MotivosAtencion() {
    }

    public static final String[] PRESETS = {
        "Incumplimiento de las normas de convivencia",
        "Agresión física a un compañero/a",
        "Agresión verbal o insultos",
        "Bullying o acoso escolar",
        "Conflicto entre compañeros",
        "Falta de respeto a un docente o autoridad",
        "Conducta disruptiva en el aula",
        "Uso inadecuado de dispositivos electrónicos",
        "Inasistencias o impuntualidad reiteradas",
        "Incumplimiento de tareas o materiales escolares",
        "Daño a bienes o materiales del colegio",
        "Uso de vocabulario inapropiado",
        "Consumo o posesión de sustancias prohibidas",
        "Situación familiar o emocional que afecta al estudiante"
    };

    /** Bolsa para cualquier línea de motivo que no coincida con ninguno de los presets de arriba. */
    public static final String OTRO = "Otro motivo";
}
