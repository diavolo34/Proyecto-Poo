package construtrack.modelo;

import static construtrack.Datos.AHORA;
import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.alertas.FaltasConsecutivas;
import construtrack.modelo.alertas.Nivel;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.novedades.Inasistencia;
import construtrack.modelo.novedades.Novedad;
import construtrack.modelo.novedades.PermisoLaboral;
import construtrack.modelo.novedades.Retardo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NovedadTest {
    private final Datos d = new Datos();
    private final FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();

    private Novedad permisoDeJose() {
        Novedad n = fabrica.crear(PermisoLaboral.CODIGO, Datos.novedad(1, d.jose, d.ana, HOY, HOY, Map.of()));
        d.ana.registrarNovedad(n);
        return n;
    }

    private Novedad reposoDeJose(LocalDate desde, LocalDate hasta) {
        Novedad n = fabrica.crear("REPOSO_MEDICO", Datos.novedad(2, d.jose, d.ana, desde, hasta, Datos.reposoCompleto()));
        d.ana.registrarNovedad(n);
        return n;
    }

    @Test
    @DisplayName("El supervisor registra; queda pendiente hasta que el gerente aprueba con una nota, y figura quién autorizó")
    void cicloDeVida() {
        Novedad n = permisoDeJose();
        assertEquals(EstadoNovedad.PENDIENTE, n.getEstado());
        assertEquals("Ana Rodríguez", n.getRegistradaPor().nombre());
        d.gerente.revisar(n, EstadoNovedad.APROBADA, "Autorizado, cubre el turno Pedro", AHORA);
        assertTrue(n.estaAprobada());
        assertEquals("Carlos Méndez", n.getRevisadaPor().nombre());
        assertEquals("Autorizado, cubre el turno Pedro", n.getNota());
        assertThrows(OperacionInvalidaException.class, () -> d.gerente.revisar(n, EstadoNovedad.RECHAZADA, "No procede", AHORA));
    }

    @Test
    @DisplayName("Aprobar y rechazar exigen una nota")
    void notaObligatoria() {
        Novedad n = permisoDeJose();
        assertThrows(DatoInvalidoException.class, () -> d.gerente.revisar(n, EstadoNovedad.APROBADA, " ", AHORA));
        assertThrows(DatoInvalidoException.class, () -> d.gerente.revisar(n, EstadoNovedad.RECHAZADA, "", AHORA));
        assertThrows(DatoInvalidoException.class, () -> d.gerente.revisar(n, EstadoNovedad.PENDIENTE, "Nota", AHORA));
    }

    @Test
    @DisplayName("Solo el supervisor registra, y solo de su personal; solo el gerente aprueba")
    void permisos() {
        Novedad deLaura = fabrica.crear(Inasistencia.CODIGO, Datos.novedad(3, d.laura, d.ana, HOY, HOY, Map.of()));
        assertThrows(PermisoDenegadoException.class, () -> d.ana.registrarNovedad(deLaura), "Laura no es del personal de Ana");
        Novedad propia = fabrica.crear(PermisoLaboral.CODIGO, Datos.novedad(4, d.jose, d.jose, HOY, HOY, Map.of()));
        assertThrows(PermisoDenegadoException.class, () -> d.jose.registrarNovedad(propia), "El empleado no registra");
        Novedad delGerente = fabrica.crear(PermisoLaboral.CODIGO, Datos.novedad(5, d.jose, d.gerente, HOY, HOY, Map.of()));
        assertThrows(PermisoDenegadoException.class, () -> d.gerente.registrarNovedad(delGerente));
        assertFalse(d.ana instanceof RevisorNovedades, "El supervisor ya no aprueba");
        assertTrue(d.gerente.puedeRevisar(permisoDeJose()));
    }

    @Test
    @DisplayName("La fábrica separa permisos y reposos de los reportes en obra, sin gravedad, y lee los códigos anteriores")
    void fabrica() {
        assertEquals(Map.of("PERMISO_LABORAL", "Permiso laboral", "REPOSO_MEDICO", "Reposo médico"), fabrica.tipos(Categoria.LICENCIA));
        assertEquals(3, fabrica.tipos(Categoria.REPORTE_OBRA).size());
        assertTrue(fabrica.crear("PERMISO_PERSONAL", Datos.novedad(6, d.jose, d.ana, HOY, HOY, Map.of())) instanceof PermisoLaboral);
        assertTrue(fabrica.crear("AUSENCIA", Datos.novedad(7, d.jose, d.ana, HOY, HOY, Map.of())) instanceof Inasistencia);
        Novedad retardo = fabrica.crear("RETARDO", Datos.novedad(8, d.jose, d.ana, HOY, HOY, Map.of("minutos", "90", "obra", "Torre")));
        assertTrue(retardo instanceof Retardo r && "Torre".equals(r.getObra()));
        assertThrows(DatoInvalidoException.class, () -> fabrica.crear("VACACIONES", Datos.novedad(9, d.jose, d.ana, HOY, HOY, Map.of())));
        assertThrows(DatoInvalidoException.class, () -> fabrica.crear("RETARDO", Categoria.LICENCIA,
                Datos.novedad(10, d.jose, d.ana, HOY, HOY, Map.of("minutos", "5"))));
    }

    @Test
    @DisplayName("El reposo médico exige médico, matrícula, centro asistencial, tipo de consulta y el documento")
    void reposoCompleto() {
        for (String falta : List.of("medico", "matricula", "centro", "tipoConsulta", "archivo")) {
            Map<String, String> datos = new HashMap<>(Datos.reposoCompleto());
            datos.remove(falta);
            Novedad incompleto = fabrica.crear("REPOSO_MEDICO", Datos.novedad(11, d.jose, d.ana, HOY, HOY, datos));
            assertThrows(DatoInvalidoException.class, () -> d.ana.registrarNovedad(incompleto), "Falta " + falta);
        }
        Novedad reposo = reposoDeJose(HOY, HOY.plusDays(2));
        assertTrue(reposo.getDetalle().contains("consulta interna"));
        assertEquals("doc-1", reposo.getArchivo());
    }

    @Test
    @DisplayName("Un reposo aprobado justifica los días como «Incapacidad» y bloquea la marca; pendiente, no")
    void incapacidad() {
        Novedad reposo = reposoDeJose(HOY, HOY.plusDays(2));
        assertNull(d.jose.getHistorial().incapacidadEl(HOY));
        d.gerente.revisar(reposo, EstadoNovedad.APROBADA, "Recibido el informe", AHORA);
        assertEquals("Incapacidad", d.jose.getHistorial().justificacionDel(HOY.plusDays(1)).etiquetaJustificacion());
        OperacionInvalidaException e = assertThrows(OperacionInvalidaException.class, () -> d.jose.marcarEntrada(AHORA));
        assertTrue(e.getMessage().contains("reposo médico aprobado"));
        assertTrue(d.ana.marcarEntrada(AHORA).estaAbierto(), "A los demás no les afecta");
    }

    @Test
    @DisplayName("Dos o más faltas laborables seguidas sin justificar generan una alerta alta")
    void faltasConsecutivas() {
        LocalDate lunes = HOY.with(DayOfWeek.MONDAY).minusWeeks(1);
        d.jose.setFechaIngreso(lunes);
        Periodo semana = Periodo.semanaDe(lunes);
        d.jose.agregarAsistencia(new RegistroAsistencia(lunes, java.time.LocalTime.of(7, 0), false));
        List<Alerta> alertas = new FaltasConsecutivas(2).evaluar(d.jose, semana, HOY, AHORA);
        assertEquals(1, alertas.size());
        assertEquals(Nivel.ALTA, alertas.get(0).getNivel());
        assertTrue(alertas.get(0).getMensaje().startsWith("4 días laborables seguidos"));

        Novedad reposo = fabrica.crear("REPOSO_MEDICO", Datos.novedad(12, d.jose, d.ana, lunes.plusDays(1), lunes.plusDays(3),
                Datos.reposoCompleto()));
        d.ana.registrarNovedad(reposo);
        d.gerente.revisar(reposo, EstadoNovedad.APROBADA, "Informe recibido", AHORA);
        assertTrue(new FaltasConsecutivas(2).evaluar(d.jose, semana, HOY, AHORA).isEmpty(), "Queda una sola falta suelta");
        assertEquals(3, d.jose.getHistorial().diasDeIncapacidad(d.jose.getHorario(), semana, HOY).size());
    }

    @Test
    @DisplayName("Aprobada o rechazada, sale del tablero 3 horas después")
    void archivadaTresHorasDespues() {
        Novedad n = permisoDeJose();
        assertFalse(n.archivadaEn(AHORA.plusDays(30)));
        d.gerente.revisar(n, EstadoNovedad.APROBADA, "Ok, aprobado", AHORA);
        assertFalse(n.archivadaEn(AHORA.plusHours(Novedad.HORAS_EN_TABLERO).minusMinutes(1)));
        assertTrue(n.archivadaEn(AHORA.plusHours(Novedad.HORAS_EN_TABLERO)));
    }
}
