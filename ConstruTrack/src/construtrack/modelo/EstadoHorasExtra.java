package construtrack.modelo;

public enum EstadoHorasExtra {
    PENDIENTE("Por aprobar"),
    APROBADAS("Aprobadas"),
    RECHAZADAS("Rechazadas"),

    NO_APLICA("Antes del control de horas extra");

    private final String nombre;

    EstadoHorasExtra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
