package construtrack.controlador.web;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioRegistro;

/** Rutas públicas de registro: crear cuenta con correo y unirse a la empresa con un código. */
public class ControladorRegistro implements ControladorWeb {

    private final ServicioRegistro registro;
    private final GestorSesiones sesiones;

    public ControladorRegistro(ServicioRegistro registro, GestorSesiones sesiones) {
        this.registro = registro;
        this.sesiones = sesiones;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post("/api/registro", this::registrar)
                .post("/api/unirse", this::unirse);
    }

    /** Crea la cuenta e inicia sesión de una vez (la persona queda pendiente de unirse). */
    private Respuesta registrar(Peticion p) {
        Cuenta cuenta = registro.registrar(p.cuerpo());
        Ingreso ingreso = new Ingreso(cuenta, null);
        return Respuesta.creado(JsonMapper.sesion(sesiones.iniciarSesion(ingreso), ingreso));
    }

    private Respuesta unirse(Peticion p) {
        Usuario usuario = registro.unirse(p.cuenta(), p.cuerpo().get("codigo"));
        return Respuesta.json(JsonMapper.mensaje("¡Bienvenido a la empresa! Ya eres " + usuario.getRol().toLowerCase() + ".",
                JsonMapper.usuario(usuario, true)));
    }
}
