package construtrack.controlador.web;

import construtrack.modelo.Cuenta;
import construtrack.servicio.ServicioAutenticacion;
import construtrack.servicio.ServicioRecuperacion;

public class ControladorRecuperacion implements ControladorWeb {
    private final ServicioRecuperacion recuperacion;
    private final ServicioAutenticacion autenticacion;
    private final GestorSesiones sesiones;

    public ControladorRecuperacion(ServicioRecuperacion recuperacion, ServicioAutenticacion autenticacion,
            GestorSesiones sesiones) {
        this.recuperacion = recuperacion;
        this.autenticacion = autenticacion;
        this.sesiones = sesiones;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post("/api/clave/olvido", this::solicitar)
                .post("/api/clave/restablecer", this::restablecer);
    }

    private Respuesta solicitar(Peticion p) {
        recuperacion.solicitar(p.cuerpo().get("correo"));
        return Respuesta.json(new JsonObjeto().texto("mensaje", "Si ese correo está registrado, te enviamos un enlace "
                + "para elegir una clave nueva. Revisa tu bandeja de entrada y la carpeta de spam.").toString());
    }

    private Respuesta restablecer(Peticion p) {
        Cuenta cuenta = recuperacion.restablecer(p.cuerpo().get("token"), p.cuerpo().get("clave"),
                p.cuerpo().get("confirmacion"));
        sesiones.cerrarSesionesDe(cuenta);
        autenticacion.desbloquear(cuenta);
        return Respuesta.json(new JsonObjeto().texto("mensaje", "Listo: tu clave cambió. Inicia sesión con la clave nueva.")
                .toString());
    }
}
