package construtrack.modelo;

import static construtrack.Datos.AHORA;
import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.excepciones.OperacionInvalidaException;
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
    @DisplayName("No se cuentan faltas de los días anteriores a su ingreso a la empresa")
    void sinFaltasAntesDelIngreso() {
        d.jose.setFechaIngreso(HOY);
        Periodo mes = Periodo.mesDe(HOY);
        assertEquals(List.of(), d.jose.getHistorial().ausenciasSinJustificar(d.jose.getHorario(), mes, HOY));
        assertEquals(List.of(), d.jose.getHistorial().diasLaborables(d.jose.getHorario(), mes, HOY));

        d.jose.setFechaIngreso(martes);
        assertEquals(List.of(martes), d.jose.getHistorial().ausenciasSinJustificar(d.jose.getHorario(), mes, HOY),
                "Desde el martes sí se espera que trabaje");
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

    @Test
    @DisplayName("La salida cierra la jornada abierta aunque haya otro registro del mismo día guardado después")
    void salidaConRegistrosDesordenados() {
        d.jefe.agregarAsistencia(new RegistroAsistencia(HOY, LocalTime.of(8, 0), true));
        RegistroAsistencia anterior = new RegistroAsistencia(HOY, LocalTime.of(6, 0), false);
        anterior.registrarSalida(LocalTime.of(7, 0));
        d.jefe.agregarAsistencia(anterior);

        assertTrue(d.jefe.estaPresente());
        RegistroAsistencia cerrada = d.jefe.marcarSalida(AHORA);
        assertEquals(LocalTime.of(8, 0), cerrada.getHoraEntrada());
        assertEquals(LocalTime.of(10, 0), cerrada.getHoraSalida());
        assertFalse(d.jefe.estaPresente());
        d.jefe.marcarEntrada(AHORA.plusMinutes(30));
        assertTrue(d.jefe.estaPresente());
        assertEquals(LocalTime.of(10, 30), d.jefe.jornadaDeHoy(HOY).getHoraEntrada());
    }

    @Test
    @DisplayName("Si olvidó marcar la salida ayer, se cierra a la hora de salida de su horario")
    void salidaOlvidadaDeAyer() {
        d.jefe.agregarAsistencia(new RegistroAsistencia(martes, LocalTime.of(7, 5), true));
        assertSame(d.jefe.getAsistenciaDel(martes), d.jefe.jornadaDeHoy(HOY), "Se muestra la jornada que quedó abierta");
        assertThrows(OperacionInvalidaException.class, () -> d.jefe.marcarEntrada(AHORA));

        RegistroAsistencia cerrada = d.jefe.marcarSalida(AHORA);
        assertEquals(martes, cerrada.getFecha());
        assertEquals(LocalTime.of(16, 0), cerrada.getHoraSalida());
        d.jefe.marcarEntrada(AHORA);
        assertEquals(HOY, d.jefe.jornadaDeHoy(HOY).getFecha());
    }

    @Test
    @DisplayName("Sin entrada abierta no se puede marcar la salida")
    void salidaSinEntrada() {
        assertFalse(d.jefe.estaPresente());
        assertThrows(OperacionInvalidaException.class, () -> d.jefe.marcarSalida(AHORA));
    }
}
