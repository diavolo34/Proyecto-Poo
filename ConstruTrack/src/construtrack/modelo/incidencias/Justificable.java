package construtrack.modelo.incidencias;

import java.time.LocalDate;

/**
 * Lo implementan solo las incidencias que pueden justificar una inasistencia (reposo médico, permiso,
 * accidente). Una Ausencia o un Retardo no la implementan: así ninguna clase queda obligada a ofrecer
 * un método que no tiene sentido para ella (principio de segregación de interfaces).
 */
public interface Justificable {

    /** Indica si la incidencia, ya aprobada, justifica no haber asistido en esa fecha. */
    boolean justificaAusenciaEl(LocalDate fecha);
}
