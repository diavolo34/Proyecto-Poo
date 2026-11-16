package construtrack.modelo;

/** Situación de las horas extra de una jornada: el supervisor o el jefe deben aprobarlas para que cuenten. */
public enum EstadoHorasExtra {
    PENDIENTE("Por aprobar"),
    APROBADAS("Aprobadas"),
    RECHAZADAS("Rechazadas");

    private final String nombre;

    EstadoHorasExtra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
