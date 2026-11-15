package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.alertas.MonitorSupervision;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.modelo.reportes.FormateadorCSV;
import construtrack.modelo.reportes.FormateadorTexto;
import construtrack.modelo.reportes.ReporteAsistencia;
import construtrack.modelo.reportes.ReporteIncidencias;
import construtrack.modelo.reportes.ReportePuntualidad;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.Map;

/**
 * Punto único de acceso a los casos de uso. Las dos vistas (Swing y web) reciben este objeto,
 * así comparten exactamente el mismo modelo y las mismas reglas.
 */
public class Servicios {

    private final ServicioAutenticacion autenticacion;
    private final ServicioAsistencia asistencia;
    private final ServicioPersonal personal;
    private final ServicioIncidencias incidencias;
    private final ServicioSupervision supervision;
    private final ServicioRegistro registro;
    private final ServicioInvitaciones invitaciones;

    public Servicios(Empresa empresa, EscritorDatos escritor, Clock reloj, int siguienteIdIncidencia) {
        this.autenticacion = new ServicioAutenticacion(empresa, escritor, reloj);
        this.asistencia = new ServicioAsistencia(empresa, escritor, reloj);
        this.personal = new ServicioPersonal(empresa, escritor, reloj);
        this.incidencias = new ServicioIncidencias(empresa, escritor, reloj,
                FabricaIncidencias.conTiposPorDefecto(), siguienteIdIncidencia);
        this.supervision = new ServicioSupervision(empresa, escritor, reloj, MonitorSupervision.porDefecto(),
                Map.of("asistencia", new ReporteAsistencia(), "puntualidad", new ReportePuntualidad(),
                        "incidencias", new ReporteIncidencias()),
                Map.of("txt", new FormateadorTexto(), "csv", new FormateadorCSV()));
        this.registro = new ServicioRegistro(empresa, escritor, reloj);
        this.invitaciones = new ServicioInvitaciones(empresa, escritor, reloj, new NotificadorConsola());
    }

    public ServicioRegistro registro() {
        return registro;
    }

    public ServicioInvitaciones invitaciones() {
        return invitaciones;
    }

    public ServicioSupervision supervision() {
        return supervision;
    }

    public ServicioIncidencias incidencias() {
        return incidencias;
    }

    public ServicioAutenticacion autenticacion() {
        return autenticacion;
    }

    public ServicioAsistencia asistencia() {
        return asistencia;
    }

    public ServicioPersonal personal() {
        return personal;
    }
}
