package construtrack.modelo.alertas;

/** Gravedad de una alerta. El orden del enum define la prioridad: ALTA aparece primero. */
public enum Nivel {

    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja");

    private final String nombre;

    Nivel(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
