package servidorobra;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una jornada de asistencia: el día, la hora en que el usuario entró a la obra y la hora en que salió.
 */
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
            throw new IllegalStateException("Esta jornada ya tiene hora de salida.");
        }
        this.horaSalida = hora;
    }

    /** Una jornada está abierta mientras no tenga hora de salida. */
    public boolean estaAbierto() {
        return horaSalida == null;
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

    public String toJson() {
        return "{\"fecha\":" + Json.texto(fecha.toString())
                + ",\"horaEntrada\":" + Json.texto(horaEntrada.toString())
                + ",\"horaSalida\":" + (horaSalida == null ? "null" : Json.texto(horaSalida.toString()))
                + ",\"tarde\":" + tarde + "}";
    }
}
