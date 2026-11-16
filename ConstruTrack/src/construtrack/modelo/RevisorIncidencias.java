package construtrack.modelo;

import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import java.time.LocalDateTime;

public interface RevisorIncidencias {
    boolean puedeRevisar(Incidencia incidencia);

    void revisar(Incidencia incidencia, EstadoIncidencia nuevo, String comentario, LocalDateTime ahora);
}
