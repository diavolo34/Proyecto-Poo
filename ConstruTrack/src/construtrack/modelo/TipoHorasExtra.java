package construtrack.modelo;

/** De dónde salen las horas extra de una jornada. */
public enum TipoHorasExtra {
    /** Se quedó más tiempo que su jornada en un día que le tocaba trabajar. */
    DIARIA("Después de su jornada"),
    /** Trabajó un día que no está en su horario (fin de semana o día libre): todo el tiempo cuenta como extra. */
    DIA_LIBRE("Fin de semana o día libre");

    private final String nombre;

    TipoHorasExtra(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
