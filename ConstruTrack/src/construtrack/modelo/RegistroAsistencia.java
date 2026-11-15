package construtrack.modelo;

import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** Una jornada de asistencia: el día, la hora en que el usuario entró a la obra y la hora en que salió. */
public class RegistroAsistencia {

    private final LocalDate fecha;
    private final LocalTime horaEntrada;
    private LocalTime horaSalida;
    private final boolean tarde;

    public RegistroAsistencia(LocalDate fecha, LocalTime horaEntrada, boolean tarde) {
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.tarde = tarde;
    }

    /** Cierra la jornada guardando la hora de salida. */
    public void registrarSalida(LocalTime hora) {
        if (!estaAbierto()) {
            throw new OperacionInvalidaException("Esta jornada ya tiene hora de salida.");
        }
        if (hora.isBefore(horaEntrada)) {
            throw new OperacionInvalidaException("La hora de salida no puede ser anterior a la de entrada.");
        }
        this.horaSalida = hora;
    }

    /** Una jornada está abierta mientras no tenga hora de salida. */
    public boolean estaAbierto() {
        return horaSalida == null;
    }

    /** Minutos trabajados en la jornada (0 si sigue abierta). */
    public long minutosTrabajados() {
        return estaAbierto() ? 0 : Duration.between(horaEntrada, horaSalida).toMinutes();
    }

    /** Minutos de retraso respecto a la hora de entrada del horario (0 si llegó a tiempo). */
    public long minutosRetraso(Horario horario) {
        return Math.max(0, Duration.between(horario.getHoraEntrada(), horaEntrada).toMinutes());
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public boolean isTarde() {
        return tarde;
    }
}
