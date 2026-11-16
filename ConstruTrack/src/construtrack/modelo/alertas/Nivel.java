package construtrack.modelo.alertas;

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
