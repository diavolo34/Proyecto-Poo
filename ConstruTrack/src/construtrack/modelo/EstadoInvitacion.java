package construtrack.modelo;

public enum EstadoInvitacion {
    VIGENTE("Vigente"),
    USADA("Usada"),
    CANCELADA("Cancelada"),
    VENCIDA("Vencida");

    private final String nombre;

    EstadoInvitacion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
