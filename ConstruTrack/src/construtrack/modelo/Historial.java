package construtrack.modelo;

import construtrack.modelo.incidencias.Incidencia;
import construtrack.modelo.incidencias.Justificable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Historial laboral de un usuario: sus jornadas de asistencia y sus incidencias.
 * Es parte del usuario (composición): se crea con él y desaparece con él.
 * Concentra las consultas sobre ese historial para que reportes, estadísticas y alertas las reutilicen.
 */
public class Historial {

    private final List<RegistroAsistencia> asistencias = new ArrayList<>();
    private final List<Incidencia> incidencias = new ArrayList<>();
    /** Primer día en la empresa: antes de esa fecha no se esperan jornadas (null = sin límite, datos antiguos). */
    private LocalDate inicio;

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public void agregarAsistencia(RegistroAsistencia registro) {
        asistencias.add(registro);
        asistencias.sort(Comparator.comparing(RegistroAsistencia::getFecha)
                .thenComparing(RegistroAsistencia::getHoraEntrada));
    }

    public void agregarIncidencia(Incidencia incidencia) {
        incidencias.add(incidencia);
    }

    public List<RegistroAsistencia> getAsistencias() {
        return Collections.unmodifiableList(asistencias);
    }

    public List<Incidencia> getIncidencias() {
        return Collections.unmodifiableList(incidencias);
    }

    public RegistroAsistencia ultimaAsistencia() {
        return asistencias.isEmpty() ? null : asistencias.get(asistencias.size() - 1);
    }

    /** Jornada de una fecha, o null si ese día no marcó entrada. */
    public RegistroAsistencia asistenciaDel(LocalDate fecha) {
        for (int i = asistencias.size() - 1; i >= 0; i--) {
            if (asistencias.get(i).getFecha().equals(fecha)) {
                return asistencias.get(i);
            }
        }
        return null;
    }

    public List<RegistroAsistencia> asistenciasEn(Periodo periodo) {
        List<RegistroAsistencia> resultado = new ArrayList<>();
        for (RegistroAsistencia r : asistencias) {
            if (periodo.contiene(r.getFecha())) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    public List<Incidencia> incidenciasEn(Periodo periodo) {
        List<Incidencia> resultado = new ArrayList<>();
        for (Incidencia i : incidencias) {
            if (!i.getHasta().isBefore(periodo.getDesde()) && !i.getDesde().isAfter(periodo.getHasta())) {
                resultado.add(i);
            }
        }
        return resultado;
    }

    public int contarRetardos(Periodo periodo) {
        int retardos = 0;
        for (RegistroAsistencia r : asistenciasEn(periodo)) {
            if (r.isTarde()) {
                retardos++;
            }
        }
        return retardos;
    }

    /**
     * Días del período en los que el horario indica que debía trabajar, sin contar hoy ni el futuro,
     * ni los días anteriores a su ingreso a la empresa.
     */
    public List<LocalDate> diasLaborables(Horario horario, Periodo periodo, LocalDate hoy) {
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate fecha : periodo.fechas()) {
            boolean yaTrabajaba = inicio == null || !fecha.isBefore(inicio);
            if (yaTrabajaba && fecha.isBefore(hoy) && horario.incluyeDia(fecha.getDayOfWeek())) {
                dias.add(fecha);
            }
        }
        return dias;
    }

    /** Una incidencia aprobada de un tipo justificable cubre esa fecha. */
    public boolean estaJustificado(LocalDate fecha) {
        for (Incidencia i : incidencias) {
            if (i instanceof Justificable j && j.justificaAusenciaEl(fecha)) {
                return true;
            }
        }
        return false;
    }

    /** Días laborables sin jornada registrada y sin justificativo aprobado: se detectan automáticamente. */
    public List<LocalDate> ausenciasSinJustificar(Horario horario, Periodo periodo, LocalDate hoy) {
        List<LocalDate> ausencias = new ArrayList<>();
        for (LocalDate fecha : diasLaborables(horario, periodo, hoy)) {
            if (asistenciaDel(fecha) == null && !estaJustificado(fecha)) {
                ausencias.add(fecha);
            }
        }
        return ausencias;
    }

    /** Días laborables sin asistencia pero cubiertos por un justificativo aprobado. */
    public List<LocalDate> ausenciasJustificadas(Horario horario, Periodo periodo, LocalDate hoy) {
        List<LocalDate> justificadas = new ArrayList<>();
        for (LocalDate fecha : diasLaborables(horario, periodo, hoy)) {
            if (asistenciaDel(fecha) == null && estaJustificado(fecha)) {
                justificadas.add(fecha);
            }
        }
        return justificadas;
    }

    /** Porcentaje de llegadas puntuales sobre las jornadas registradas (100 si no hay jornadas). */
    public double porcentajePuntualidad(Periodo periodo) {
        List<RegistroAsistencia> jornadas = asistenciasEn(periodo);
        if (jornadas.isEmpty()) {
            return 100.0;
        }
        return 100.0 * (jornadas.size() - contarRetardos(periodo)) / jornadas.size();
    }

    public long minutosTrabajados(Periodo periodo) {
        long minutos = 0;
        for (RegistroAsistencia r : asistenciasEn(periodo)) {
            minutos += r.minutosTrabajados();
        }
        return minutos;
    }

    /** Copia todo el historial de otro (se usa cuando el Jefe cambia el rol de una persona). */
    void copiarDe(Historial otro) {
        asistencias.clear();
        asistencias.addAll(otro.asistencias);
        incidencias.clear();
        incidencias.addAll(otro.incidencias);
        inicio = otro.inicio;
    }
}
