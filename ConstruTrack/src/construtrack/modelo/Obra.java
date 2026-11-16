package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.LocalDate;

public class Obra {
    private final String codigo;
    private String nombre;
    private String ubicacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFinEstimada;

    public Obra(String codigo, String nombre, String ubicacion, LocalDate fechaInicio, LocalDate fechaFinEstimada) {
        if (codigo == null || !codigo.matches("[A-Z]{2,4}-\\d{2,4}")) {
            throw new DatoInvalidoException("El código de obra debe tener el formato OB-01.");
        }
        this.codigo = codigo;
        setNombre(nombre);
        setUbicacion(ubicacion);
        setFechas(fechaInicio, fechaFinEstimada);
    }

    public int porcentajeTranscurrido(LocalDate hoy) {
        if (!hoy.isAfter(fechaInicio)) {
            return 0;
        }
        if (!hoy.isBefore(fechaFinEstimada)) {
            return 100;
        }
        long total = fechaFinEstimada.toEpochDay() - fechaInicio.toEpochDay();
        return (int) Math.round(100.0 * (hoy.toEpochDay() - fechaInicio.toEpochDay()) / total);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new DatoInvalidoException("El nombre de la obra debe tener al menos 3 caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public final void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion == null ? "" : ubicacion.trim();
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFinEstimada() {
        return fechaFinEstimada;
    }

    public final void setFechas(LocalDate inicio, LocalDate finEstimada) {
        if (inicio == null || finEstimada == null || !finEstimada.isAfter(inicio)) {
            throw new DatoInvalidoException("La fecha estimada de fin debe ser posterior al inicio de la obra.");
        }
        this.fechaInicio = inicio;
        this.fechaFinEstimada = finEstimada;
    }

    @Override
    public String toString() {
        return codigo + " · " + nombre;
    }
}
