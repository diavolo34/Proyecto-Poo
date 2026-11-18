package construtrack.modelo;

import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.Novedad;
import java.time.LocalDateTime;

public interface RevisorNovedades {
    boolean puedeRevisar(Novedad novedad);

    void revisar(Novedad novedad, EstadoNovedad nuevo, String nota, LocalDateTime ahora);
}
