package construtrack.controlador.web;

import construtrack.servicio.ServicioPersonal;
import construtrack.util.Json;

/** Rutas CRUD del personal. Los permisos por rol los aplica el servicio, no la vista. */
public class ControladorUsuarios implements ControladorWeb {

    private final ServicioPersonal personal;

    public ControladorUsuarios(ServicioPersonal personal) {
        this.personal = personal;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/personal/opciones", p -> {
                    p.usuario();
                    String supervisores = Json.lista(personal.listarSupervisores(), s -> new JsonObjeto()
                            .texto("cedula", s.getCedula()).texto("nombre", s.getNombre()).toString());
                    String obras = Json.lista(personal.listarObras(), o -> new JsonObjeto()
                            .texto("codigo", o.getCodigo()).texto("nombre", o.getNombre()).toString());
                    return Respuesta.json(new JsonObjeto().crudo("supervisores", supervisores).crudo("obras", obras).toString());
                })
                .get("/api/usuarios", p -> Respuesta.json(JsonMapper.usuarios(personal.listarVisibles(p.usuario()))))
                .post("/api/usuarios", p -> Respuesta.creado(
                        JsonMapper.usuario(personal.registrar(p.usuario(), p.cuerpo()), false)))
                .get("/api/usuarios/{cedula}", p -> Respuesta.json(
                        JsonMapper.usuario(personal.ver(p.usuario(), p.parametro("cedula")), true)))
                .put("/api/usuarios/{cedula}", p -> Respuesta.json(
                        JsonMapper.usuario(personal.actualizar(p.usuario(), p.parametro("cedula"), p.cuerpo()), false)))
                .delete("/api/usuarios/{cedula}", p -> {
                    personal.eliminar(p.usuario(), p.parametro("cedula"));
                    return Respuesta.json(JsonMapper.ok());
                });
    }
}
