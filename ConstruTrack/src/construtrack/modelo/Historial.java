package construtrack.modelo;

import construtrack.modelo.incidencias.Incidencia;
import construtrack.modelo.incidencias.Justificable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Historial {
    private final List<RegistroAsistencia> asistencias = new ArrayList<>();
    private final List<Incidencia> incidencias = new ArrayList<>();

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

    public RegistroAsistencia jornadaAbierta() {
        for (int i = asistencias.size() - 1; i >= 0; i--) {
            if (asistencias.get(i).estaAbierto()) {
                return asistencias.get(i);
            }
        }
        return null;
    }

    public RegistroAsistencia asistenciaDel(LocalDate fecha) {
        for (int i = asistencias.size() - 1; i >= 0; i--) {
            if (asistencias.get(i).getFecha().equals(fecha)) {
                return asistencias.get(i);
            }
        }
        return null;
    }

    public RegistroAsistencia asistenciaValidaDel(LocalDate fecha) {
        for (int i = asistencias.size() - 1; i >= 0; i--) {
            RegistroAsistencia r = asistencias.get(i);
            if (r.getFecha().equals(fecha) && r.cuenta()) {
                return r;
            }
        }
        return null;
    }

    public RegistroAsistencia jornada(LocalDate fecha, LocalTime entrada) {
        if (entrada == null) {
            return asistenciaDel(fecha);
        }
        for (RegistroAsistencia r : asistencias) {
            if (r.getFecha().equals(fecha) && r.getHoraEntrada().equals(entrada)) {
                return r;
            }
        }
        return null;
    }

    public List<RegistroAsistencia> asistenciasPorConfirmar() {
        List<RegistroAsistencia> resultado = new ArrayList<>();
        for (RegistroAsistencia r : asistencias) {
            if (r.getVerificacion() == EstadoVerificacion.POR_CONFIRMAR) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    public List<RegistroAsistencia> asistenciasEn(Periodo periodo) {
        List<RegistroAsistencia> resultado = new ArrayList<>();
        for (RegistroAsistencia r : asistencias) {
            if (periodo.contiene(r.getFecha()) && r.cuenta()) {
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

    public long minutosExtra(Horario horario, Periodo periodo, TipoHorasExtra tipo, EstadoHorasExtra estado) {
        long total = 0;
        for (RegistroAsistencia r : asistenciasEn(periodo)) {
            if (r.getEstadoExtra() == estado && r.tipoExtra(horario) == tipo) {
                total += r.minutosExtra(horario);
            }
        }
        return total;
    }

    public long minutosExtraPendientes(Horario horario, Periodo periodo) {
        return minutosExtra(horario, periodo, TipoHorasExtra.DIARIA, EstadoHorasExtra.PENDIENTE)
                + minutosExtra(horario, periodo, TipoHorasExtra.DIA_LIBRE, EstadoHorasExtra.PENDIENTE);
    }

    public List<RegistroAsistencia> horasExtraPendientes(Horario horario) {
        List<RegistroAsistencia> pendientes = new ArrayList<>();
        for (RegistroAsistencia r : asistencias) {
            if (r.cuenta() && r.tieneHorasExtra(horario) && r.getEstadoExtra() == EstadoHorasExtra.PENDIENTE) {
                pendientes.add(r);
            }
        }
        return pendientes;
    }

    public boolean estaJustificado(LocalDate fecha) {
        for (Incidencia i : incidencias) {
            if (i instanceof Justificable j && j.justificaAusenciaEl(fecha)) {
                return true;
            }
        }
        return false;
    }

    public List<LocalDate> ausenciasSinJustificar(Horario horario, Periodo periodo, LocalDate hoy) {
        List<LocalDate> ausencias = new ArrayList<>();
        for (LocalDate fecha : diasLaborables(horario, periodo, hoy)) {
            if (asistenciaValidaDel(fecha) == null && !estaJustificado(fecha)) {
                ausencias.add(fecha);
            }
        }
        return ausencias;
    }

    public List<LocalDate> ausenciasJustificadas(Horario horario, Periodo periodo, LocalDate hoy) {
        List<LocalDate> justificadas = new ArrayList<>();
        for (LocalDate fecha : diasLaborables(horario, periodo, hoy)) {
            if (asistenciaValidaDel(fecha) == null && estaJustificado(fecha)) {
                justificadas.add(fecha);
            }
        }
        return justificadas;
    }

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

    void copiarDe(Historial otro) {
        asistencias.clear();
        asistencias.addAll(otro.asistencias);
        incidencias.clear();
        incidencias.addAll(otro.incidencias);
        inicio = otro.inicio;
    }
}
