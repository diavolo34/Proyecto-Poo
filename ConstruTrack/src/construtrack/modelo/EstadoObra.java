package construtrack.modelo;

public enum EstadoObra {
    PLANIFICADA("Planificado"),
    EN_CURSO("En curso"),
    CONCLUIDA("Concluido");

    private final String nombre;

    EstadoObra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
