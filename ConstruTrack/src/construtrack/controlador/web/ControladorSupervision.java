package construtrack.controlador.web;

import construtrack.modelo.alertas.Alerta;
import construtrack.servicio.ServicioSupervision;
import construtrack.util.Json;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

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
                .put("/api/horas-extra/{cedula}/{fecha}", this::revisarHorasExtra)
                .get("/api/asistencia/por-confirmar", this::porConfirmar)
                .put("/api/asistencia/{cedula}/{fecha}/verificacion", this::revisarAsistencia);
    }

    private Respuesta alertas(Peticion p) {
        List<Alerta> alertas = supervision.alertas(p.usuario(), p.consulta("periodo", "ultimos7"));
        return Respuesta.json(Json.lista(alertas, JsonMapper::alerta));
    }

    private Respuesta horasExtra(Peticion p) {
        return Respuesta.json(Json.lista(supervision.horasExtraPendientes(p.usuario()),
                h -> JsonMapper.horasExtra(h.persona(), h.jornada())));
    }

    private Respuesta porConfirmar(Peticion p) {
        return Respuesta.json(Json.lista(supervision.asistenciasPorConfirmar(p.usuario()),
                m -> JsonMapper.marcaPorConfirmar(m.persona(), m.jornada())));
    }

    private Respuesta revisarAsistencia(Peticion p) {
        LocalDate fecha;
        LocalTime entrada;
        try {
            fecha = LocalDate.parse(p.parametro("fecha"));
            String hora = p.cuerpo().get("entrada");
            entrada = hora == null || hora.isBlank() ? null : LocalTime.parse(hora);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD y la hora HH:MM.");
        }
        supervision.revisarAsistencia(p.usuario(), p.parametro("cedula"), fecha, entrada, p.cuerpo().get("estado"));
        return Respuesta.json(JsonMapper.ok());
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
