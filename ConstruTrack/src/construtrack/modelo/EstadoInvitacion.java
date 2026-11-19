package construtrack.modelo;

public enum EstadoInvitacion {
    VIGENTE("Vigente"),
    USADA("Usado"),
    CANCELADA("Cancelado"),
    VENCIDA("Vencido");

    private final String nombre;

    EstadoInvitacion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
