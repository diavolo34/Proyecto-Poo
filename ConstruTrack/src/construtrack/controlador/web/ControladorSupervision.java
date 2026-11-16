package construtrack.controlador.web;

import construtrack.modelo.alertas.Alerta;
import construtrack.servicio.ServicioSupervision;
import construtrack.util.Json;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
                .get("/api/estadisticas", this::estadisticas)
                .get("/api/horas-extra", this::horasExtra)
                .put("/api/horas-extra/{cedula}/{fecha}", this::revisarHorasExtra);
    }

    private Respuesta alertas(Peticion p) {
        List<Alerta> alertas = supervision.alertas(p.usuario(), p.consulta("periodo", "ultimos7"));
        return Respuesta.json(Json.lista(alertas, JsonMapper::alerta));
    }

    private Respuesta horasExtra(Peticion p) {
        return Respuesta.json(Json.lista(supervision.horasExtraPendientes(p.usuario()),
                h -> JsonMapper.horasExtra(h.persona(), h.jornada())));
    }

    private Respuesta revisarHorasExtra(Peticion p) {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(p.parametro("fecha"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD.");
        }
        supervision.revisarHorasExtra(p.usuario(), p.parametro("cedula"), fecha, p.cuerpo().get("estado"));
        return Respuesta.json(JsonMapper.ok());
    }

    private Respuesta estadisticas(Peticion p) {
        return Respuesta.json(JsonMapper.estadisticas(
                supervision.estadisticas(p.usuario(), p.consulta("periodo", "ultimos7"))));
    }
}
