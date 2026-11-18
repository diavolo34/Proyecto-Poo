package construtrack.modelo.inventario;

import construtrack.modelo.excepciones.DatoInvalidoException;

public enum CategoriaArticulo {
    HERRAMIENTA("Herramienta", "HE"),
    EQUIPO("Equipo", "EQ"),
    MATERIAL("Material", "MA");

    private final String nombre;
    private final String prefijo;

    CategoriaArticulo(String nombre, String prefijo) {
        this.nombre = nombre;
        this.prefijo = prefijo;
    }

    public static CategoriaArticulo desdeTexto(String texto) {
        try {
            return valueOf(texto == null ? "" : texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("La categoría debe ser herramienta, equipo o material.");
        }
    }

    public String getNombre() {
        return nombre;
    }

    public String getPrefijo() {
        return prefijo;
    }
}
