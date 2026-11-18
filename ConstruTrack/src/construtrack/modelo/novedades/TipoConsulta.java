package construtrack.modelo.novedades;

import construtrack.modelo.excepciones.DatoInvalidoException;

public enum TipoConsulta {
    INTERNA("Interna"),
    EMERGENCIA("Por emergencia");

    private final String nombre;

    TipoConsulta(String nombre) {
        this.nombre = nombre;
    }

    public static TipoConsulta desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("El tipo de consulta debe ser Interna o Por emergencia.");
        }
    }

    public String getNombre() {
        return nombre;
    }
}
