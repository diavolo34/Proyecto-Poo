package construtrack.modelo.incidencias;

public enum Gravedad {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta");

    private final String nombre;

    Gravedad(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
