package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Horario de trabajo de un usuario: los días que trabaja y su hora de entrada y salida.
 */
public class Horario {

    /** Minutos de tolerancia antes de considerar una llegada como tarde. */
    public static final int TOLERANCIA_MINUTOS = 10;

    /** Códigos de los días en el orden de DayOfWeek (lunes = 1). */
    private static final List<String> CODIGOS = Arrays.asList("LU", "MA", "MI", "JU", "VI", "SA", "DO");

    private String dias;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;

    public Horario(String dias, LocalTime horaEntrada, LocalTime horaSalida) {
        setDias(dias);
        setHoras(horaEntrada, horaSalida);
    }

    /** Crea un horario a partir de texto, por ejemplo ("LU,MA,MI", "07:00", "16:00"). */
    public static Horario desdeTexto(String dias, String horaEntrada, String horaSalida) {
        try {
            return new Horario(dias, LocalTime.parse(horaEntrada), LocalTime.parse(horaSalida));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DatoInvalidoException("Las horas deben tener el formato HH:mm.");
        }
    }

    /** Indica si el horario incluye el día de la semana recibido. */
    public boolean incluyeDia(DayOfWeek dia) {
        return Arrays.asList(dias.split(",")).contains(CODIGOS.get(dia.getValue() - 1));
    }

    /** Indica si una hora de llegada supera la hora de entrada más la tolerancia. */
    public boolean esLlegadaTarde(LocalTime hora) {
        return hora.isAfter(horaEntrada.plusMinutes(TOLERANCIA_MINUTOS));
    }

    public String getDias() {
        return dias;
    }

    /** Recibe los días separados por coma, los valida y los guarda en orden de la semana. */
    public final void setDias(String dias) {
        if (dias == null || dias.trim().isEmpty()) {
            throw new DatoInvalidoException("El horario debe tener al menos un día.");
        }
        Set<String> elegidos = new LinkedHashSet<>();
        for (String dia : dias.toUpperCase().split(",")) {
            String codigo = dia.trim();
            if (!CODIGOS.contains(codigo)) {
                throw new DatoInvalidoException("Día no válido en el horario: " + codigo);
            }
            elegidos.add(codigo);
        }
        StringBuilder ordenados = new StringBuilder();
        for (String codigo : CODIGOS) {
            if (elegidos.contains(codigo)) {
                ordenados.append(ordenados.length() == 0 ? "" : ",").append(codigo);
            }
        }
        this.dias = ordenados.toString();
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    /** Cambia ambas horas a la vez para poder validar que la entrada sea antes de la salida. */
    public final void setHoras(LocalTime horaEntrada, LocalTime horaSalida) {
        if (horaEntrada == null || horaSalida == null || !horaEntrada.isBefore(horaSalida)) {
            throw new DatoInvalidoException("La hora de entrada debe ser anterior a la hora de salida.");
        }
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
    }
}
