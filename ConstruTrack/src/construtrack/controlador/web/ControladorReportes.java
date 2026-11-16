package construtrack.controlador.web;

import construtrack.servicio.Exportacion;
import construtrack.servicio.ServicioSupervision;

/** Ruta de descarga de reportes: /api/reportes/{tipo}?formato=csv|txt&periodo=semana|mes. */
public class ControladorReportes implements ControladorWeb {

    private final ServicioSupervision supervision;

    public ControladorReportes(ServicioSupervision supervision) {
        this.supervision = supervision;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/reportes/{tipo}", this::descargar);
    }

    private Respuesta descargar(Peticion p) {
        Exportacion exportacion = supervision.exportar(p.usuario(), p.parametro("tipo"),
                p.consulta("formato", "csv"), p.consulta("periodo", "ultimos7"));
        return Respuesta.descarga(exportacion.contenido(), exportacion.mime(), exportacion.nombreArchivo());
    }
}
