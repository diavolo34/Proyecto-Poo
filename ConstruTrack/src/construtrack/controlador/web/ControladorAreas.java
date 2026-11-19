package construtrack.controlador.web;

import construtrack.modelo.Area;
import construtrack.modelo.Usuario;
import construtrack.servicio.ServicioAreas;
import construtrack.util.Json;

public class ControladorAreas implements ControladorWeb {
    private final ServicioAreas areas;

    public ControladorAreas(ServicioAreas areas) {
        this.areas = areas;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/areas", p -> Respuesta.json(vista(p.usuario())))
                .post("/api/areas", p -> {
                    Area area = areas.crear(p.usuario(), p.cuerpo().get("nombre"), p.cuerpo().get("supervisor"));
                    return Respuesta.creado(new JsonObjeto().texto("nombre", area.getNombre()).toString());
                })
                .post("/api/areas/supervisor", p -> {
                    areas.cambiarSupervisor(p.usuario(), p.cuerpo().get("area"), p.cuerpo().get("supervisor"));
                    return Respuesta.json(JsonMapper.ok());
                })
                .post("/api/areas/eliminar", p -> {
                    int sueltos = areas.eliminar(p.usuario(), p.cuerpo().get("area")).size();
                    return Respuesta.json(new JsonObjeto().numero("sinArea", sueltos).toString());
                })
                .post("/api/areas/incluir", p -> Respuesta.json(JsonMapper.usuario(
                        areas.incluir(p.usuario(), p.cuerpo().get("cedula"), p.cuerpo().get("area")), false)))
                .post("/api/areas/sacar", p -> Respuesta.json(JsonMapper.usuario(
                        areas.sacar(p.usuario(), p.cuerpo().get("cedula")), false)));
    }

    private String vista(Usuario actual) {
        ServicioAreas.Vista v = areas.ver(actual);
        String lista = Json.lista(v.areas(), a -> new JsonObjeto()
                .texto("nombre", a.area().getNombre())
                .crudo("supervisor", a.area().getSupervisor() == null ? "null" : new JsonObjeto()
                        .texto("cedula", a.area().getSupervisor().getCedula())
                        .texto("nombre", a.area().getSupervisor().getNombre()).toString())
                .logico("esMia", a.area().esDirigidaPor(actual))
                .crudo("integrantes", JsonMapper.usuarios(a.integrantes()))
                .toString());
        String supervisores = Json.lista(v.supervisores(), s -> new JsonObjeto().texto("cedula", s.getCedula())
                .texto("nombre", s.getNombre()).toString());
        return new JsonObjeto().crudo("areas", lista).crudo("sinArea", JsonMapper.usuarios(v.sinArea()))
                .crudo("supervisores", supervisores).toString();
    }
}
