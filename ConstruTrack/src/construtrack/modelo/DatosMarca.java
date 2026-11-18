package construtrack.modelo;

import java.time.Instant;

public record DatosMarca(String codigo, Double latitud, Double longitud, Double precision, Instant momento) {
    public static DatosMarca sinDatos(Instant momento) {
        return new DatosMarca(null, null, null, null, momento);
    }

    public boolean tieneCodigo() {
        return codigo != null && !codigo.isBlank();
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }
}
