package construtrack.modelo;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import construtrack.Datos;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HorasExtraTest {
    private final Datos d = new Datos();
    private final LocalDate lunes = LocalDate.of(2026, 11, 16);
    private final LocalDate sabado = LocalDate.of(2026, 11, 14);
    private final Periodo semana = Periodo.ultimosDias(HOY, 7);

    private RegistroAsistencia jornada(LocalDate dia, String entrada, String salida) {
        RegistroAsistencia r = new RegistroAsistencia(dia, LocalTime.parse(entrada), false);
        r.registrarSalida(LocalTime.parse(salida));
        d.jose.agregarAsistencia(r);
        return r;
    }

    @Test
    @DisplayName("Extra diaria: lo que pasa de la jornada; menos de 15 minutos no cuenta")
    void extraDiaria() {
        assertEquals(0, jornada(lunes, "07:00", "16:10").minutosExtra(d.jose.getHorario()));
        RegistroAsistencia martes = jornada(lunes.plusDays(1), "07:00", "18:30");
        assertEquals(150, martes.minutosExtra(d.jose.getHorario()));
        assertEquals(TipoHorasExtra.DIARIA, martes.tipoExtra(d.jose.getHorario()));
    }

    @Test
    @DisplayName("En un día libre (fin de semana) todo lo trabajado es extra")
    void extraDiaLibre() {
        RegistroAsistencia r = jornada(sabado, "08:00", "12:30");
        assertEquals(270, r.minutosExtra(d.jose.getHorario()));
        assertEquals(TipoHorasExtra.DIA_LIBRE, r.tipoExtra(d.jose.getHorario()));
    }

    @Test
    @DisplayName("Solo cuentan al aprobarse; el supervisor revisa a su equipo y una sola vez")
    void aprobacionConPermisos() {
        jornada(lunes, "07:00", "18:00");
        Historial h = d.jose.getHistorial();
        assertEquals(120, h.minutosExtraPendientes(d.jose.getHorario(), semana));
        assertEquals(0, h.minutosExtra(d.jose.getHorario(), semana, TipoHorasExtra.DIARIA, EstadoHorasExtra.APROBADAS));

        assertThrows(PermisoDenegadoException.class, () -> d.luis.revisarHorasExtra(d.jose, lunes, EstadoHorasExtra.APROBADAS),
                "Luis no supervisa a José");
        assertThrows(PermisoDenegadoException.class, () -> d.jose.revisarHorasExtra(d.jose, lunes, EstadoHorasExtra.APROBADAS),
                "Nadie aprueba sus propias horas");
        d.ana.revisarHorasExtra(d.jose, lunes, EstadoHorasExtra.APROBADAS);
        assertEquals(120, h.minutosExtra(d.jose.getHorario(), semana, TipoHorasExtra.DIARIA, EstadoHorasExtra.APROBADAS));
        assertEquals(0, h.minutosExtraPendientes(d.jose.getHorario(), semana));
        assertThrows(OperacionInvalidaException.class, () -> d.gerente.revisarHorasExtra(d.jose, lunes, EstadoHorasExtra.RECHAZADAS),
                "Ya fueron aprobadas");
    }

    @Test
    @DisplayName("La tabla compara horas trabajadas con las esperadas según el horario")
    void horasEsperadas() {
        jornada(lunes, "07:00", "16:00");
        Estadisticas.Resumen r = new Estadisticas(java.util.List.of(d.jose), semana, HOY).resumenes().get(0);

        assertEquals(4 * 540, r.minutosEsperados());
        assertEquals(540, r.minutos());
    }
}
