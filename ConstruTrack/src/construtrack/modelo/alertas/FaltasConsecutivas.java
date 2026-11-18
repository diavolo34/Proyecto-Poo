package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FaltasConsecutivas implements ReglaAlerta {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM");

    private final int minimo;

    public FaltasConsecutivas(int minimo) {
        this.minimo = minimo;
    }

    @Override
    public List<Alerta> evaluar(Usuario persona, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        List<LocalDate> racha = persona.getHistorial().rachaDeFaltas(persona.getHorario(), periodo, hoy);
        if (racha.size() < minimo) {
            return List.of();
        }
        LocalDate primero = racha.get(0);
        LocalDate ultimo = racha.get(racha.size() - 1);
        return List.of(Alerta.de(Nivel.ALTA, persona, racha.size() + " días laborables seguidos sin asistir ni justificar ("
                + primero.format(FORMATO) + " al " + ultimo.format(FORMATO) + "). Si es una incapacidad, registra el reposo médico.",
                primero.atStartOfDay(), ahora));
    }
}
