package construtrack.modelo.inventario;

import construtrack.modelo.excepciones.DatoInvalidoException;

public enum EstadoArticulo {
    OPERATIVO("Operativo"),
    EN_MANTENIMIENTO("En mantenimiento"),
    DE_BAJA("De baja");

    private final String nombre;

    EstadoArticulo(String nombre) {
        this.nombre = nombre;
    }

    public static EstadoArticulo desdeTexto(String texto) {
        try {
            return valueOf(texto == null ? "" : texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("El estado debe ser operativo, en mantenimiento o de baja.");
        }
    }

    public String getNombre() {
        return nombre;
    }
}
