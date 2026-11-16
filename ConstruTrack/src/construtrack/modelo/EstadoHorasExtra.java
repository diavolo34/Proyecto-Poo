package construtrack.modelo;

/** Situación de las horas extra de una jornada: el supervisor o el jefe deben aprobarlas para que cuenten. */
public enum EstadoHorasExtra {
    PENDIENTE("Por aprobar"),
    APROBADAS("Aprobadas"),
    RECHAZADAS("Rechazadas"),
    /** Jornada guardada antes de existir el control de horas extra: no se cuenta ni se revisa. */
    NO_APLICA("Antes del control de horas extra");

    private final String nombre;

    EstadoHorasExtra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
