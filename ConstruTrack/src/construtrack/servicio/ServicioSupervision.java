package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoHorasExtra;
import construtrack.modelo.Estadisticas;
import construtrack.modelo.Jefe;
import construtrack.modelo.Periodo;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.alertas.MonitorSupervision;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.reportes.FormateadorReporte;
import construtrack.modelo.reportes.Reporte;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ServicioSupervision extends ServicioBase {
    private final MonitorSupervision monitor;
    private final Map<String, Reporte> reportes;
    private final Map<String, FormateadorReporte> formatos;

    public ServicioSupervision(Plataforma plataforma, EscritorDatos escritor, Clock reloj,
            MonitorSupervision monitor, Map<String, Reporte> reportes, Map<String, FormateadorReporte> formatos) {
        super(plataforma, escritor, reloj);
        this.monitor = monitor;
        this.reportes = Map.copyOf(reportes);
        this.formatos = Map.copyOf(formatos);
    }

    public List<Alerta> alertas(Usuario actual, String nombrePeriodo) {
        LocalDate hoy = LocalDate.now(reloj);
        Periodo periodo = periodo(nombrePeriodo, hoy);
        return leer(() -> monitor.evaluar(alcance(actual), periodo, hoy, LocalDateTime.now(reloj)));
    }

    public Estadisticas estadisticas(Usuario actual, String nombrePeriodo) {
        LocalDate hoy = LocalDate.now(reloj);
        Periodo periodo = periodo(nombrePeriodo, hoy);
        return leer(() -> new Estadisticas(actual instanceof Empleado ? List.of(actual) : alcance(actual), periodo, hoy));
    }

    public Exportacion exportar(Usuario actual, String tipo, String nombreFormato, String nombrePeriodo) {
        Reporte reporte = reportes.get(tipo);
        FormateadorReporte formateador = formatos.get(nombreFormato);
        if (reporte == null) {
            throw new DatoInvalidoException("Tipo de reporte no válido. Usa asistencia, puntualidad o incidencias.");
        }
        if (formateador == null) {
            throw new DatoInvalidoException("Formato no válido. Usa txt o csv.");
        }
        LocalDate hoy = LocalDate.now(reloj);
        String contenido = leer(() -> {
            List<Usuario> personas = alcance(actual);
            Periodo periodo = "todo".equals(nombrePeriodo) ? historialCompleto(personas, hoy) : periodo(nombrePeriodo, hoy);
            return formateador.formatear(reporte.generar(personas, periodo, hoy));
        });
        String nombre = "construtrack-" + tipo + "-" + hoy + "." + formateador.extension();
        return new Exportacion(contenido, formateador.mime(), nombre);
    }

    public record HorasExtraPendientes(Usuario persona, RegistroAsistencia jornada) {
    }

    public List<HorasExtraPendientes> horasExtraPendientes(Usuario actual) {
        return leer(() -> alcance(actual).stream()
                .filter(actual::puedeAprobarHorasDe)
                .flatMap(u -> u.getHistorial().horasExtraPendientes(u.getHorario()).stream()
                        .map(r -> new HorasExtraPendientes(u, r)))
                .sorted(Comparator.comparing((HorasExtraPendientes h) -> h.jornada().getFecha()))
                .toList());
    }

    public RegistroAsistencia revisarHorasExtra(Usuario actual, String cedula, LocalDate fecha, String estado) {
        EstadoHorasExtra nuevo;
        try {
            nuevo = EstadoHorasExtra.valueOf(estado == null ? "" : estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("El estado debe ser APROBADAS o RECHAZADAS.");
        }
        return modificar(() -> {
            Usuario persona = empresaDe(actual).buscarUsuario(cedula);
            if (persona == null) {
                throw new NoEncontradoException("No existe una persona con la cédula " + cedula + ".");
            }
            return actual.revisarHorasExtra(persona, fecha, nuevo);
        });
    }

    private List<Usuario> alcance(Usuario actual) {
        if (actual instanceof Jefe) {
            return empresaDe(actual).listarUsuarios();
        }
        if (actual instanceof Supervisor supervisor) {
            List<Usuario> grupo = new ArrayList<>();
            grupo.add(supervisor);
            grupo.addAll(supervisor.verEmpleadosACargo());
            return grupo;
        }
        throw new PermisoDenegadoException("Tu rol no tiene acceso a los reportes del equipo.");
    }

    static Periodo historialCompleto(List<Usuario> personas, LocalDate hoy) {
        LocalDate desde = hoy;
        LocalDate hasta = hoy;
        for (Usuario u : personas) {
            if (u.getFechaIngreso() != null && u.getFechaIngreso().isBefore(desde)) {
                desde = u.getFechaIngreso();
            }
            for (RegistroAsistencia r : u.getAsistencias()) {
                desde = r.getFecha().isBefore(desde) ? r.getFecha() : desde;
            }
            for (var i : u.getHistorial().getIncidencias()) {
                desde = i.getDesde().isBefore(desde) ? i.getDesde() : desde;
                hasta = i.getHasta().isAfter(hasta) ? i.getHasta() : hasta;
            }
        }
        return new Periodo(desde, hasta);
    }

    public static Periodo periodo(String nombre, LocalDate hoy) {
        if (nombre == null || nombre.isBlank() || "ultimos7".equals(nombre)) {
            return Periodo.ultimosDias(hoy, 7);
        }
        if ("semana".equals(nombre)) {
            return Periodo.semanaDe(hoy);
        }
        if ("mes".equals(nombre)) {
            return Periodo.mesDe(hoy);
        }
        throw new DatoInvalidoException("El período debe ser 'ultimos7', 'semana' o 'mes'.");
    }
}
