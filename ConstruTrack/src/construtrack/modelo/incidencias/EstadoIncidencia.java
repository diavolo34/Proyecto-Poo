package construtrack.modelo.incidencias;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de una incidencia. Cada estado conoce a qué estados puede pasar,
 * así la regla vive en un solo lugar y no se repite en las vistas.
 *
 * PENDIENTE → EN_REVISION → APROBADA | RECHAZADA (también se puede resolver directamente desde PENDIENTE).
 */
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

    /** Un estado final ya no admite cambios. */
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
