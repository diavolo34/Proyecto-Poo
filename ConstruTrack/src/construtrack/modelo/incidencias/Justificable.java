package construtrack.modelo.incidencias;

import java.time.LocalDate;

public interface Justificable {
    boolean justificaAusenciaEl(LocalDate fecha);
}
