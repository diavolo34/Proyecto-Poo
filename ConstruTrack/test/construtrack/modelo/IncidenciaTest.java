package construtrack.modelo;

import static construtrack.Datos.AHORA;
import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.modelo.incidencias.Gravedad;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.modelo.incidencias.Retardo;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncidenciaTest {
    private final Datos d = new Datos();
    private final FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();

    private Incidencia permisoDeJose() {
        Incidencia i = fabrica.crear("PERMISO_PERSONAL", Datos.incidencia(1, d.jose, d.jose, HOY, HOY, Map.of()));
        d.jose.reportar(i);
        return i;
    }

    @Test
    @DisplayName("Ciclo de vida: pendiente → en revisión → aprobada, y luego ya no cambia")
    void cicloDeVida() {
        Incidencia i = permisoDeJose();
        assertEquals(EstadoIncidencia.PENDIENTE, i.getEstado());
        d.ana.revisar(i, EstadoIncidencia.EN_REVISION, "", AHORA);
        d.ana.revisar(i, EstadoIncidencia.APROBADA, "Ok", AHORA);
        assertTrue(i.estaAprobada());
        assertThrows(OperacionInvalidaException.class, () -> d.ana.revisar(i, EstadoIncidencia.RECHAZADA, "No", AHORA));
    }

    @Test
    @DisplayName("Rechazar exige un motivo")
    void rechazoSinMotivo() {
        Incidencia i = permisoDeJose();
        assertThrows(DatoInvalidoException.class, () -> d.ana.revisar(i, EstadoIncidencia.RECHAZADA, " ", AHORA));
    }

    @Test
    @DisplayName("Un supervisor solo revisa a su equipo; el gerente revisa a todos menos a otro gerente")
    void autoridadDeRevision() {
        Incidencia i = permisoDeJose();
        assertFalse(d.luis.puedeRevisar(i));
        assertThrows(PermisoDenegadoException.class, () -> d.luis.revisar(i, EstadoIncidencia.APROBADA, "", AHORA));
        assertTrue(d.gerente.puedeRevisar(i));
    }

    @Test
    @DisplayName("Un empleado no puede reportar incidencias de otra persona")
    void empleadoSoloReportaLasSuyas() {
        Incidencia ajena = fabrica.crear("AUSENCIA", Datos.incidencia(2, d.laura, d.jose, HOY, HOY, Map.of()));
        assertThrows(PermisoDenegadoException.class, () -> d.jose.reportar(ajena));
        assertTrue(d.ana.puedeReportarPara(d.jose));
        assertFalse(d.ana.puedeReportarPara(d.laura));
    }

    @Test
    @DisplayName("La fábrica crea cada tipo por su código y valida sus datos propios (polimorfismo)")
    void fabricaYPolimorfismo() {
        Incidencia retardo = fabrica.crear("RETARDO", Datos.incidencia(3, d.jose, d.jose, HOY, HOY, Map.of("minutos", "90")));
        assertTrue(retardo instanceof Retardo);
        assertEquals(Gravedad.MEDIA, retardo.getGravedad(), "Más de 60 minutos es gravedad media");
        assertThrows(DatoInvalidoException.class,
                () -> fabrica.crear("RETARDO", Datos.incidencia(4, d.jose, d.jose, HOY, HOY, Map.of("minutos", "0"))));
        assertThrows(DatoInvalidoException.class,
                () -> fabrica.crear("VACACIONES", Datos.incidencia(5, d.jose, d.jose, HOY, HOY, Map.of())));
    }

    @Test
    @DisplayName("Una incidencia aprobada o rechazada sale del tablero 3 horas después de revisarse")
    void archivadaTresHorasDespues() {
        Incidencia i = permisoDeJose();
        assertFalse(i.archivadaEn(AHORA.plusDays(30)), "Mientras está pendiente sigue en el tablero");
        d.ana.revisar(i, EstadoIncidencia.EN_REVISION, "", AHORA);
        assertFalse(i.archivadaEn(AHORA.plusDays(30)), "En revisión tampoco se archiva");
        d.ana.revisar(i, EstadoIncidencia.APROBADA, "Ok", AHORA);
        assertFalse(i.archivadaEn(AHORA.plusHours(Incidencia.HORAS_EN_TABLERO).minusMinutes(1)));
        assertTrue(i.archivadaEn(AHORA.plusHours(Incidencia.HORAS_EN_TABLERO)));
    }
}
