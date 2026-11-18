package construtrack.modelo;

import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class RegistroAsistencia {
    private final LocalDate fecha;
    private final LocalTime horaEntrada;
    private LocalTime horaSalida;
    private final boolean tarde;
    private EstadoHorasExtra estadoExtra = EstadoHorasExtra.PENDIENTE;
    private EstadoVerificacion verificacion = EstadoVerificacion.NO_APLICA;
    private String motivoVerificacion = "";

    public static final int MINIMO_EXTRA_MINUTOS = 15;

    public RegistroAsistencia(LocalDate fecha, LocalTime horaEntrada, boolean tarde) {
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.tarde = tarde;
    }

    public void registrarSalida(LocalTime hora) {
        if (!estaAbierto()) {
            throw new OperacionInvalidaException("Esta jornada ya tiene hora de salida.");
        }
        if (hora.isBefore(horaEntrada)) {
            throw new OperacionInvalidaException("La hora de salida no puede ser anterior a la de entrada.");
        }
        this.horaSalida = hora;
    }

    public boolean estaAbierto() {
        return horaSalida == null;
    }

    public long minutosTrabajados() {
        return estaAbierto() ? 0 : Duration.between(horaEntrada, horaSalida).toMinutes();
    }

    public long minutosRetraso(Horario horario) {
        return Math.max(0, Duration.between(horario.getHoraEntrada(), horaEntrada).toMinutes());
    }

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

    public void marcarVerificada() {
        if (verificacion == EstadoVerificacion.NO_APLICA) {
            verificacion = EstadoVerificacion.VERIFICADA;
        }
    }

    public void marcarPorConfirmar(String motivo) {
        verificacion = EstadoVerificacion.POR_CONFIRMAR;
        motivoVerificacion = motivoVerificacion.isEmpty() ? motivo : motivoVerificacion + "; " + motivo;
    }

    void revisarVerificacion(EstadoVerificacion nuevo) {
        if (nuevo != EstadoVerificacion.CONFIRMADA && nuevo != EstadoVerificacion.RECHAZADA) {
            throw new OperacionInvalidaException("Elige confirmar o rechazar la marca.");
        }
        if (verificacion != EstadoVerificacion.POR_CONFIRMAR) {
            throw new OperacionInvalidaException("La marca del " + fecha + " no está por confirmar.");
        }
        verificacion = nuevo;
    }

    public boolean cuenta() {
        return verificacion != EstadoVerificacion.RECHAZADA;
    }

    public EstadoVerificacion getVerificacion() {
        return verificacion;
    }

    public String getMotivoVerificacion() {
        return motivoVerificacion;
    }

    public void restaurarVerificacion(EstadoVerificacion estado, String motivo) {
        this.verificacion = estado;
        this.motivoVerificacion = motivo == null ? "" : motivo;
    }

    public EstadoHorasExtra getEstadoExtra() {
        return estadoExtra;
    }

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
