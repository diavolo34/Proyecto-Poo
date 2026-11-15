package construtrack.modelo;

import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import java.time.LocalDateTime;

/**
 * Rol de quien puede revisar incidencias. Solo lo implementan Supervisor y Jefe; el Empleado no
 * queda obligado a tener métodos de revisión que no le corresponden (segregación de interfaces).
 */
public interface RevisorIncidencias {

    /** Indica si este revisor tiene autoridad sobre la incidencia. */
    boolean puedeRevisar(Incidencia incidencia);

    /** Cambia el estado de la incidencia si tiene autoridad; si no, lanza PermisoDenegadoException. */
    void revisar(Incidencia incidencia, EstadoIncidencia nuevo, String comentario, LocalDateTime ahora);
}
