package construtrack.modelo.inventario;

import construtrack.modelo.excepciones.DatoInvalidoException;

public enum CondicionDevolucion {
    BUENA("En buen estado"),
    DANADA("Con daños"),
    EXTRAVIADA("Extraviado");

    private final String nombre;

    CondicionDevolucion(String nombre) {
        this.nombre = nombre;
    }

    public static CondicionDevolucion desdeTexto(String texto) {
        try {
            return valueOf(texto == null ? "" : texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("Indica cómo se devolvió: en buen estado, con daños o extraviado.");
        }
    }

    public String getNombre() {
        return nombre;
    }
}
