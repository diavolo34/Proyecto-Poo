package construtrack.controlador.web;

import construtrack.modelo.alertas.Alerta;
import construtrack.servicio.ServicioSupervision;
import construtrack.util.Json;
import java.util.List;

/** Rutas de apoyo al supervisor: alertas automáticas y estadísticas del período. */
public class ControladorSupervision implements ControladorWeb {

    private final ServicioSupervision supervision;

    public ControladorSupervision(ServicioSupervision supervision) {
        this.supervision = supervision;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/supervision/alertas", this::alertas)
                .get("/api/estadisticas", this::estadisticas);
    }

    private Respuesta alertas(Peticion p) {
        List<Alerta> alertas = supervision.alertas(p.usuario(), p.consulta("periodo", "semana"));
        return Respuesta.json(Json.lista(alertas, JsonMapper::alerta));
    }

    private Respuesta estadisticas(Peticion p) {
        return Respuesta.json(JsonMapper.estadisticas(
                supervision.estadisticas(p.usuario(), p.consulta("periodo", "semana"))));
    }
}
