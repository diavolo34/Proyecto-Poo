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
                .get("/api/obras/{codigo}/codigo-asistencia", p -> {
                    ServicioObras.CodigoObra c = obras.codigoAsistencia(p.usuario(), p.parametro("codigo"));
                    return Respuesta.json(new JsonObjeto().texto("obra", c.obra().getCodigo()).texto("nombre", c.obra().getNombre())
                            .texto("codigo", c.codigo()).numero("segundosRestantes", c.segundosRestantes()).toString());
                })
                .post("/api/obras/{codigo}/inicio", p -> Respuesta.json(
                        obraSimple(obras.iniciar(p.usuario(), p.parametro("codigo")))))
                .post("/api/obras/{codigo}/conclusion", p -> Respuesta.json(
                        obraSimple(obras.concluir(p.usuario(), p.parametro("codigo")))))
                .post("/api/obras/{codigo}/retrasos", p -> Respuesta.creado(JsonMapper.retraso(obras.reportarRetraso(
                        p.usuario(), p.parametro("codigo"), p.cuerpo().get("motivo"), p.cuerpo().get("nuevaFecha")))))
                .put("/api/obras/{codigo}/retrasos/{id}/revision", p -> Respuesta.json(JsonMapper.retraso(obras.revisarRetraso(
                        p.usuario(), p.parametro("codigo"), entero(p.parametro("id"))))))
                .put("/api/usuarios/{cedula}/obra", p -> Respuesta.json(JsonMapper.usuario(
                        obras.asignar(p.usuario(), p.parametro("cedula"), p.cuerpo().get("obra")), false)));
    }

    private static int entero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El número de retraso no es válido.");
        }
    }

    private static String obraSimple(Obra o) {
        return new JsonObjeto().texto("codigo", o.getCodigo()).texto("nombre", o.getNombre())
                .texto("estado", o.getEstado().name()).texto("estadoNombre", o.getEstado().getNombre()).toString();
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
                .numero("latitud", o.getLatitud())
                .numero("longitud", o.getLongitud())
                .numero("radio", o.getRadioMetros())
                .logico("exigeCodigo", o.exigeCodigo())
                .texto("estado", o.getEstado().name())
                .texto("estadoNombre", o.getEstado().getNombre())
                .texto("inicioReal", o.getFechaInicioReal())
                .texto("conclusion", o.getFechaConclusion())
                .crudo("retrasos", Json.lista(o.getRetrasos(), JsonMapper::retraso))
                .crudo("personas", Json.lista(op.personas(), u -> new JsonObjeto()
                        .texto("cedula", u.getCedula()).texto("nombre", u.getNombre()).texto("rol", u.getRol()).texto("puesto", u.getPuesto()).toString()))
                .toString();
    }
}
