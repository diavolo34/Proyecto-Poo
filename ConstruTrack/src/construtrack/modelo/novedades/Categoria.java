package construtrack.modelo.novedades;

public enum Categoria {
    LICENCIA("Permisos y reposos", "permiso o reposo"),
    REPORTE_OBRA("Reportes de proyecto", "reporte de proyecto");

    private final String nombre;
    private final String singular;

    Categoria(String nombre, String singular) {
        this.nombre = nombre;
        this.singular = singular;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSingular() {
        return singular;
    }
}
