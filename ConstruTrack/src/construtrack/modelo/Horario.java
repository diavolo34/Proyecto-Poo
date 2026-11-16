package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Horario {
    public static final int TOLERANCIA_MINUTOS = 10;

    private static final List<String> CODIGOS = Arrays.asList("LU", "MA", "MI", "JU", "VI", "SA", "DO");

    private String dias;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;

    public Horario(String dias, LocalTime horaEntrada, LocalTime horaSalida) {
        setDias(dias);
        setHoras(horaEntrada, horaSalida);
    }

    public static Horario desdeTexto(String dias, String horaEntrada, String horaSalida) {
        try {
            return new Horario(dias, LocalTime.parse(horaEntrada), LocalTime.parse(horaSalida));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DatoInvalidoException("Las horas deben tener el formato HH:mm.");
        }
    }

    public boolean incluyeDia(DayOfWeek dia) {
        return Arrays.asList(dias.split(",")).contains(CODIGOS.get(dia.getValue() - 1));
    }

    public boolean esLlegadaTarde(LocalTime hora) {
        return hora.isAfter(horaEntrada.plusMinutes(TOLERANCIA_MINUTOS));
    }

    public long minutosJornada() {
        return java.time.Duration.between(horaEntrada, horaSalida).toMinutes();
    }

    public String getDias() {
        return dias;
    }

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

    public final void setHoras(LocalTime horaEntrada, LocalTime horaSalida) {
        if (horaEntrada == null || horaSalida == null || !horaEntrada.isBefore(horaSalida)) {
            throw new DatoInvalidoException("La hora de entrada debe ser anterior a la hora de salida.");
        }
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
    }
}
