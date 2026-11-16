package construtrack.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indicadores de asistencia y puntualidad de un grupo de personas en un período.
 * Solo consulta el historial: no modifica datos. Un Empleado la usa con él mismo y un Supervisor o Jefe con su equipo.
 */
public final class Estadisticas {

    /** Resumen de una persona en el período. */
    public record Resumen(String cedula, String nombre, String rol, int laborables, int asistencias,
            double tasaAsistencia, int retardos, int ausencias, int justificadas,
            double puntualidad, long minutos, long minutosEsperados, long extraDiaria, long extraDiaLibre,
            long extraPendiente) {
    }

    private final Periodo periodo;
    private final List<Resumen> resumenes = new ArrayList<>();
    private final Map<String, Integer> incidenciasPorTipo = new TreeMap<>();

    public Estadisticas(List<Usuario> personas, Periodo periodo, LocalDate hoy) {
        this.periodo = periodo;
        for (Usuario u : personas) {
            Historial h = u.getHistorial();
            // Hoy todavía no cuenta como falta, pero si ya marcó entrada se suma a los días esperados (evita "11 / 10")
            boolean vinoHoy = periodo.contiene(hoy) && h.asistenciaDel(hoy) != null;
            int laborables = h.diasLaborables(u.getHorario(), periodo, hoy).size() + (vinoHoy ? 1 : 0);
            int asistencias = h.asistenciasEn(periodo).size();
            double tasa = laborables == 0 ? 100.0 : Math.min(100.0, 100.0 * asistencias / laborables);
            Horario horario = u.getHorario();
            // Horas que debía cumplir: solo los días de su horario (un sábado libre no suma horas esperadas)
            int diasConHorario = h.diasLaborables(horario, periodo, hoy).size() + (vinoHoy && u.trabajaEl(hoy) ? 1 : 0);
            long esperados = diasConHorario * horario.minutosJornada();
            resumenes.add(new Resumen(u.getCedula(), u.getNombre(), u.getRol(), laborables, asistencias, tasa,
                    h.contarRetardos(periodo), h.ausenciasSinJustificar(u.getHorario(), periodo, hoy).size(),
                    h.ausenciasJustificadas(u.getHorario(), periodo, hoy).size(),
                    h.porcentajePuntualidad(periodo), h.minutosTrabajados(periodo), esperados,
                    h.minutosExtra(horario, periodo, TipoHorasExtra.DIARIA, EstadoHorasExtra.APROBADAS),
                    h.minutosExtra(horario, periodo, TipoHorasExtra.DIA_LIBRE, EstadoHorasExtra.APROBADAS),
                    h.minutosExtraPendientes(horario, periodo)));
            h.incidenciasEn(periodo).forEach(i -> incidenciasPorTipo.merge(i.getNombreTipo(), 1, Integer::sum));
        }
    }

    /** Tasa de asistencia del grupo: jornadas registradas frente a días laborables. */
    public double tasaAsistencia() {
        int laborables = resumenes.stream().mapToInt(Resumen::laborables).sum();
        int asistencias = resumenes.stream().mapToInt(Resumen::asistencias).sum();
        return laborables == 0 ? 100.0 : Math.min(100.0, 100.0 * asistencias / laborables);
    }

    /** Porcentaje de llegadas puntuales del grupo. */
    public double porcentajePuntualidad() {
        int jornadas = resumenes.stream().mapToInt(Resumen::asistencias).sum();
        int retardos = resumenes.stream().mapToInt(Resumen::retardos).sum();
        return jornadas == 0 ? 100.0 : 100.0 * (jornadas - retardos) / jornadas;
    }

    public long minutosTrabajados() {
        return resumenes.stream().mapToLong(Resumen::minutos).sum();
    }

    /** Personas ordenadas de más a menos retardos y ausencias: el primero es quien más requiere atención. */
    public List<Resumen> ranking() {
        return resumenes.stream()
                .sorted(Comparator.comparingInt(Resumen::retardos).thenComparingInt(Resumen::ausencias).reversed())
                .toList();
    }

    public List<Resumen> resumenes() {
        return List.copyOf(resumenes);
    }

    /** Número de incidencias del período por tipo (Retardo, Reposo médico...). */
    public Map<String, Integer> incidenciasPorTipo() {
        return new LinkedHashMap<>(incidenciasPorTipo);
    }

    public Periodo getPeriodo() {
        return periodo;
    }
}
