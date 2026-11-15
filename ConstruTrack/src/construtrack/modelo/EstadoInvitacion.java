package construtrack.modelo;

/** Estado de una invitación. VENCIDA no se guarda: se calcula comparando la fecha de vencimiento. */
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
