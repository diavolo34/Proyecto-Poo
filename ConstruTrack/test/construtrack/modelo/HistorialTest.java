package construtrack.modelo;

import static construtrack.Datos.AHORA;
import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.ReposoMedico;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HistorialTest {

    private final Datos d = new Datos();
    private final LocalDate lunes = LocalDate.of(2026, 11, 16);
    private final LocalDate martes = LocalDate.of(2026, 11, 17);
    private final Periodo semana = Periodo.semanaDe(HOY);

    @Test
    @DisplayName("Un día laborable sin jornada y sin justificativo cuenta como ausencia")
    void detectaAusencias() {
        d.jose.agregarAsistencia(new RegistroAsistencia(lunes, LocalTime.of(6, 55), false));
        List<LocalDate> faltas = d.jose.getHistorial().ausenciasSinJustificar(d.jose.getHorario(), semana, HOY);
        assertEquals(List.of(martes), faltas, "Solo falta el martes; hoy (miércoles) todavía no cuenta");
    }

    @Test
    @DisplayName("Un reposo médico aprobado justifica la falta; uno pendiente no")
    void reposoAprobadoJustifica() {
        ReposoMedico reposo = new ReposoMedico(Datos.incidencia(1, d.jose, d.jose, martes, martes,
                Map.of("centroMedico", "Clínica Santa Sofía")));
        d.jose.reportar(reposo);
        assertTrue(d.jose.getHistorial().ausenciasSinJustificar(d.jose.getHorario(), semana, HOY).contains(martes));

        d.ana.revisar(reposo, EstadoIncidencia.APROBADA, "Certificado válido", AHORA);
        assertTrue(d.jose.getHistorial().estaJustificado(martes));
        assertEquals(List.of(lunes), d.jose.getHistorial().ausenciasSinJustificar(d.jose.getHorario(), semana, HOY));
        assertEquals(List.of(martes), d.jose.getHistorial().ausenciasJustificadas(d.jose.getHorario(), semana, HOY));
    }

    @Test
    @DisplayName("La puntualidad es el porcentaje de jornadas sin retardo")
    void porcentajePuntualidad() {
        d.jose.agregarAsistencia(new RegistroAsistencia(lunes, LocalTime.of(6, 55), false));
        d.jose.agregarAsistencia(new RegistroAsistencia(martes, LocalTime.of(7, 30), true));
        assertEquals(1, d.jose.getHistorial().contarRetardos(semana));
        assertEquals(50.0, d.jose.getHistorial().porcentajePuntualidad(semana), 0.001);
    }

    @Test
    @DisplayName("Marcar entrada a las 07:25 queda como llegada tarde y deja a la persona presente")
    void marcarEntradaTarde() {
        RegistroAsistencia r = d.jose.marcarEntrada(HOY.atTime(7, 25));
        assertTrue(r.isTarde());
        assertTrue(d.jose.estaPresente());
        assertEquals(25, r.minutosRetraso(d.jose.getHorario()));
    }
}
