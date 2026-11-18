package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Plataforma;
import construtrack.modelo.alertas.MonitorSupervision;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.reportes.FormateadorCSV;
import construtrack.modelo.reportes.FormateadorTexto;
import construtrack.modelo.reportes.ReporteAsistencia;
import construtrack.modelo.reportes.ReporteNovedades;
import construtrack.modelo.reportes.ReportePuntualidad;
import construtrack.persistencia.AlmacenArchivos;
import construtrack.persistencia.AlmacenArchivosMemoria;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executor;

public class Servicios {
    private final ServicioAutenticacion autenticacion;
    private final ServicioAsistencia asistencia;
    private final ServicioPersonal personal;
    private final ServicioNovedades novedades;
    private final ServicioSupervision supervision;
    private final ServicioRegistro registro;
    private final ServicioInvitaciones invitaciones;
    private final ServicioObras obras;
    private final ServicioRecuperacion recuperacion;
    private final ServicioDosPasos dosPasos;
    private final ServicioCatalogo catalogo;
    private final ServicioTalento talento;
    private final ServicioInventario inventario;
    private final ServicioArchivos archivos;

    public Servicios(Empresa empresa, EscritorDatos escritor, Clock reloj, int siguienteIdNovedad) {
        this(empresa.getPlataforma(), escritor, reloj, siguienteIdNovedad, new NotificadorConsola());
    }

    public Servicios(Plataforma plataforma, EscritorDatos escritor, Clock reloj, int siguienteIdNovedad,
            Notificador notificador) {
        this(plataforma, escritor, reloj, siguienteIdNovedad, notificador, Runnable::run);
    }

    public Servicios(Plataforma plataforma, EscritorDatos escritor, Clock reloj, int siguienteIdNovedad,
            Notificador notificador, Executor envioCorreos) {
        this(plataforma, escritor, reloj, siguienteIdNovedad, notificador, envioCorreos, new AlmacenArchivosMemoria());
    }

    public Servicios(Plataforma plataforma, EscritorDatos escritor, Clock reloj, int siguienteIdNovedad,
            Notificador notificador, Executor envioCorreos, AlmacenArchivos almacenArchivos) {
        this.autenticacion = new ServicioAutenticacion(plataforma, escritor, reloj);
        this.asistencia = new ServicioAsistencia(plataforma, escritor, reloj);
        this.personal = new ServicioPersonal(plataforma, escritor, reloj);
        this.novedades = new ServicioNovedades(plataforma, escritor, reloj, FabricaNovedades.conTiposPorDefecto(),
                siguienteIdNovedad);
        this.supervision = new ServicioSupervision(plataforma, escritor, reloj, MonitorSupervision.porDefecto(),
                Map.of("asistencia", new ReporteAsistencia(), "puntualidad", new ReportePuntualidad(),
                        "permisos", new ReporteNovedades(Categoria.LICENCIA),
                        "reportes-obra", new ReporteNovedades(Categoria.REPORTE_OBRA)),
                Map.of("txt", new FormateadorTexto(), "csv", new FormateadorCSV()));
        this.registro = new ServicioRegistro(plataforma, escritor, reloj,
                new ServicioVerificacionCorreo(plataforma, escritor, reloj, notificador, envioCorreos));
        this.invitaciones = new ServicioInvitaciones(plataforma, escritor, reloj, notificador);
        this.obras = new ServicioObras(plataforma, escritor, reloj);
        this.recuperacion = new ServicioRecuperacion(plataforma, escritor, reloj, notificador, envioCorreos);
        this.dosPasos = new ServicioDosPasos(plataforma, escritor, reloj);
        this.catalogo = new ServicioCatalogo(plataforma, escritor, reloj);
        this.talento = new ServicioTalento(plataforma, escritor, reloj);
        this.inventario = new ServicioInventario(plataforma, escritor, reloj);
        this.archivos = new ServicioArchivos(plataforma, escritor, reloj, almacenArchivos);
    }

    public ServicioCatalogo catalogo() {
        return catalogo;
    }

    public ServicioTalento talento() {
        return talento;
    }

    public ServicioInventario inventario() {
        return inventario;
    }

    public ServicioArchivos archivos() {
        return archivos;
    }

    public ServicioDosPasos dosPasos() {
        return dosPasos;
    }

    public ServicioRecuperacion recuperacion() {
        return recuperacion;
    }

    public ServicioObras obras() {
        return obras;
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

    public ServicioNovedades novedades() {
        return novedades;
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
