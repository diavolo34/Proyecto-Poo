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
    private EstadoHorasExtra estadoExtra = EstadoHorasExtra.PENDIENTE;

    /** Menos de este tiempo de más no se considera hora extra (salir unos minutos tarde es normal). */
    public static final int MINIMO_EXTRA_MINUTOS = 15;

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

    /**
     * Minutos extra de la jornada según el horario: lo que pasa de la jornada en un día laborable,
     * o todo lo trabajado en un día libre. Por debajo del mínimo devuelve 0.
     */
    public long minutosExtra(Horario horario) {
        long extra = horario.incluyeDia(fecha.getDayOfWeek())
                ? minutosTrabajados() - horario.minutosJornada()
                : minutosTrabajados();
        return extra >= MINIMO_EXTRA_MINUTOS ? extra : 0;
    }

    public boolean tieneHorasExtra(Horario horario) {
        return minutosExtra(horario) > 0;
    }

    public TipoHorasExtra tipoExtra(Horario horario) {
        return horario.incluyeDia(fecha.getDayOfWeek()) ? TipoHorasExtra.DIARIA : TipoHorasExtra.DIA_LIBRE;
    }

    /** Aprueba o rechaza las horas extra; solo una vez y solo si las hay. */
    void revisarHorasExtra(EstadoHorasExtra nuevo, Horario horario) {
        if (!tieneHorasExtra(horario)) {
            throw new OperacionInvalidaException("La jornada del " + fecha + " no tiene horas extra.");
        }
        if (nuevo == null || nuevo == EstadoHorasExtra.PENDIENTE || nuevo == EstadoHorasExtra.NO_APLICA) {
            throw new OperacionInvalidaException("Elige aprobar o rechazar las horas extra.");
        }
        if (estadoExtra != EstadoHorasExtra.PENDIENTE) {
            throw new OperacionInvalidaException("Las horas extra del " + fecha + " ya están "
                    + estadoExtra.getNombre().toLowerCase() + ".");
        }
        estadoExtra = nuevo;
    }

    public EstadoHorasExtra getEstadoExtra() {
        return estadoExtra;
    }

    /** Solo la usa la persistencia para restaurar el estado guardado. */
    public void restaurarEstadoExtra(EstadoHorasExtra estado) {
        this.estadoExtra = estado;
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
