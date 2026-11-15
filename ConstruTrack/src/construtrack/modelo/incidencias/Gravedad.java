package construtrack.modelo.incidencias;

/** Importancia de una incidencia: ordena el tablero y define el color en las vistas. */
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
