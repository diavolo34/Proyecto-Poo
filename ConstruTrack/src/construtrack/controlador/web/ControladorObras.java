package construtrack.controlador.web;

import construtrack.modelo.Obra;
import construtrack.servicio.ServicioObras;
import construtrack.util.Json;
import java.time.LocalDate;

public class ControladorObras implements ControladorWeb {
    private final ServicioObras obras;

    public ControladorObras(ServicioObras obras) {
        this.obras = obras;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/obras", p -> {
                    var lista = obras.listar(p.usuario());
                    LocalDate hoy = obras.hoy();
                    return Respuesta.json(Json.lista(lista, o -> obra(o, hoy)));
                })
                .post("/api/obras", p -> Respuesta.creado(obraSimple(obras.crear(p.usuario(), p.cuerpo()))))
                .put("/api/obras/{codigo}", p -> Respuesta.json(
                        obraSimple(obras.editar(p.usuario(), p.parametro("codigo"), p.cuerpo()))))
                .delete("/api/obras/{codigo}", p -> {
                    obras.eliminar(p.usuario(), p.parametro("codigo"));
                    return Respuesta.json(JsonMapper.ok());
                })
                .put("/api/usuarios/{cedula}/obra", p -> Respuesta.json(JsonMapper.usuario(
                        obras.asignar(p.usuario(), p.parametro("cedula"), p.cuerpo().get("obra")), false)));
    }

    private static String obraSimple(Obra o) {
        return new JsonObjeto().texto("codigo", o.getCodigo()).texto("nombre", o.getNombre()).toString();
    }

    private static String obra(ServicioObras.ObraConPersonas op, LocalDate hoy) {
        Obra o = op.obra();
        return new JsonObjeto()
                .texto("codigo", o.getCodigo())
                .texto("nombre", o.getNombre())
                .texto("ubicacion", o.getUbicacion())
                .texto("inicio", o.getFechaInicio())
                .texto("fin", o.getFechaFinEstimada())
                .numero("avance", o.porcentajeTranscurrido(hoy))
                .crudo("personas", Json.lista(op.personas(), u -> new JsonObjeto()
                        .texto("cedula", u.getCedula()).texto("nombre", u.getNombre()).texto("rol", u.getRol()).toString()))
                .toString();
    }
}
