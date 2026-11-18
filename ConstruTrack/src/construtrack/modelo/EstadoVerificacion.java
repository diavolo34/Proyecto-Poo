package construtrack.modelo;

public enum EstadoVerificacion {
    NO_APLICA("Sin control"),
    VERIFICADA("Verificada"),
    POR_CONFIRMAR("Por confirmar"),
    CONFIRMADA("Confirmada"),
    RECHAZADA("Rechazada");

    private final String nombre;

    EstadoVerificacion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
