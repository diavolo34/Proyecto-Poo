package construtrack.modelo.talento;

import construtrack.modelo.excepciones.DatoInvalidoException;

public enum TipoEgreso {
    DESPIDO("Despido"),
    RENUNCIA("Renuncia");

    private final String nombre;

    TipoEgreso(String nombre) {
        this.nombre = nombre;
    }

    public static TipoEgreso desdeTexto(String texto) {
        try {
            return valueOf(texto == null ? "" : texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("Elige si es un despido o una renuncia.");
        }
    }

    public String getNombre() {
        return nombre;
    }
}
