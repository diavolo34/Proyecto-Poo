package construtrack.modelo;

public enum EstadoObra {
    PLANIFICADA("Planificada"),
    EN_CURSO("En curso"),
    CONCLUIDA("Concluida");

    private final String nombre;

    EstadoObra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
