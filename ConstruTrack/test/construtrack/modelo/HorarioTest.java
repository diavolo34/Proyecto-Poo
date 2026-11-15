package construtrack.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HorarioTest {

    private final Horario horario = Horario.desdeTexto("vi,lu,mi", "07:00", "16:00");

    @Test
    @DisplayName("Llegar dentro de los 10 minutos de tolerancia no es llegada tarde")
    void toleranciaDeDiezMinutos() {
        assertFalse(horario.esLlegadaTarde(LocalTime.of(7, 10)));
        assertTrue(horario.esLlegadaTarde(LocalTime.of(7, 11)));
    }

    @Test
    @DisplayName("Los días se guardan ordenados y se reconocen")
    void diasOrdenados() {
        assertEquals("LU,MI,VI", horario.getDias());
        assertTrue(horario.incluyeDia(DayOfWeek.WEDNESDAY));
        assertFalse(horario.incluyeDia(DayOfWeek.TUESDAY));
    }

    @Test
    @DisplayName("Datos inválidos se rechazan con un mensaje claro")
    void datosInvalidos() {
        assertThrows(DatoInvalidoException.class, () -> Horario.desdeTexto("LU", "16:00", "07:00"));
        assertThrows(DatoInvalidoException.class, () -> Horario.desdeTexto("XX", "07:00", "16:00"));
        assertThrows(DatoInvalidoException.class, () -> Horario.desdeTexto("LU", "7am", "16:00"));
    }
}
