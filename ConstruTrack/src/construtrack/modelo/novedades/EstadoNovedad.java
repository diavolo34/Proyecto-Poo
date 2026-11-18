package construtrack.modelo.novedades;

public enum EstadoNovedad {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String nombre;

    EstadoNovedad(String nombre) {
        this.nombre = nombre;
    }

    public static EstadoNovedad desdeTexto(String texto) {
        return "EN_REVISION".equals(texto) ? PENDIENTE : valueOf(texto);
    }

    public boolean esFinal() {
        return this != PENDIENTE;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
