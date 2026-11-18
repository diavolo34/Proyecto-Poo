package construtrack.controlador.web;

import construtrack.servicio.ServicioTalento;
import construtrack.util.Json;
import java.time.LocalDate;

public class ControladorTalento implements ControladorWeb {
    private final ServicioTalento talento;

    public ControladorTalento(ServicioTalento talento) {
        this.talento = talento;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/talento", p -> Respuesta.json(new JsonObjeto()
                        .crudo("activos", JsonMapper.usuarios(talento.activos(p.usuario())))
                        .crudo("egresos", Json.lista(talento.listar(p.usuario()), JsonMapper::egreso)).toString()))
                .get("/api/talento/pendientes/{cedula}", p -> {
                    ServicioTalento.Pendientes pendientes = talento.pendientes(p.usuario(), p.parametro("cedula"));
                    LocalDate hoy = LocalDate.now();
                    return Respuesta.json(new JsonObjeto()
                            .crudo("persona", JsonMapper.usuario(pendientes.persona(), false))
                            .crudo("herramientas", Json.lista(pendientes.entregas(), a -> JsonMapper.asignacion(a, hoy))).toString());
                })
                .post("/api/talento/egresos/{cedula}", p -> Respuesta.creado(
                        JsonMapper.egreso(talento.registrarEgreso(p.usuario(), p.parametro("cedula"), p.cuerpo()))))
                .post("/api/talento/egresos/{id}/eliminar-ficha", p -> {
                    talento.eliminarFicha(p.usuario(), id(p));
                    return Respuesta.json(JsonMapper.ok());
                })
                .get("/api/talento/egresos/{id}/expediente", p -> Respuesta.descarga(talento.expediente(p.usuario(), id(p)),
                        "text/plain", "expediente-" + p.parametro("id") + ".txt"));
    }

    private static int id(Peticion p) {
        try {
            return Integer.parseInt(p.parametro("id"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El número de egreso no es válido.");
        }
    }
}
