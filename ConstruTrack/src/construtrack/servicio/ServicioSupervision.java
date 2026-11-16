package construtrack.servicio;

import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Estadisticas;
import construtrack.modelo.Jefe;
import construtrack.modelo.Periodo;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.alertas.MonitorSupervision;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.reportes.FormateadorReporte;
import construtrack.modelo.reportes.Reporte;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso de apoyo al supervisor: alertas, estadísticas y reportes descargables.
 * Solo consulta datos (no los modifica), y cada rol ve únicamente lo que le corresponde.
 */
public class ServicioSupervision extends ServicioBase {

    private final MonitorSupervision monitor;
    private final Map<String, Reporte> reportes;
    private final Map<String, FormateadorReporte> formatos;

    public ServicioSupervision(Empresa empresa, EscritorDatos escritor, Clock reloj,
            MonitorSupervision monitor, Map<String, Reporte> reportes, Map<String, FormateadorReporte> formatos) {
        super(empresa, escritor, reloj);
        this.monitor = monitor;
        this.reportes = Map.copyOf(reportes);
        this.formatos = Map.copyOf(formatos);
    }

    /** Alertas de los empleados que el usuario puede supervisar (el Jefe, todos). */
    public List<Alerta> alertas(Usuario actual, String nombrePeriodo) {
        LocalDate hoy = LocalDate.now(reloj);
        Periodo periodo = periodo(nombrePeriodo, hoy);
        return leer(() -> monitor.evaluar(alcance(actual), periodo, hoy, LocalDateTime.now(reloj)));
    }

    /** Estadísticas del grupo visible; un Empleado solo ve las suyas. */
    public Estadisticas estadisticas(Usuario actual, String nombrePeriodo) {
        LocalDate hoy = LocalDate.now(reloj);
        Periodo periodo = periodo(nombrePeriodo, hoy);
        return leer(() -> new Estadisticas(actual instanceof Empleado ? List.of(actual) : alcance(actual), periodo, hoy));
    }

    /** Genera un reporte del grupo visible en el formato pedido. */
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
        Periodo periodo = periodo(nombrePeriodo, hoy);
        String contenido = leer(() -> formateador.formatear(reporte.generar(alcance(actual), periodo, hoy)));
        String nombre = "construtrack-" + tipo + "-" + hoy + "." + formateador.extension();
        return new Exportacion(contenido, formateador.mime(), nombre);
    }

    /** Personas que el usuario puede ver en reportes y alertas (sin incluir a los jefes). */
    private List<Usuario> alcance(Usuario actual) {
        if (actual instanceof Jefe) {
            return empresa.listarUsuarios().stream().filter(u -> !(u instanceof Jefe)).toList();
        }
        if (actual instanceof Supervisor supervisor) {
            return new ArrayList<Usuario>(supervisor.verEmpleadosACargo());
        }
        throw new PermisoDenegadoException("Tu rol no tiene acceso a los reportes del equipo.");
    }

    /**
     * Convierte el nombre del período en un rango de fechas. Por defecto, los últimos 7 días: así un lunes
     * por la mañana también hay datos que revisar (la semana calendario apenas empieza).
     */
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
