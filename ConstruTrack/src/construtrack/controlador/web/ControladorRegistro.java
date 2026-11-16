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
                .post("/api/unirse", this::unirse)
                .post("/api/empresas", this::crearEmpresa);
    }

    /** Crea la cuenta e inicia sesión de una vez: el jefe entra con su empresa; los demás quedan por unirse. */
    private Respuesta registrar(Peticion p) {
        Cuenta cuenta = registro.registrar(p.cuerpo());
        Ingreso ingreso = new Ingreso(cuenta, cuenta.getUsuario());
        return Respuesta.creado(JsonMapper.sesion(sesiones.iniciarSesion(ingreso), ingreso));
    }

    private Respuesta unirse(Peticion p) {
        Usuario usuario = registro.unirse(p.cuenta(), p.cuerpo().get("codigo"));
        return Respuesta.json(JsonMapper.mensaje("¡Te damos la bienvenida a " + usuario.getEmpresa().getNombre()
                + "! Ya eres " + usuario.getRol().toLowerCase() + ".", JsonMapper.usuario(usuario, true)));
    }

    /** Una cuenta pendiente crea su empresa y pasa a ser su Jefe. */
    private Respuesta crearEmpresa(Peticion p) {
        Usuario jefe = registro.crearEmpresa(p.cuenta(), p.cuerpo().get("nombre"));
        return Respuesta.creado(JsonMapper.mensaje("Tu empresa «" + jefe.getEmpresa().getNombre()
                + "» está lista. Ahora invita a tu equipo.", JsonMapper.usuario(jefe, true)));
    }
}
