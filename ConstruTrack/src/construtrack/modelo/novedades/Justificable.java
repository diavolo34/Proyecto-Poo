package construtrack.modelo.novedades;

import java.time.LocalDate;

public interface Justificable {
    boolean justificaAusenciaEl(LocalDate fecha);

    String etiquetaJustificacion();
}
