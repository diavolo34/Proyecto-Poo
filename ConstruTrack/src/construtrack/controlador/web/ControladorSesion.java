package construtrack.controlador.web;

import construtrack.modelo.Usuario;
import construtrack.servicio.ServicioAutenticacion;

/** Rutas de estado y sesión: /api/estado, /api/login, /api/logout, /api/yo. */
public class ControladorSesion implements ControladorWeb {

    private final ServicioAutenticacion autenticacion;
    private final GestorSesiones sesiones;

    public ControladorSesion(ServicioAutenticacion autenticacion, GestorSesiones sesiones) {
        this.autenticacion = autenticacion;
        this.sesiones = sesiones;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/estado", this::estado)
                .post("/api/login", this::iniciarSesion)
                .post("/api/logout", this::cerrarSesion)
                .get("/api/yo", p -> Respuesta.json(JsonMapper.usuario(p.usuario(), true)));
    }

    private Respuesta estado(Peticion p) {
        return Respuesta.json(new JsonObjeto()
                .texto("status", "ok")
                .texto("servidor", "Java")
                .texto("empresa", autenticacion.getNombreEmpresa())
                .toString());
    }

    private Respuesta iniciarSesion(Peticion p) {
        Usuario usuario = autenticacion.autenticar(p.cuerpo().get("cedula"), p.cuerpo().get("clave"));
        return Respuesta.json(new JsonObjeto()
                .texto("token", sesiones.iniciarSesion(usuario))
                .crudo("usuario", JsonMapper.usuario(usuario, true))
                .toString());
    }

    private Respuesta cerrarSesion(Peticion p) {
        sesiones.cerrarSesion(p.token());
        return Respuesta.json(JsonMapper.ok());
    }
}
