package construtrack.modelo.incidencias;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoIncidencia {
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String nombre;

    EstadoIncidencia(String nombre) {
        this.nombre = nombre;
    }

    public Set<EstadoIncidencia> siguientes() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(EN_REVISION, APROBADA, RECHAZADA);
            case EN_REVISION -> EnumSet.of(APROBADA, RECHAZADA);
            case APROBADA, RECHAZADA -> EnumSet.noneOf(EstadoIncidencia.class);
        };
    }

    public boolean puedePasarA(EstadoIncidencia otro) {
        return siguientes().contains(otro);
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
