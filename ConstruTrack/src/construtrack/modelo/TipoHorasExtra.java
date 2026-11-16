package construtrack.modelo;

public enum TipoHorasExtra {
    DIARIA("Después de su jornada"),

    DIA_LIBRE("Fin de semana o día libre");

    private final String nombre;

    TipoHorasExtra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
